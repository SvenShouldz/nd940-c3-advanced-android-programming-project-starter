package com.udacity

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
        val downloadStatus = intent.getStringExtra("download_status") ?: "Unknown"

        binding.contentDetail.detailTitle.text = repositoryName
        binding.contentDetail.detailStatus.text = downloadStatus

        // navigate back to the main screen
        binding.contentDetail.backButton.setOnClickListener {
            finish()
        }
    }
}
