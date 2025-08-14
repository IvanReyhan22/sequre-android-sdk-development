package id.sequre.scanner_sdk.data.remote.retrofit

import android.util.Log
import id.sequre.scanner_sdk.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

enum class ApiType {
    BASE,
    CLASSIFICATION,
}

object ApiConfig {
    private val baseService: ApiService by lazy { createService(ApiType.BASE) }
    private val classificationService: ApiService by lazy { createService(ApiType.CLASSIFICATION) }

    fun getApiService(type: ApiType = ApiType.BASE): ApiService {
        return when (type) {
            ApiType.BASE -> baseService
            ApiType.CLASSIFICATION -> classificationService
        }
    }

    private fun createService(type: ApiType): ApiService {
        val baseUrl = when (type) {
            ApiType.BASE -> BuildConfig.BASE_API_URL
            ApiType.CLASSIFICATION -> BuildConfig.API_CLASSIFICATION_URL
        }

        Log.e("SDKApiConfig", "Base URL: $baseUrl")

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
//
//    fun getApiService(type: ClassificationType = ClassificationType.MAIN): ApiService {
//        val baseUrl = when (type) {
//            ClassificationType.MAIN -> BuildConfig.BASE_API_URL_MAIN
//            ClassificationType.THIRD_PARTY -> BuildConfig.BASE_API_URL_THIRD_PARTY
//        }
//
//        val loggingInterceptor: HttpLoggingInterceptor = if (BuildConfig.DEBUG) {
//            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
//        } else {
//            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE)
//        }
//        val client: OkHttpClient = OkHttpClient
//            .Builder()
//            .addInterceptor(loggingInterceptor)
//            .build()
//        val retrofit: Retrofit = Retrofit.Builder()
//            .baseUrl(baseUrl)
//            .addConverterFactory(GsonConverterFactory.create())
//            .client(client)
//            .build()
//        return retrofit.create(ApiService::class.java)
//    }
}