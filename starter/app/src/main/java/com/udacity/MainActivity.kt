package com.udacity

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.udacity.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStartTime: Long = 0
    private val TIMEOUT_THRESHOLD: Long = 10000
    private val CHANNEL_ID = "notification_downloads"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupNotifications()
        setupClickListener()
        setupInitialDrawable()
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 0)
        }
    }


    private fun setDrawable(drawableResId: Int): AnimatedVectorDrawable? {
        return try {
            val animatedDrawable = ContextCompat.getDrawable(this, drawableResId) as? AnimatedVectorDrawable
            binding.contentMain.animatedIcon.setImageDrawable(animatedDrawable)
            animatedDrawable
        } catch (e: Resources.NotFoundException) {
            Log.e("MainActivity", "Drawable not found: $drawableResId", e)
            null
        }
    }

    private fun setupInitialDrawable() {
        setDrawable(R.drawable.avd_download_anim)
    }

    private fun getDownloadAnimation(): AnimatedVectorDrawable? {
        return setDrawable(R.drawable.avd_download_anim)
    }

    private fun getCompletedAnimation(): AnimatedVectorDrawable? {
        return setDrawable(R.drawable.avd_download_completed_anim)
    }

    private fun getErrorAnimation(): AnimatedVectorDrawable? {
        return setDrawable(R.drawable.avd_download_fail_anim)
    }

    // Create a helper function to loop drawable
    private fun startLoopingAnimation(drawable: AnimatedVectorDrawable) {
        drawable.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                // Restart the animation
                (drawable as? AnimatedVectorDrawable)?.start()
            }
        })
        drawable.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNotifications() {

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

    }

    private fun setupClickListener() {
        binding.contentMain.loadingButton.setOnClickListener {
            val selectedId = binding.contentMain.radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val radioButton = findViewById<RadioButton>(selectedId)
                val url = radioButton.tag.toString()
                // Download chosen repository
                download(url)
            } else {
                // Send toast if no radiobutton is checked
                Toast.makeText(this, R.string.select_repository, Toast.LENGTH_SHORT).show()
                binding.contentMain.loadingButton.setLoadingState(ButtonState.Default)
            }
        }
    }

    fun sendNotification(title: String, message: String, downloadSuccessful: Boolean) {
        // Create intent for DetailActivity
        val detailIntent = Intent(this, DetailActivity::class.java).apply {
            putExtra("repository_name", getSelectedRepositoryName())
            putExtra("download_status", downloadSuccessful)
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
                getString(R.string.notification_button),
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
        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), 1)
            return
        }
        notificationManager.notify(1, builder.build())
    }

    private fun getSelectedRepositoryName(): String {
        val selectedId = binding.contentMain.radioGroup.checkedRadioButtonId
        val selectedRadioButton = findViewById<RadioButton>(selectedId)
        return selectedRadioButton?.text.toString()
    }

    private fun download(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isUrlValid(url)) {
                withContext(Dispatchers.Main) {
                    startDownload(url)
                }
            } else {
                withContext(Dispatchers.Main) {
                    showUrlError()
                }
            }
        }
    }

    private fun showUrlError() {
        Toast.makeText(this, getString(R.string.url_error), Toast.LENGTH_SHORT).show()
        getErrorAnimation()?.start()
        binding.contentMain.loadingButton.setLoadingState(ButtonState.Failed)
    }

    private fun startDownload(url: String) {
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

        getDownloadAnimation()?.let { startLoopingAnimation(it) }

        handler.post(checkDownloadRunnable)
    }

    private val checkDownloadRunnable = object : Runnable {
        override fun run() {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadID)
            val cursor: Cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val status =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_PENDING -> {
                        if (pendingStartTime == 0L) {
                            // Set start time when download enters pending state
                            pendingStartTime = System.currentTimeMillis()
                        }

                        val elapsedTime = System.currentTimeMillis() - pendingStartTime
                        if (elapsedTime > TIMEOUT_THRESHOLD) {
                            // Timeout exceeded, fail the download
                            binding.contentMain.loadingButton.setLoadingState(ButtonState.Failed)
                            handler.removeCallbacks(this) // Stop checking
                        } else {
                            // Continue checking if still within timeout threshold
                            binding.contentMain.loadingButton.setLoadingState(ButtonState.Pending)
                            handler.postDelayed(this, 500) // Continue checking
                        }
                    }

                    DownloadManager.STATUS_RUNNING -> {
                        binding.contentMain.loadingButton.setLoadingState(ButtonState.Loading)
                        handler.postDelayed(this, 500) // Continue checking
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        binding.contentMain.loadingButton.setLoadingState(ButtonState.Completed)
                        getCompletedAnimation()?.start()
                        handler.removeCallbacks(this) // Stop checking
                    }

                    DownloadManager.STATUS_FAILED -> {
                        binding.contentMain.loadingButton.setLoadingState(ButtonState.Failed)
                        getErrorAnimation()?.start()
                        handler.removeCallbacks(this) // Stop checking
                    }
                }
            }
            cursor.close()
        }
    }

    private fun isUrlValid(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000 // Timeout for connection
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299 // HTTP 2xx indicates success
        } catch (e: Exception) {
            Log.e("URLCheck", "Error validating URL", e)
            false
        }
    }
}
