package com.example.smartnotificationmanager

// Importing necessary libraries for various functionalities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest

// Main activity class that manages the app's lifecycle and handles the core logic of checking notification service and camera permissions.
class MainActivity : AppCompatActivity() {

    // Constant used to identify the request code for camera permission
    private val CAMERA_PERMISSION_CODE = 100

    // onCreate method is the entry point of the activity. This method initializes the app's layout and checks if notification listener service is enabled.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if the notification listener service is enabled.
        // If it's not enabled, redirect the user to the settings screen to enable it.
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    // This function checks if the notification listener service is enabled for the app.
    // The notification listener service allows the app to read notifications on the device.
    // It checks the list of enabled notification listeners and returns true if the service for this app is enabled, otherwise false.


    private fun isNotificationServiceEnabled(): Boolean {
        // Get the package name of this app.
        val pkgName = packageName
        // Retrieve the list of enabled notification listeners (as a flattened string).
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")

        // Check if the list is not empty.
        if (!TextUtils.isEmpty(flat)) {
            // Split the flattened string by ":" to get the list of component names.
            val names = flat.split(":").toTypedArray()
            // Loop through the component names.
            for (name in names) {
                // Unflatten the string to get the component name.
                val cn = ComponentName.unflattenFromString(name)
                // If the component name is not null and matches this app's package name, the service is enabled.
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true // Return true if the service is enabled for this app.
                    }
                }
            }
        }
        return false // Return false if the service is not enabled.
    }
}

