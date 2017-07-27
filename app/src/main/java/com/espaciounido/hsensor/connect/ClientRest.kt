package com.espaciounido.hsensor.connect

import java.util.concurrent.TimeUnit

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by MyMac on 1/09/16.
 */
class ClientRest {

    internal val api: SensorNodeMCUService

    init {

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(40, TimeUnit.SECONDS)
                .connectTimeout(40, TimeUnit.SECONDS)
                .build()


        val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.4.1/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        api = retrofit.create(SensorNodeMCUService::class.java)
    }

    internal interface SensorNodeMCUService {
        @GET("settingWifi")
        fun settingWifi(@Query("ssid") ssid: String, @Query("password") password: String, @Query("ip") ip: String): Call<ResponseNodeMCU>
    }

    class ResponseNodeMCU {
        var status: String? = null
        var node: String? = null
        var ssid: String? = null
        var password: String? = null
    }
}
