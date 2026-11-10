package com.brennensubs.billvault.android

import java.util.UUID
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RemoteVaultTest {
    @Test
    fun desktopTrustRequiresTheExactPairedCertificate() {
        val certificate =
            okhttp3.tls.HeldCertificate.Builder()
                .commonName("Fictional desktop")
                .addSubjectAlternativeName("localhost")
                .build()
        val tls = okhttp3.tls.HandshakeCertificates.Builder().heldCertificate(certificate).build()
        MockWebServer().use { server ->
            server.useHttps(tls.sslSocketFactory(), false)
            server.enqueue(MockResponse().setBody(Snapshot.empty().json().toString()))
            val profile =
                Profile(
                    mode = VaultMode.DESKTOP,
                    label = "Test desktop",
                    url = server.url("/").toString(),
                    fingerprint = RemoteVault.fingerprint(certificate.certificate),
                )
            assertTrue(RemoteVault(profile, "fixture-token").snapshot().expenses.isEmpty())
            assertEquals("Bearer fixture-token", server.takeRequest().getHeader("Authorization"))
            assertThrows(javax.net.ssl.SSLHandshakeException::class.java) {
                RemoteVault(profile.copy(fingerprint = "0".repeat(64)), "fixture-token").snapshot()
            }
        }
    }

    @Test
    fun loginRotatesCookiesAndRefreshesCsrfBeforeSync() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody("{\"token\":\"before\",\"headerName\":\"X-CSRF-TOKEN\"}")
                    .addHeader("Set-Cookie", "JSESSIONID=before; Path=/; HttpOnly")
            )
            server.enqueue(
                MockResponse()
                    .setResponseCode(204)
                    .addHeader("Set-Cookie", "JSESSIONID=after; Path=/; HttpOnly")
            )
            server.enqueue(
                MockResponse().setBody("{\"token\":\"after\",\"headerName\":\"X-CSRF-TOKEN\"}")
            )
            server.enqueue(MockResponse().setBody(Snapshot.empty().json().toString()))
            server.enqueue(MockResponse().setBody("{\"commandId\":\"fixture\"}"))
            val profile =
                Profile(
                    mode = VaultMode.SELF_HOSTED,
                    label = "Test",
                    url = server.url("/").toString(),
                    username = "fictional",
                )
            val remote = RemoteVault(profile, "fixture-password")
            remote.login()
            remote.snapshot()
            val command =
                Pending(
                    UUID.randomUUID().toString(),
                    "CREATE",
                    -1,
                    null,
                    JSONObject().put("name", "Fictional"),
                )
            remote.command(command)
            assertEquals("/api/auth/csrf", server.takeRequest().path)
            val login = server.takeRequest()
            assertEquals("before", login.getHeader("X-CSRF-TOKEN"))
            assertTrue(login.body.readUtf8().contains("username=fictional"))
            assertEquals("JSESSIONID=after", server.takeRequest().getHeader("Cookie"))
            assertEquals("/api/mobile/snapshot", server.takeRequest().path)
            val write = server.takeRequest()
            assertEquals("after", write.getHeader("X-CSRF-TOKEN"))
            assertEquals(command.id, JSONObject(write.body.readUtf8()).getString("commandId"))
        }
    }

    @Test
    fun conflictsRemainExplicitAndDoNotFollowRedirects() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
                    .setBody("sensitive details should not be reflected")
            )
            val remote =
                RemoteVault(
                    Profile(
                        mode = VaultMode.SELF_HOSTED,
                        label = "Test",
                        url = server.url("/").toString(),
                    ),
                    "fixture",
                )
            val error = assertThrows(RemoteFailure::class.java) { remote.snapshot() }
            assertEquals(409, error.status)
            assertFalse(error.message!!.contains("sensitive details"))
            server.enqueue(
                MockResponse().setResponseCode(302).addHeader("Location", "https://example.com")
            )
            assertEquals(302, assertThrows(RemoteFailure::class.java) { remote.snapshot() }.status)
            assertEquals(2, server.requestCount)
        }
    }
}
