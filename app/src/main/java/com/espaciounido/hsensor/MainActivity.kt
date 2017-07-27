package com.espaciounido.hsensor

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebSettings
import com.espaciounido.hsensor.connect.ConfigActivity
import com.espaciounido.hsensor.connect.SuccessActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val urlBroker = TinyDB.getInstance(this).getString("url_broker")

        if("" === urlBroker){
            startConfigActivity()
            return
        }

        val myWebView = this.findViewById(R.id.webView) as WebView
        myWebView.loadUrl(urlBroker)
        val webSettings = myWebView.settings
        webSettings.javaScriptEnabled = true
        myWebView.addJavascriptInterface(WebAppInterface(this), "Broker Loading...")

    }

    private fun startConfigActivity() {
        val intent = Intent(this, ConfigActivity::class.java)
        startActivity(intent)
        finish()
    }
}
