package mec0why.lime.data.auth

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class AuthManager(context: Context, private val clientId: String, private val clientSecret: String) {

    companion object {
        private const val REDIRECT_URI = "https://mec0why.github.io/lime/callback"
        private const val AUTH_URL = "https://id.kick.com/oauth/authorize"
        private const val TOKEN_URL = "https://id.kick.com/oauth/token"
        private const val SCOPES = "chat:write user:read"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_PENDING_VERIFIER = "pending_verifier"
        private const val KEY_PENDING_STATE = "pending_state"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "lime_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    private val _isLoggedIn = MutableStateFlow(prefs.getString(KEY_ACCESS_TOKEN, null) != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<mec0why.lime.data.api.KickUserData?>(null)
    val currentUser: StateFlow<mec0why.lime.data.api.KickUserData?> = _currentUser

    init {
        if (_isLoggedIn.value) {
            kotlinx.coroutines.GlobalScope.launch {
                fetchCurrentUser()
            }
        }
    }

    suspend fun fetchCurrentUser() {
        val token = getAccessToken() ?: return
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.kick.com/public/v1/users")
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext
                val userData = json.decodeFromString<mec0why.lime.data.api.KickUserResponse>(body)
                _currentUser.value = userData.data?.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun buildAuthUrl(): String {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        val state = generateState()

        prefs.edit()
            .putString(KEY_PENDING_VERIFIER, verifier)
            .putString(KEY_PENDING_STATE, state)
            .apply()

        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    suspend fun exchangeCode(code: String, state: String): Boolean {
        val savedState = prefs.getString(KEY_PENDING_STATE, null)
        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null)
        if (state != savedState || verifier == null) return false

        return withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("redirect_uri", REDIRECT_URI)
                    .add("code_verifier", verifier)
                    .build()

                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body.string()
                val tokenResponse = json.decodeFromString<TokenResponse>(responseBody)

                if (tokenResponse.accessToken.isNotBlank()) {
                    saveTokens(tokenResponse)
                    prefs.edit()
                        .remove(KEY_PENDING_VERIFIER)
                        .remove(KEY_PENDING_STATE)
                        .apply()
                    fetchCurrentUser()
                    true
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun refreshToken(): Boolean {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .build()

                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body.string()
                val tokenResponse = json.decodeFromString<TokenResponse>(responseBody)

                if (tokenResponse.accessToken.isNotBlank()) {
                    saveTokens(tokenResponse)
                    fetchCurrentUser()
                    true
                } else {
                    logout()
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (System.currentTimeMillis() >= expiresAt) return null
        return token
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
        _isLoggedIn.value = false
        _currentUser.value = null
    }

    private fun saveTokens(response: TokenResponse) {
        val expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
        _isLoggedIn.value = true
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(96)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("token_type") val tokenType: String = ""
)
