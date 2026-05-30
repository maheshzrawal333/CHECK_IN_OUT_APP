package com.maheshz.di

import android.content.Context
import androidx.room.Room
import com.maheshz.ble.FpPacketAdvertiser
import com.maheshz.ble.IotBeaconScanner
import com.maheshz.ble.ResultReceiver
import com.maheshz.data.local.AppDatabase
import com.maheshz.data.remote.ApiService
import com.maheshz.data.remote.AuthInterceptor
import com.maheshz.data.remote.TokenAuthenticator
import com.maheshz.data.repository.AttendanceRepository
import com.maheshz.data.repository.AuthRepository
import com.maheshz.util.DataStoreManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppContainer(private val context: Context) {
    val dataStoreManager: DataStoreManager by lazy {
        DataStoreManager(context)
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(dataStoreManager))
            .authenticator(TokenAuthenticator(dataStoreManager) { apiService })
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.example.com") // fallback, should be configured per env
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "checkin_db").build()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, dataStoreManager)
    }

    val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepository(appDatabase.attendanceDao())
    }

    val iotBeaconScanner: IotBeaconScanner by lazy { IotBeaconScanner(context) }
    val fpPacketAdvertiser: FpPacketAdvertiser by lazy { FpPacketAdvertiser(context) }
    val resultReceiver: ResultReceiver by lazy { ResultReceiver(context) }
}
