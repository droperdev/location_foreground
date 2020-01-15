package com.example.android.whileinuselocation.presentation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.location.Location
import android.net.Uri
import android.os.IBinder
import android.os.Bundle
import android.provider.Settings
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.android.whileinuselocation.*
import com.example.android.whileinuselocation.utils.SharedPreferenceUtil
import com.example.android.whileinuselocation.data.local.dao.LocationDAO
import com.example.android.whileinuselocation.services.ForegroundOnlyLocationService
import com.example.android.whileinuselocation.utils.toText

import com.google.android.material.snackbar.Snackbar
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var foregroundOnlyLocationServiceBound = false
    private var foregroundAndBackgroundLocationEnabled = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences:SharedPreferences

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnect()")
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "onServiceDisconnected")
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //declara el broadcastReceiver para obtener actualización del servicio en la view
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        foreground_only_location_button.setOnClickListener {

            val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ONLY_ENABLED, false)
            Log.d(TAG, enabled.toString())

            if (enabled) {
                foregroundOnlyLocationService?.stopTrackingLocation()
            } else {
                if (foregroundPermissionApproved()) {
                    foregroundOnlyLocationService?.startTrackingLocation()
                        ?: Log.d(TAG, "Service Not Bound")
                } else {
                    requestForegroundPermissions()
                }
            }
        }
        btn_delete_all.setOnClickListener{
            val realm = Realm.getDefaultInstance()
            LocationDAO(realm).deleteAll()
        }

       /* foreground_and_background_location_button.setOnClickListener {
            when {
                foregroundAndBackgroundLocationEnabled -> stopForegroundAndBackgroundLocation()
                else -> {
                    if (foregroundAndBackgroundPermissionApproved()) {
                        startForegroundAndBackgroundLocation()
                    } else {
                        requestForegroundAndBackgroundPermissions()
                    }
                }
            }
        }*/
    }

    override fun onStart() {
        super.onStart()

        updateForegroundOnlyButtonsState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ONLY_ENABLED, false)
        )
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ONLY_ENABLED) {
            updateForegroundOnlyButtonsState(sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ONLY_ENABLED, false)
            )
        }
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun foregroundAndBackgroundPermissionApproved(): Boolean {
        val foregroundLocationApproved = foregroundPermissionApproved()

        // TODO: Step 3.3, Add check for background permission.
        val backgroundPermissionApproved = true

        return foregroundLocationApproved && backgroundPermissionApproved
    }

    private fun requestForegroundPermissions() {

        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun requestForegroundAndBackgroundPermissions() {
        val provideRationale = foregroundAndBackgroundPermissionApproved()

        val permissionRequests = arrayListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        // TODO: Step 3.4, Add another entry to permission request array.


        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission(s)
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        permissionRequests.toTypedArray(),
                        REQUEST_FOREGROUND_AND_BACKGROUND_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground and background permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                permissionRequests.toTypedArray(),
                REQUEST_FOREGROUND_AND_BACKGROUND_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    foregroundOnlyLocationService?.startTrackingLocation()

                else -> {
                    // Permission denied.
                    updateForegroundOnlyButtonsState(false)

                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }

            REQUEST_FOREGROUND_AND_BACKGROUND_REQUEST_CODE -> {

                var foregroundAndBackgroundLocationApproved =
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

                // TODO: Step 3.5, For Android 10, check if background permissions approved in request code.


                // If user interaction was interrupted, the permission request
                // is cancelled and you receive empty arrays.
                when {
                    grantResults.isEmpty() -> Log.d(TAG, "User interaction was cancelled.")
                    // TODO: Step 3.6, review method call for foreground and background location.
                    foregroundAndBackgroundLocationApproved ->
                        startForegroundAndBackgroundLocation()
                    else -> {
                        // Permission denied.
                        updateForegroundOnlyButtonsState(false)

                        Snackbar.make(
                            findViewById(R.id.activity_main),
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(R.string.settings) {
                                // Build intent that displays the App settings screen.
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts(
                                    "package",
                                    BuildConfig.APPLICATION_ID,
                                    null
                                )
                                intent.data = uri
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun updateForegroundOnlyButtonsState(trackingLocation: Boolean) {
        if (trackingLocation) {
            foreground_only_location_button.text = getString(R.string.disable_foreground_only_location)
        } else {
            foreground_only_location_button.text = getString(R.string.enable_foreground_only_location)
        }
    }

    private fun updateForegroundAndBackgroundButtonsState() {
       /* if (foregroundAndBackgroundLocationEnabled) {
            foreground_and_background_location_button.text =
                getString(R.string.disable_foreground_and_background_location)
        } else {
            foreground_and_background_location_button.text =
                getString(R.string.enable_foreground_and_background_location)
        }*/
    }

    private fun startForegroundAndBackgroundLocation() {
        Log.d(TAG, "startForegroundAndBackgroundLocation()")
        foregroundAndBackgroundLocationEnabled = true
        updateForegroundAndBackgroundButtonsState()
        logResultsToScreen("Foreground and background location enabled.")
        // TODO: Add your specific background tracking logic here (start tracking).
    }

    private fun stopForegroundAndBackgroundLocation() {
        Log.d(TAG, "stopForegroundAndBackgroundLocation()")
        foregroundAndBackgroundLocationEnabled = false
        updateForegroundAndBackgroundButtonsState()
        logResultsToScreen("Foreground and background location disabled.")
        // TODO: Add your specific background tracking logic here (stop tracking).
    }

    private fun logResultsToScreen(output:String) {
        output_text_view.text = output
    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )

            if (location != null) {
                logResultsToScreen("Posición Gps: ${location.toText()}")
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private const val REQUEST_FOREGROUND_AND_BACKGROUND_REQUEST_CODE = 56
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    }
}
