package com.brennensubs.billvault;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.profiles.group.personal=local",
      "BILLVAULT_DATA_DIR=./target/personal-profile-test"
    })
@ActiveProfiles("personal")
class PersonalProfileTests {
  @Autowired Environment environment;
  @LocalServerPort int port;

  @Test
  void personalConfigurationUsesSelectedDataDirectoryAndServesBuiltFrontend() throws Exception {
    assertTrue(
        environment
            .getRequiredProperty("spring.datasource.url")
            .contains("target/personal-profile-test/billvault"));
    assertEquals("127.0.0.1", environment.getRequiredProperty("server.address"));
    assertEquals("false", environment.getRequiredProperty("springdoc.api-docs.enabled"));
    HttpClient client = HttpClient.newHttpClient();
    var response =
        client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("<title>BillVault"));
    assertTrue(
        response
            .headers()
            .firstValue("Content-Security-Policy")
            .orElse("")
            .contains("default-src 'self'"));
  }
}
