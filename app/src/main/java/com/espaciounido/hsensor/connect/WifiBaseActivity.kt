package com.espaciounido.hsensor.connect

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import com.espaciounido.hsensor.R

import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


abstract class WifiBaseActivity : AppCompatActivity() {

    private val worker = Executors.newSingleThreadScheduledExecutor()
    private var taskHandler: ScheduledFuture<*>? = null

    private var progressDialog: ProgressDialog? = null
    private var scanReceiver: ScanReceiver? = null
    private var connectionReceiver: ConnectionReceiver? = null

    protected abstract val secondsTimeout: Int
    protected abstract val wifiSSID: String
    protected abstract val wifiPass: String


    protected fun handleWIFI() {
        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifi.isWifiEnabled) {
            connectToSpecificNetwork()
        } else {
            showWifiDisabledDialog()
        }
    }


    private fun showWifiDisabledDialog() {
        AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.wifi_disabled))
                .setPositiveButton(getString(R.string.enable_wifi)) { dialog, which ->
                    // open settings screen
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivityForResult(intent, REQUEST_ENABLE_WIFI)
                }
                .setNegativeButton(getString(R.string.exit_app)) { dialog, which ->
                    handleWIFI()
                    dialog.dismiss()
                }
                .show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_ENABLE_WIFI && resultCode == 0) {
            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (wifi.isWifiEnabled || wifi.wifiState == WifiManager.WIFI_STATE_ENABLING) {
                connectToSpecificNetwork()
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    /**
     * Start to connect to a specific wifi network
     */
    private fun connectToSpecificNetwork() {
        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val wifiInfo = wifi.connectionInfo
        if (networkInfo !=null && networkInfo.isConnected && wifiInfo.ssid.replace("\"", "") == wifiSSID) {
            return
        } else {
            wifi.disconnect()
        }
        progressDialog = ProgressDialog.show(this, getString(R.string.connecting), String.format(getString(R.string.connecting_to_wifi), wifiSSID))
        taskHandler = worker.schedule(TimeoutTask(), secondsTimeout.toLong(), TimeUnit.SECONDS)
        scanReceiver = ScanReceiver()
        registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifi.startScan()
    }

    /**
     * Broadcast receiver for connection related events
     */
    private inner class ConnectionReceiver : BroadcastReceiver() {

        internal var wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager


        override fun onReceive(context: Context, intent: Intent) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = cm.activeNetworkInfo
            val wifiInfo = wifi.connectionInfo
            if (networkInfo!= null && networkInfo.isConnected) {
                if (wifiSSID == wifiInfo.ssid.replace("\"", "")) {
                    unregisterReceiver(this)
                    if (taskHandler != null) {
                        taskHandler!!.cancel(true)
                    }
                    if (progressDialog != null) {
                        progressDialog!!.dismiss()
                    }
                }
            }
        }
    }


    /**
     * Broadcast receiver for wifi scanning related events
     */
    private inner class ScanReceiver : BroadcastReceiver() {


        override fun onReceive(context: Context, intent: Intent) {
            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifiIpAddress(wifi)
            if (ip != null && ip == "192.168.4.2") {
                connectionReceiver = ConnectionReceiver()
                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
                intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
                registerReceiver(connectionReceiver, intentFilter)
                unregisterReceiver(this)
                return

            }
            // configure based on security
            val wifiConfig = WifiConfiguration()

            wifiConfig.SSID = wifiSSID
            wifiConfig.status = WifiConfiguration.Status.DISABLED
            wifiConfig.priority = 1000000

            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            wifiConfig.preSharedKey = "\"" + wifiPass + "\""

            val netId = wifi.addNetwork(wifiConfig)

            wifi.disconnect()
            wifi.enableNetwork(netId, true)
            wifi.reconnect()
        }

        protected fun wifiIpAddress(wifiManager: WifiManager): String? {
            var ipAddress = wifiManager.connectionInfo.ipAddress

            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                ipAddress = Integer.reverseBytes(ipAddress)
            }

            val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()

            var ipAddressString: String?
            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).hostAddress
            } catch (ex: UnknownHostException) {
                ipAddressString = null
            }

            return ipAddressString
        }

    }

    /**
     * Timeout task. Called when timeout is reached
     */
    private inner class TimeoutTask : Runnable {
        override fun run() {
            val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val wifiInfo = wifi.connectionInfo
            if (networkInfo.isConnected && wifiInfo.ssid.replace("\"", "") == wifiSSID) {
                try {
                    unregisterReceiver(connectionReceiver)
                } catch (ex: Exception) {
                    // ignore if receiver already unregistered
                }

                this@WifiBaseActivity.runOnUiThread {
                    if (progressDialog != null) {
                        progressDialog!!.dismiss()
                    }
                }
            } else {
                try {
                    unregisterReceiver(connectionReceiver)
                } catch (ex: Exception) {
                    // ignore if receiver already unregistered
                }

                this@WifiBaseActivity.runOnUiThread {
                    if (progressDialog != null) {
                        progressDialog!!.dismiss()
                    }
                    AlertDialog.Builder(this@WifiBaseActivity)
                            .setCancelable(false)
                            .setMessage(String.format(getString(R.string.wifi_not_connected), wifiSSID))
                            .setPositiveButton(getString(R.string.exit_app)) { dialog, which ->
                                handleWIFI()
                                dialog.dismiss()
                            }
                            .show()
                }
            }
        }
    }

    companion object {
        private val REQUEST_ENABLE_WIFI = 10
    }


}
