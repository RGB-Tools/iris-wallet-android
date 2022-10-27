package com.iriswallet.data.retrofit

import com.google.gson.GsonBuilder
import com.iriswallet.BuildConfig
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitModule {

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        val gson = GsonBuilder().disableHtmlEscaping().create()
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(baseUrl)
            .client(client)
            .build()
    }

    private fun getHttpClient(): OkHttpClient {
        val client =
            OkHttpClient.Builder()
                .readTimeout(AppConstants.httpReadWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(AppConstants.httpReadWriteTimeout, TimeUnit.SECONDS)
                .connectTimeout(AppConstants.httpConnectTimeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            client.addInterceptor(loggingInterceptor)
        }
        return client.build()
    }

    val btcFaucetApiService: BtcFaucetApiService by lazy {
        if (AppContainer.btcFaucetURLS == null)
            throw IllegalArgumentException("a configured URL is required")
        retrofit(AppContainer.btcFaucetURLS!!, getHttpClient())
            .create(BtcFaucetApiService::class.java)
    }

    fun getRgbFaucetApiService(url: String): RgbFaucetApiService {
        return retrofit(url, getHttpClient()).create(RgbFaucetApiService::class.java)
    }
}
