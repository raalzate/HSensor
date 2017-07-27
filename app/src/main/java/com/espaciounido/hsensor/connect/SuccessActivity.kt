package com.espaciounido.hsensor.connect

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import com.espaciounido.hsensor.MainActivity
import com.espaciounido.hsensor.R


class SuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        Thread(Runnable {
            kotlin.run {
                SystemClock.sleep(5000)
                startActivity(Intent(this, MainActivity::class.java))
            }
        }).start()
    }
}
