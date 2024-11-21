package com.udacity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.udacity.databinding.ActivityDetailBinding


class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // get intent extras
        val repositoryName = intent.getStringExtra("repository_name") ?: "Unknown"
        val downloadStatus = intent.getBooleanExtra("download_status", false)

        binding.contentDetail.detailTitle.text = repositoryName

        if (downloadStatus) {
            binding.contentDetail.detailStatus.text = getString( R.string.download_successful)
            binding.contentDetail.detailStatus.setTextColor(Color.GREEN)
        }else{
            binding.contentDetail.detailStatus.text = getString(R.string.download_failed)
            binding.contentDetail.detailStatus.setTextColor(Color.RED)
        }

        // start motionLayout transition
        binding.contentDetail.motionLayout.post {
            binding.contentDetail.motionLayout.transitionToEnd()
        }

        // navigate back to the main screen
        binding.contentDetail.backButton.setOnClickListener {
            finish()
        }
        // open downloads directory in file app
        binding.contentDetail.openDirButton.setOnClickListener {
            openFilesApp()
        }
    }

    private fun openFilesApp() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }
}
