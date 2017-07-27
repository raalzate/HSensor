package com.espaciounido.hsensor

/**
 * Created by MyMac on 26/07/17.
 */

import android.app.AlertDialog
import android.content.Context
import android.webkit.JavascriptInterface

class WebAppInterface (internal var context: Context) {


    @JavascriptInterface
    fun showDialog(message: String) {

        val builder = AlertDialog.Builder(this.context)
        builder.setMessage(message).setNeutralButton("OK") {
            dialog, id -> dialog.dismiss()
        }
        builder.create().show()
    }
}

