package com.example.convert

import java.util.Timer
import java.util.TimerTask
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window

class SplashScreenActivity : Activity() {
    @Override
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        // Hide title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(layout.splash_screen)
        val task: TimerTask = object : TimerTask() {
            @Override
            override fun run() {

                // Start the next activity
                val mainIntent: Intent = Intent().setClass(
                    this@SplashScreenActivity, MainActivity::class.java
                )
                startActivity(mainIntent)

                // Close the activity so the user won't able to go back this
                // activity pressing Back button
                finish()
            }
        }

        // Simulate a long loading process on application startup.
        val timer = Timer()
        timer.schedule(task, SPLASH_SCREEN_DELAY)
    }

    companion object {
        // Set the duration of the splash screen
        private const val SPLASH_SCREEN_DELAY: Long = 3000
    }
}