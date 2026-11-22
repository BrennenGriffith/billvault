package com.brennensubs.billvault.desktop;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class DesktopMobileBridgeTest {
  @TempDir Path directory;

  @Test
  void tlsPairingTokenAndRevocationProtectRealVault() throws Exception {
    try (var store = new DesktopStore(directory)) {
      var pairing = store.enableMobile(0, () -> {});
      var json = JsonMapper.builder().build();
      String base = "https://127.0.0.1:" + pairing.port();
      var tls = SSLContext.getInstance("TLS");
      tls.init(
          null,
          new TrustManager[] {
            new X509TrustManager() {
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }

              public void checkClientTrusted(X509Certificate[] c, String a) {}

              public void checkServerTrusted(X509Certificate[] c, String a)
                  throws java.security.cert.CertificateException {
                try {
                  if (!java.util.HexFormat.of()
                      .formatHex(MessageDigest.getInstance("SHA-256").digest(c[0].getEncoded()))
                      .equals(pairing.fingerprint()))
                    throw new java.security.cert.CertificateException("Wrong identity");
                } catch (java.security.NoSuchAlgorithmException e) {
                  throw new RuntimeException(e);
                }
              }
            }
          },
          new SecureRandom());
      assertEquals(401, request(tls, base + "/api/mobile/snapshot", null, null).status());
      assertEquals(
          400, request(tls, base + "/api/mobile/pair", null, "{\"code\":\"wrong\"}").status());
      var response =
          request(
              tls,
              base + "/api/mobile/pair",
              null,
              "{\"code\":\"" + pairing.code() + "\",\"name\":\"Fictional phone\"}");
      assertEquals(200, response.status());
      String token = json.readTree(response.body()).get("token").asString();
      assertEquals(
          400,
          request(tls, base + "/api/mobile/pair", null, "{\"code\":\"" + pairing.code() + "\"}")
              .status());
      assertEquals(200, request(tls, base + "/api/mobile/snapshot", token, null).status());
      store.revokeMobile();
      assertEquals(401, request(tls, base + "/api/mobile/snapshot", token, null).status());
      store.disableMobile();
      assertThrows(
          IOException.class, () -> request(tls, base + "/api/mobile/snapshot", token, null));
    }
  }

  record Reply(int status, String body) {}

  private Reply request(SSLContext tls, String url, String token, String body) throws Exception {
    var connection = (HttpsURLConnection) URI.create(url).toURL().openConnection();
    connection.setConnectTimeout(3000);
    connection.setReadTimeout(5000);
    connection.setSSLSocketFactory(tls.getSocketFactory());
    connection.setHostnameVerifier((host, session) -> host.equals("127.0.0.1"));
    if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
    if (body != null) {
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");
      try (var out = connection.getOutputStream()) {
        out.write(body.getBytes(StandardCharsets.UTF_8));
      }
    }
    int status = connection.getResponseCode();
    String text;
    try (var input = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
      text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
    connection.disconnect();
    return new Reply(status, text);
  }
}
