package com.tsarshield.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tsarshield.BuildConfig
import com.tsarshield.data.network.TsarShieldApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для предоставления зависимостей сетевого слоя
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = BuildConfig.BASE_URL
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    
    /**
     * Предоставляет Moshi для JSON сериализации/десериализации
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    /**
     * Предоставляет HttpLoggingInterceptor для отладки сетевых запросов
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * Предоставляет CertificatePinner для certificate pinning
     */
    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            // TODO: Добавить реальные хэши сертификатов сервера
            .add("api.tsar-shield.ru", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    }
    
    /**
     * Предоставляет OkHttpClient с настройками безопасности и логированием
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        certificatePinner: CertificatePinner
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("User-Agent", "TsarShield-Android/${BuildConfig.VERSION_NAME}")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                
                // TODO: Добавить JWT токен при наличии
                // val token = tokenManager.getToken()
                // if (!token.isNullOrEmpty()) {
                //     requestBuilder.header("Authorization", "Bearer $token")
                // }
                
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .certificatePinner(certificatePinner)
            .build()
    }
    
    /**
     * Предоставляет Retrofit клиент
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    /**
     * Предоставляет API интерфейс TsarShieldApi
     */
    @Provides
    @Singleton
    fun provideTsarShieldApi(retrofit: Retrofit): TsarShieldApi {
        return retrofit.create(TsarShieldApi::class.java)
    }
}