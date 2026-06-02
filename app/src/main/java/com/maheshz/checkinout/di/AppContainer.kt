package com.maheshz.checkinout.di

import android.content.Context
import androidx.room.Room
import com.maheshz.checkinout.ble.FpPacketAdvertiser
import com.maheshz.checkinout.ble.IotBeaconScanner
import com.maheshz.checkinout.ble.ResultReceiver
import com.maheshz.checkinout.data.local.AppDatabase
import com.maheshz.checkinout.data.remote.ApiService
import com.maheshz.checkinout.data.remote.AuthInterceptor
import com.maheshz.checkinout.data.remote.TokenAuthenticator
import com.maheshz.checkinout.data.repository.AttendanceRepository
import com.maheshz.checkinout.data.repository.AuthRepository
import com.maheshz.checkinout.util.DataStoreManager
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
            // 🌟 FIXED: Points your employee app to your live running server tunnel link
            .baseUrl("https://subprime-encircle-guiding.ngrok-free.dev/")
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
        AttendanceRepository(
            dao = appDatabase.attendanceDao(), // 🌟 FIXED: Changed 'database' to 'appDatabase' to match your variable above
            apiService = apiService
        )
    }

    val iotBeaconScanner: IotBeaconScanner by lazy { IotBeaconScanner(context) }
    val fpPacketAdvertiser: FpPacketAdvertiser by lazy { FpPacketAdvertiser(context) }
    val resultReceiver: ResultReceiver by lazy { ResultReceiver(context) }
}