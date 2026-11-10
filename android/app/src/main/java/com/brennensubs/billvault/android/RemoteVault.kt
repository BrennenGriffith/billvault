package com.brennensubs.billvault.android

import java.net.URI
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RemoteFailure(val status: Int, message: String) : Exception(message)

// Desktop mode trusts only the explicitly paired certificate, including its validity dates.
// Self-hosted mode retains Android/system CA and hostname validation.
@android.annotation.SuppressLint("CustomX509TrustManager")
class RemoteVault(private val profile: Profile, private val secret: String) {
    private val cookies = mutableListOf<Cookie>()
    private var csrfToken = ""
    private var csrfHeader = "X-CSRF-TOKEN"
    private val base = validatedUrl(profile.url, profile.mode, BuildConfig.DEBUG)
    private val client: OkHttpClient

    init {
        val builder =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
        if (profile.mode == VaultMode.DESKTOP) {
            val expected = profile.fingerprint.lowercase().replace(":", "").replace(" ", "")
            require(expected.matches(Regex("[0-9a-f]{64}"))) {
                "Copy the desktop certificate fingerprint"
            }
            val trust =
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) {
                        throw java.security.cert.CertificateException("Client trust not supported")
                    }

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) {
                        require(chain.isNotEmpty()) { "Missing desktop certificate" }
                        chain[0].checkValidity()
                        if (fingerprint(chain[0]) != expected)
                            throw java.security.cert.CertificateException(
                                "Desktop identity changed; pair again"
                            )
                    }
                }
            val ssl =
                SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf(trust), java.security.SecureRandom())
                }
            builder.sslSocketFactory(ssl.socketFactory, trust).hostnameVerifier { _, session ->
                runCatching {
                        fingerprint(session.peerCertificates[0] as X509Certificate) == expected
                    }
                    .getOrDefault(false)
            }
        } else {
            builder.cookieJar(
                object : CookieJar {
                    override fun saveFromResponse(url: HttpUrl, new: List<Cookie>) {
                        synchronized(cookies) {
                            new.forEach { cookie ->
                                cookies.removeAll {
                                    it.name == cookie.name &&
                                        it.domain == cookie.domain &&
                                        it.path == cookie.path
                                }
                                cookies += cookie
                            }
                        }
                    }

                    override fun loadForRequest(url: HttpUrl) =
                        synchronized(cookies) {
                            cookies.filter {
                                it.matches(url) && it.expiresAt > System.currentTimeMillis()
                            }
                        }
                }
            )
        }
        client = builder.build()
    }

    fun login() {
        if (profile.mode != VaultMode.SELF_HOSTED) return
        require(secret.isNotBlank()) { "Sign in before syncing" }
        val token = request("/api/auth/csrf")
        csrfToken = token.getString("token")
        csrfHeader = token.getString("headerName")
        request(
            "/api/auth/login",
            "POST",
            form =
                FormBody.Builder()
                    .add("username", profile.username)
                    .add("password", secret)
                    .build(),
        )
        val refreshed = request("/api/auth/csrf")
        csrfToken = refreshed.getString("token")
        csrfHeader = refreshed.getString("headerName")
    }

    fun pair(code: String): String =
        request(
                "/api/mobile/pair",
                "POST",
                JSONObject().put("code", code).put("name", "BillVault Android"),
            )
            .getString("token")

    fun snapshot() = Snapshot.parse(request("/api/mobile/snapshot"))

    fun command(p: Pending): JSONObject =
        request(
            "/api/mobile/commands",
            "POST",
            JSONObject()
                .put("commandId", p.id)
                .put("operation", p.operation)
                .put("expenseId", p.remoteId ?: JSONObject.NULL)
                .put("payload", p.payload),
        )

    private fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        form: RequestBody? = null,
    ): JSONObject {
        val builder = Request.Builder().url(base + path).header("Accept", "application/json")
        if (profile.mode == VaultMode.DESKTOP && secret.isNotBlank())
            builder.header("Authorization", "Bearer $secret")
        if (profile.mode == VaultMode.SELF_HOSTED && method != "GET" && csrfToken.isNotBlank())
            builder.header(csrfHeader, csrfToken)
        builder.method(
            method,
            if (method == "GET") null
            else
                form
                    ?: (body ?: JSONObject())
                        .toString()
                        .toRequestBody("application/json".toMediaType()),
        )
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful)
                throw RemoteFailure(
                    response.code,
                    when (response.code) {
                        401,
                        403 -> "Sign-in or pairing expired; reconnect in settings"
                        409 ->
                            "An expense changed on the host. Pending changes are preserved; review before retrying or discarding them."
                        else ->
                            "Host rejected sync (HTTP ${response.code}); pending changes are preserved"
                    },
                )
            if (response.code == 204) return JSONObject()
            val source = response.body?.source() ?: error("Empty host response")
            require(!source.request(20L * 1024 * 1024 + 1)) {
                "Snapshot exceeds the 20 MB mobile limit"
            }
            return JSONObject(source.readUtf8())
        }
    }

    companion object {
        fun validatedUrl(value: String, mode: VaultMode, debug: Boolean): String {
            val uri = URI(value.trim())
            require(
                uri.host != null &&
                    uri.userInfo == null &&
                    uri.query == null &&
                    uri.fragment == null
            ) {
                "Enter a URL without credentials or query parameters"
            }
            val development =
                debug &&
                    uri.scheme == "http" &&
                    uri.host in listOf("10.0.2.2", "127.0.0.1", "localhost")
            require(uri.scheme == "https" || (mode == VaultMode.SELF_HOSTED && development)) {
                "Use HTTPS; debug builds also support the Android emulator host at http://10.0.2.2:8080"
            }
            return uri.toString().trimEnd('/')
        }

        fun fingerprint(cert: X509Certificate) =
            MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") {
                "%02x".format(it)
            }
    }
}
