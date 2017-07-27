package com.espaciounido.hsensor.connect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.AppCompatButton
import android.view.View
import android.widget.EditText
import android.widget.ScrollView


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.espaciounido.hsensor.R
import com.espaciounido.hsensor.TinyDB
import android.net.NetworkInfo
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class ConfigActivity : WifiBaseActivity(), View.OnClickListener {

    internal var clientRest = ClientRest()

    private var password: EditText? = null
    private var ssid: EditText? = null
    private var ip: EditText? = null

    private var scrollView: ScrollView? = null
    private var btnConfig: AppCompatButton? = null

    override val secondsTimeout: Int = TIMEOUT
    override val wifiSSID: String get() {
        if(resources == null){
            return ""
        }
       return getString(R.string.wifi_ssid)
    }
    override val wifiPass: String get() {
        if(resources == null){
            return ""
        }
        return getString(R.string.wifi_password)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        ip = findViewById(R.id.input_ip) as EditText?
        password = findViewById(R.id.input_password) as EditText?
        ssid = findViewById(R.id.input_ssid) as EditText?
        scrollView = findViewById(R.id.scrollView) as ScrollView?
        btnConfig = findViewById(R.id.btn_config) as AppCompatButton?

        btnConfig!!.setOnClickListener(this)

        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifi.connectionInfo
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        if(info.ssid.length > 2 && networkInfo != null && networkInfo.isConnected){
            ssid!!.setText(info.ssid.replace("\"", ""))
        }

        handleWIFI()
    }


    fun sendConfiguration(view: View) {
        val textSsid = ssid!!.text.toString()
        val textPassword = password!!.text.toString()
        val textIp = ip!!.text.toString()
        if (textPassword == "" || textPassword == "" || textIp == "") {
            Snackbar.make(view, "Debe ingresar todos los campos", Snackbar.LENGTH_LONG).show()
            return
        }
        clientRest.api.settingWifi(textSsid, textPassword, textIp)
                .enqueue(object : Callback<ClientRest.ResponseNodeMCU> {
                    override fun onResponse(call: Call<ClientRest.ResponseNodeMCU>,
                                            response: Response<ClientRest.ResponseNodeMCU>) {
                        if (response.body() != null && response.body().status == "success") {
                            Snackbar.make(view, "Wifi Configurada", Snackbar.LENGTH_LONG).show()
                            startSuccessActivity()
                        } else {
                            Snackbar.make(view, "No se pudo configurar", Snackbar.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ClientRest.ResponseNodeMCU>, t: Throwable) {
                        t.printStackTrace()
                        handleWIFI()
                        Snackbar.make(view, "Existe problemas en encontrar el sensor", Snackbar.LENGTH_LONG).show()
                    }
                })
    }

    private fun startSuccessActivity() {
        val intent = Intent(this, SuccessActivity::class.java)
        if(isServerReachable(this, ip!!.text.toString())){
            TinyDB.getInstance(this).putString("url_broker", ip!!.text.toString())
        }
        startActivity(intent)
        finish()

    }

    override fun onClick(view: View) {
        sendConfiguration(view)
    }

    fun isServerReachable(context: Context, ip: String): Boolean {
        val connMan = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connMan.activeNetworkInfo
        if (netInfo != null && netInfo.isConnected) {
            try {
                val urlServer = URL("http://"+ip)
                val urlConn = urlServer.openConnection() as HttpURLConnection
                urlConn.connectTimeout = 3000
                urlConn.connect()
                return urlConn.responseCode == 200
            } catch (e1: MalformedURLException) {
                return false
            } catch (e: IOException) {
                return false
            }

        }
        return false
    }

    companion object {
        private val TIMEOUT = 30
    }
}
