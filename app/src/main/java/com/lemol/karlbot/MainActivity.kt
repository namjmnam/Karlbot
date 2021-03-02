package com.lemol.karlbot

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import java.io.*


class MainActivity : AppCompatActivity() {

    private lateinit var checkButton : Button
    private lateinit var savedData : TextView
    private val fileName: String = "test.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        savedData = findViewById(R.id.saved_data)
        checkButton = findViewById(R.id.check_button)

//        save("HAHAHA")
//        load()
//        saveData("test", "HEHE")
//        savedData.text = loadData("test")

        checkButton.setOnClickListener {
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
        return sets.contains(packageName)
    }

    private fun saveData(key: String, value: String) {
        val gameData: SharedPreferences = getSharedPreferences("gameData", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = gameData.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun loadData(key: String) : String? {
        val gameData: SharedPreferences = getSharedPreferences("gameData", MODE_PRIVATE)
        return gameData.getString(key, null)
    }

    fun save(input: String) {
        var fos: FileOutputStream? = null
        try {
            fos = openFileOutput(fileName, MODE_PRIVATE)
            fos.write(input.toByteArray())
            Toast.makeText(this, "Saved to $filesDir/$fileName",
                    Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun load() {
        var fis: FileInputStream? = null
        try {
            fis = openFileInput(fileName)
            val isr = InputStreamReader(fis)
            val br = BufferedReader(isr)
            val sb = StringBuilder()
            var text: String?
            while (br.readLine().also { text = it } != null) {
                sb.append(text).append("\n")
            }
            Toast.makeText(this, "Loaded ${sb.toString()}",
                    Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}