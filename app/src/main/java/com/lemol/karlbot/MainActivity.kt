package com.lemol.karlbot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat


class MainActivity : AppCompatActivity() {

    private lateinit var checkbtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkbtn = findViewById(R.id.check_button)

        checkbtn.setOnClickListener {
            if (nlAllowed()) Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show()
            else {
                // Try to request the permission manually by launching the menu
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
            }
        }
    }

    // Outputs whether the permission to access notification is granted or not
    private fun nlAllowed(): Boolean {
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        return sets != null && sets.contains(packageName)
    }




}