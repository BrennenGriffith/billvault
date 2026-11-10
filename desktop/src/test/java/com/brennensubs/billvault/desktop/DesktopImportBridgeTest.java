package com.brennensubs.billvault.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class DesktopImportBridgeTest {
  @TempDir Path directory;

  @Test
  void authenticatedBridgeUsesOpenStoreRejectsBrowserAndCommitsRetrySafely() throws Exception {
    int port;
    try (var socket = new java.net.ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    String token = "a".repeat(64);
    String receiptId;
    String savedBody;
    try (var store = new DesktopStore(directory)) {
      store.enableImports(port, token, () -> {});
      var client = HttpClient.newHttpClient();
      String base = "http://127.0.0.1:" + port;
      assertEquals(
          401,
          client
              .send(
                  HttpRequest.newBuilder(URI.create(base + "/api/categories")).GET().build(),
                  HttpResponse.BodyHandlers.ofString())
              .statusCode());
      assertEquals(
          403,
          client
              .send(
                  HttpRequest.newBuilder(URI.create(base + "/api/categories"))
                      .header("Authorization", "Bearer " + token)
                      .header("Origin", "https://example.com")
                      .GET()
                      .build(),
                  HttpResponse.BodyHandlers.ofString())
              .statusCode());
      assertThrows(java.io.IOException.class, () -> new DesktopStore(directory));
      long category = store.service.categories().getFirst().id();
      String body =
          """
          {"source":"fictional fixture","expenses":[{"name":"Demo stream","kind":"SUBSCRIPTION",
          "categoryId":%d,"amount":"14.99","currency":"USD","frequency":"MONTHLY",
          "startDate":"2026-11-01","nextDueDate":"2026-12-01","status":"ACTIVE","variableAmount":false}]}
          """
              .formatted(category);
      var preview = send(client, base + "/api/imports/preview", token, body);
      assertEquals(200, preview.statusCode());
      assertEquals(0, store.service.list(null, null, null, null, "name", 0, 20).totalElements());
      String id = JsonMapper.builder().build().readTree(preview.body()).get("previewId").asString();
      receiptId = id;
      var saved =
          send(client, base + "/api/imports/" + id + "/commit", token, "{\"confirmed\":true}");
      savedBody = saved.body();
      assertEquals(200, saved.statusCode());
      assertEquals(
          saved.body(),
          send(client, base + "/api/imports/" + id + "/commit", token, "{\"confirmed\":true}")
              .body());
      assertEquals(1, store.service.list(null, null, null, null, "name", 0, 20).totalElements());
      assertEquals(404, send(client, base + "/api/expenses", token, body).statusCode());
    }
    try (var reopened = new DesktopStore(directory)) {
      reopened.enableImports(port, token, () -> {});
      assertEquals(
          savedBody,
          send(
                  HttpClient.newHttpClient(),
                  "http://127.0.0.1:" + port + "/api/imports/" + receiptId + "/commit",
                  token,
                  "{\"confirmed\":true}")
              .body());
      assertEquals(1, reopened.service.list(null, null, null, null, "name", 0, 20).totalElements());
    }
  }

  private HttpResponse<String> send(HttpClient client, String url, String token, String body)
      throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
