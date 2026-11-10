package com.brennensubs.billvault.desktop;

import com.brennensubs.billvault.dto.MobileApi.Command;
import com.brennensubs.billvault.service.MobileSyncService;
import com.sun.net.httpserver.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** An explicitly enabled TLS companion listener, separate from the loopback-only MCP bridge. */
public final class DesktopMobileBridge implements AutoCloseable {
  public record Pairing(
      String fingerprint, String code, Instant expiresAt, int port, List<String> addresses) {}

  private final HttpsServer server;
  private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
  private final MobileSyncService service;
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final Runnable changed;
  private final String fingerprint;
  private String pairCode;
  private Instant expires;
  private int attempts;

  public DesktopMobileBridge(
      Path directory,
      int port,
      MobileSyncService service,
      JdbcTemplate jdbc,
      ObjectMapper json,
      Runnable changed)
      throws Exception {
    this.service = service;
    this.jdbc = jdbc;
    this.json = json;
    this.changed = changed;
    var store = identity(directory.resolve("mobile-identity.p12"));
    var cert = (X509Certificate) store.getCertificate("desktop");
    fingerprint =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
    var km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    km.init(store, new char[0]);
    var tls = SSLContext.getInstance("TLS");
    tls.init(km.getKeyManagers(), null, new SecureRandom());
    server = HttpsServer.create(new InetSocketAddress("0.0.0.0", port), 8);
    server.setHttpsConfigurator(new HttpsConfigurator(tls));
    server.setExecutor(workers);
    server.createContext("/api/mobile/", this::handle);
    newCode();
    server.start();
  }

  private static KeyStore identity(Path file) throws Exception {
    var store = KeyStore.getInstance("PKCS12");
    if (Files.exists(file)) {
      try (var stream = Files.newInputStream(file)) {
        store.load(stream, new char[0]);
      }
      return store;
    }
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(3072);
    var keys = generator.generateKeyPair();
    Instant now = Instant.now();
    var name = new X500Name("CN=BillVault Desktop");
    var builder =
        new JcaX509v3CertificateBuilder(
            name,
            new BigInteger(160, new SecureRandom()),
            Date.from(now.minusSeconds(300)),
            Date.from(now.plus(Duration.ofDays(3650))),
            name,
            keys.getPublic());
    var certificate =
        new JcaX509CertificateConverter()
            .getCertificate(
                builder.build(
                    new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())));
    store.load(null, new char[0]);
    store.setKeyEntry(
        "desktop",
        keys.getPrivate(),
        new char[0],
        new java.security.cert.Certificate[] {certificate});
    Files.createDirectories(file.getParent());
    try (var stream = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
      store.store(stream, new char[0]);
    }
    try {
      Files.setPosixFilePermissions(
          file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    } catch (UnsupportedOperationException ignored) {
    }
    return store;
  }

  public synchronized Pairing newCode() {
    pairCode = String.format(Locale.ROOT, "%012d", new SecureRandom().nextLong(1_000_000_000_000L));
    expires = Instant.now().plusSeconds(300);
    attempts = 0;
    return pairing();
  }

  public synchronized Pairing pairing() {
    List<String> addresses = new ArrayList<>();
    try {
      var interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        var network = interfaces.nextElement();
        if (!network.isUp() || network.isLoopback()) continue;
        for (var address : Collections.list(network.getInetAddresses()))
          if (address instanceof Inet4Address && address.isSiteLocalAddress())
            addresses.add(
                "https://" + address.getHostAddress() + ":" + server.getAddress().getPort());
      }
    } catch (SocketException ignored) {
    }
    return new Pairing(
        fingerprint, pairCode, expires, server.getAddress().getPort(), List.copyOf(addresses));
  }

  public void revokeDevices() {
    jdbc.update("UPDATE mobile_devices SET revoked=TRUE");
  }

  private synchronized Map<String, Object> pair(byte[] body) {
    var node = json.readTree(body);
    String code = node.path("code").asString("");
    if (attempts >= 5
        || expires.isBefore(Instant.now())
        || !MessageDigest.isEqual(
            pairCode.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            code.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      attempts++;
      throw new IllegalArgumentException("Pairing rejected");
    }
    String token = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));
    String id = UUID.randomUUID().toString();
    String name = node.path("name").asString("Android phone").strip();
    if (name.isBlank() || name.length() > 80)
      throw new IllegalArgumentException("Device name required");
    jdbc.update(
        "INSERT INTO mobile_devices(id,name,token_hash,paired_at,revoked) VALUES(?,?,?,?,FALSE)",
        id,
        name,
        MobileSyncService.hash(token),
        Instant.now());
    expires = Instant.EPOCH;
    pairCode = "";
    return Map.of("token", token, "deviceId", id, "schemaVersion", 1);
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      if (exchange.getRequestHeaders().containsKey("Origin")) {
        respond(exchange, 403, Map.of("detail", "Browser requests are not allowed"));
        return;
      }
      try {
        String path = exchange.getRequestURI().getPath(), method = exchange.getRequestMethod();
        Object result;
        if (path.equals("/api/mobile/pair") && method.equals("POST")) result = pair(body(exchange));
        else {
          String header = exchange.getRequestHeaders().getFirst("Authorization");
          if (header == null
              || !header.startsWith("Bearer ")
              || jdbc.queryForObject(
                      "SELECT COUNT(*) FROM mobile_devices WHERE token_hash=? AND revoked=FALSE",
                      Integer.class,
                      MobileSyncService.hash(header.substring(7)))
                  != 1) {
            respond(exchange, 401, Map.of("detail", "Pair this device again in desktop settings"));
            return;
          }
          if (path.equals("/api/mobile/snapshot") && method.equals("GET"))
            result = service.snapshot();
          else if (path.equals("/api/mobile/commands") && method.equals("POST")) {
            result = service.command(json.readValue(body(exchange), Command.class));
            changed.run();
          } else {
            respond(exchange, 404, Map.of("detail", "Mobile operation not found"));
            return;
          }
        }
        respond(exchange, 200, result);
      } catch (ResponseStatusException e) {
        respond(
            exchange,
            e.getStatusCode().value(),
            Map.of("detail", Objects.requireNonNullElse(e.getReason(), "Request rejected")));
      } catch (org.springframework.dao.DataIntegrityViolationException e) {
        respond(exchange, 409, Map.of("detail", "Concurrent command; retry sync"));
      } catch (IllegalArgumentException | tools.jackson.core.JacksonException e) {
        respond(exchange, 400, Map.of("detail", "Invalid command or expired pairing code"));
      } catch (Exception e) {
        respond(exchange, 500, Map.of("detail", "Mobile request failed; retry the same command"));
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

  private void respond(HttpExchange exchange, int status, Object result) throws IOException {
    byte[] bytes = json.writeValueAsBytes(result);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  public void close() {
    server.stop(0);
    workers.shutdownNow();
  }
}
