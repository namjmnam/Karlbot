package com.lemol.karlbot

import android.app.Notification
import android.app.RemoteInput
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
    private val fileName: String = "botData.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        savedData = findViewById(R.id.saved_data)
        checkButton = findViewById(R.id.check_button)

        // testing shared preference
        // saveData("test", "test result")
        // savedData.text = loadData("test")

        // declaring test map
        // var testMap : MutableMap<String, List<Number>>
        // val testList : List<Number> = listOf(100F, 0, 1)
        // testMap = mutableMapOf("test" to testList)

        // testing map conversion
        // val savedMap = mapToByteArray(testMap)
        // savedData.text = byteArrayToMap(savedMap).toString()

        // testing saving to internal storage
        // save(testMap)
        // load()

        // Code below sends test message to linked chat room
        // NotiListener.savedNoti?.let { it1 -> replyFromMain(it1, "test") }

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

    // saves bot data to shared preference
    private fun saveData(key: String, value: String) {
        val botData: SharedPreferences = getSharedPreferences("botData", MODE_PRIVATE)
        val editor: SharedPreferences.Editor = botData.edit()
        editor.putString(key, value)
        editor.apply()
    }

    // loads bot data to shared preference
    private fun loadData(key: String) : String? {
        val botData: SharedPreferences = getSharedPreferences("botData", MODE_PRIVATE)
        return botData.getString(key, null)
    }

    // saves the MutableMap to internal storage
    private fun save(input: MutableMap<*, *>) {
        var fos: FileOutputStream? = null
        try {
            fos = openFileOutput(fileName, MODE_PRIVATE)
            val data = mapToByteArray(input)
            fos.write(data)
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

    // loads the MutableMap from internal storage
    private fun load() {
        var fis: FileInputStream? = null
        try {
            fis = openFileInput(fileName)
            val data = fis.readBytes()
            Toast.makeText(this, "Loaded ${byteArrayToMap(data)}",
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

    private fun mapToByteArray(input: MutableMap<*, *>): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = ObjectOutputStream(byteOut)
        out.writeObject(input)
        return byteOut.toByteArray()
    }

    private fun byteArrayToMap(input: ByteArray): MutableMap<*, *> {
        val byteIn = ByteArrayInputStream(input)
        val out = ObjectInputStream(byteIn)
        return out.readObject() as MutableMap<*, *>
    }

    private fun replyFromMain(action: Notification.Action, msg: String) {
        val intent = Intent()
        val bundle = Bundle()
        for (input in action.remoteInputs) bundle.putCharSequence(input.resultKey, msg)
        RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
        action.actionIntent.send(this, 0, intent)
    }
}