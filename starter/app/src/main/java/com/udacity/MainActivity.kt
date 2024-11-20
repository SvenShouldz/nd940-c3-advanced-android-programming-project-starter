package com.udacity

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.udacity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupNotifications()
        setupClickListener()
    }

    private fun setupNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Request permission for Notifications
            ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 1)
            // Notifications Channel
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_description)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            // Register receiver for download completed
            registerReceiver(
                receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_EXPORTED
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove Receiver
        unregisterReceiver(receiver)
    }

    private fun setupClickListener() {
        binding.contentMain.loadingButton.setOnClickListener {
            val selectedId = binding.contentMain.radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val radioButton = findViewById<RadioButton>(selectedId)
                val url = radioButton.tag.toString()
                // Download chosen repository
                download(url)
                binding.contentMain.loadingButton.setLoadingState(ButtonState.Loading)
            } else {
                // Send toast if no radiobutton is checked
                Toast.makeText(this, R.string.select_repository, Toast.LENGTH_SHORT).show()
                binding.contentMain.loadingButton.setLoadingState(ButtonState.Unclicked)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadID) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.contentMain.loadingButton.setLoadingState(ButtonState.Completed)
                    sendNotification(
                        getString(R.string.state_completed),
                        getString(R.string.download_finished)
                    )
                }, 2000)
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        // Create intent for DetailActivity
        val detailIntent = Intent(this, DetailActivity::class.java).apply {
            putExtra("repository_name", getSelectedRepositoryName())
            putExtra("download_status", "Success")
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        action =
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save,
                getString(R.string.download_finished),
                pendingIntent
            )
                .build()

        // Build the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(action)

        // Show the notification
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    private fun getSelectedRepositoryName(): String {
        val selectedId = binding.contentMain.radioGroup.checkedRadioButtonId
        val selectedRadioButton = findViewById<RadioButton>(selectedId)
        return selectedRadioButton?.text.toString()
    }

    private fun download(url: String) {
        // Download and save file to Downloads Directory
        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    getSelectedRepositoryName()
                )

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    companion object {
        private const val CHANNEL_ID = "notification_downloads"
    }
}
