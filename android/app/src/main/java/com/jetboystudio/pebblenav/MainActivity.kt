package com.jetboystudio.pebblenav

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.jetboystudio.pebblenav.pebble.NavController

/**
 * Minimal setup screen: grant Notification Access, send a test to the watch, and read the
 * short how-it-works help. Built with plain views to avoid heavy UI dependencies.
 */
class MainActivity : ComponentActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Pebble Maps Nav"
            textSize = 24f
        })

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, pad, 0, pad)
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Grant Notification Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "Test send to watch"
            setOnClickListener {
                NavController.testSend(applicationContext)
                toast("Sent a test turn to the watch")
            }
        })

        root.addView(TextView(this).apply {
            text = HELP
            textSize = 14f
            setPadding(0, pad, 0, 0)
            autoLinkMask = Linkify.WEB_URLS
            movementMethod = LinkMovementMethod.getInstance()
        })

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        maybeRequestPostNotifications()
    }

    override fun onResume() {
        super.onResume()
        status.text = if (hasNotificationAccess()) {
            "✓ Notification access granted.\nStart navigation in Google Maps and the " +
                "turn will pop up on your Pebble automatically."
        } else {
            "✗ Notification access NOT granted.\nTap “Grant Notification Access” " +
                "and enable Pebble Maps Nav."
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun maybeRequestPostNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val HELP =
            "How it works:\n" +
                "1. Install the watchapp on your Pebble (see the repo README).\n" +
                "2. Grant Notification Access above.\n" +
                "3. Start driving directions in Google Maps.\n\n" +
                "The watch shows the turn arrow, distance, street and a trip summary " +
                "(time left, distance left, ETA) on one screen.\n\n" +
                "Project: https://github.com/konsumer/pebble-map-android"
    }
}
