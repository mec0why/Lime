package mec0why.lime

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import mec0why.lime.data.api.KickApi
import mec0why.lime.data.api.KickUnofficialApi
import mec0why.lime.data.api.SevenTVApi
import mec0why.lime.data.api.TokenResponse
import mec0why.lime.data.repository.FollowingRepository
import mec0why.lime.data.repository.KickRepository
import mec0why.lime.data.repository.SevenTVRepository
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class LimeApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val imageClient = OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Referer", "https://kick.com/")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageClient)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(coil.decode.SvgDecoder.Factory())
            }
            .build()
    }

    lateinit var repository: KickRepository
        private set

    lateinit var sevenTVRepository: SevenTVRepository
        private set

    lateinit var followingRepository: FollowingRepository
        private set

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Volatile
    private var accessToken: String? = null

    private val tokenClient = OkHttpClient.Builder().build()

    private fun fetchToken(): String {
        return try {
            val requestBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", BuildConfig.KICK_CLIENT_ID)
                .add("client_secret", BuildConfig.KICK_CLIENT_SECRET)
                .build()

            val request = Request.Builder()
                .url("https://id.kick.com/oauth/token")
                .post(requestBody)
                .build()

            val response = tokenClient.newCall(request).execute()
            val body = response.body.string()
            val tokenResponse = json.decodeFromString<TokenResponse>(body)
            tokenResponse.accessToken
        } catch (_: Exception) {
            ""
        }
    }

    override fun onCreate() {
        super.onCreate()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authInterceptor = Interceptor { chain ->
            val token = accessToken ?: synchronized(this) {
                accessToken ?: fetchToken().also { accessToken = it }
            }

            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        val officialClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val officialRetrofit = Retrofit.Builder()
            .baseUrl("https://api.kick.com/")
            .client(officialClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val unofficialHeaderInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .header("Referer", "https://kick.com/")
                .build()
            chain.proceed(request)
        }

        val unofficialClient = OkHttpClient.Builder()
            .addInterceptor(unofficialHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val unofficialRetrofit = Retrofit.Builder()
            .baseUrl("https://kick.com/")
            .client(unofficialClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val sevenTvRetrofit = Retrofit.Builder()
            .baseUrl("https://7tv.io/v3/")
            .client(unofficialClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = officialRetrofit.create(KickApi::class.java)
        val unofficialApi = unofficialRetrofit.create(KickUnofficialApi::class.java)
        val sevenTvApi = sevenTvRetrofit.create(SevenTVApi::class.java)

        repository = KickRepository(api, unofficialApi)
        sevenTVRepository = SevenTVRepository(sevenTvApi)
        followingRepository = FollowingRepository(this)
        
        coil.Coil.setImageLoader(newImageLoader())
    }
}
