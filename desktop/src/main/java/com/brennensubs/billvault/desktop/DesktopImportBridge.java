package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.ImportApi.*;
import com.brennensubs.billvault.service.ExpenseImportService;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Opt-in loopback bridge; it only accesses the store owned by the running JavaFX process. */
public final class DesktopImportBridge implements AutoCloseable {
  private final HttpServer server;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private final DesktopStore store;
  private final ExpenseImportService imports;
  private final ObjectMapper json;
  private final byte[] authorization;
  private final Runnable changed;

  public DesktopImportBridge(
      int port,
      String token,
      DesktopStore store,
      ExpenseImportService imports,
      ObjectMapper json,
      Runnable changed)
      throws IOException {
    if (port < 1 || port > 65535 || token == null || !token.matches("[A-Za-z0-9_-]{32,256}"))
      throw new IllegalArgumentException(
          "MCP requires a port from 1–65535 and BILLVAULT_MCP_TOKEN with 32–256 URL-safe random"
              + " characters.");
    this.store = store;
    this.imports = imports;
    this.json = json;
    this.changed = changed;
    authorization = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 8);
    server.createContext("/api/", this::handle);
    server.setExecutor(executor);
    server.start();
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      var headers = exchange.getRequestHeaders();
      String host = "127.0.0.1:" + server.getAddress().getPort();
      if (headers.containsKey("Origin") || !host.equals(headers.getFirst("Host"))) {
        respond(
            exchange, 403, Map.of("detail", "Browser and foreign-host requests are not allowed."));
        return;
      }
      String supplied = headers.getFirst("Authorization");
      if (supplied == null
          || !MessageDigest.isEqual(authorization, supplied.getBytes(StandardCharsets.UTF_8))) {
        respond(exchange, 401, Map.of("detail", "Integration authentication required."));
        return;
      }
      try {
        String path = exchange.getRequestURI().getPath(), method = exchange.getRequestMethod();
        Object result;
        if (method.equals("GET") && path.equals("/api/categories"))
          result = store.service.categories();
        else if (method.equals("GET") && path.equals("/api/expenses")) {
          // Deliberately bounded; the adapter requests one page at a time.
          var parameters = new java.util.HashMap<String, String>();
          String query = exchange.getRequestURI().getRawQuery();
          if (query != null)
            for (String pair : query.split("&")) {
              var parts = pair.split("=", 2);
              parameters.put(
                  parts[0],
                  parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
            }
          result =
              store.service.list(
                  parameters.get("search"),
                  null,
                  null,
                  null,
                  "name",
                  Integer.parseInt(parameters.getOrDefault("page", "0")),
                  Integer.parseInt(parameters.getOrDefault("size", "20")));
        } else if (method.equals("POST") && path.equals("/api/imports/preview")) {
          result = imports.preview(json.readValue(body(exchange), Batch.class));
        } else if (method.equals("POST") && path.matches("/api/imports/[0-9a-f-]{36}/commit")) {
          var confirmation = json.readValue(body(exchange), Confirmation.class);
          result =
              imports.commit(path.split("/")[3], Boolean.TRUE.equals(confirmation.confirmed()));
          changed.run();
        } else {
          respond(exchange, 404, Map.of("detail", "Integration operation not available."));
          return;
        }
        respond(exchange, 200, result);
      } catch (ResponseStatusException e) {
        respond(
            exchange,
            e.getStatusCode().value(),
            Map.of("detail", e.getReason() == null ? "Request rejected" : e.getReason()));
      } catch (org.springframework.dao.DataIntegrityViolationException e) {
        respond(exchange, 409, Map.of("detail", "Concurrent import; retry the same preview ID."));
      } catch (IllegalArgumentException | tools.jackson.core.JacksonException e) {
        respond(exchange, 400, Map.of("detail", "Invalid integration request."));
      } catch (Exception e) {
        respond(
            exchange,
            500,
            Map.of(
                "detail",
                "Integration failed; retry the same preview ID before preparing another batch."));
      }
    }
  }

  private byte[] body(HttpExchange exchange) throws IOException {
    if (!"application/json".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Content-Type")))
      throw new IllegalArgumentException("JSON required");
    byte[] bytes = exchange.getRequestBody().readNBytes(262145);
    if (bytes.length > 262144) throw new IllegalArgumentException("Request too large");
    return bytes;
  }

  private void respond(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] bytes = json.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @Override
  public void close() {
    server.stop(0);
    executor.shutdownNow();
  }
}
