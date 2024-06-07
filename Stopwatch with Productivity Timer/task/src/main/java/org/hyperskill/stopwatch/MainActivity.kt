package org.hyperskill.stopwatch

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlin.random.Random

@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity() {

    // Text view that displays the current timer value
    lateinit var timerView: TextView
    lateinit var settingsButton: Button

    lateinit var progressBar: ProgressBar

    // Handler associated with the main thread to schedule timer updates
    lateinit var handler: Handler

    // Runnable object used for the timer countdown functionality
    lateinit var runnable: Runnable

    // Variable to store the elapsed time in milliseconds
    var milliseconds = 0

    // Variable to store the stop time in milliseconds
    var timerStopInMilliseconds: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerView = findViewById(R.id.textView)

        // Set click listener for the start button
        findViewById<Button>(R.id.startButton).setOnClickListener {
            if (milliseconds == 0) {
                settingsButton.isEnabled = false
                startCountdown()
            }
        }

        // Set click listener for the reset button
        findViewById<Button>(R.id.resetButton).setOnClickListener {
            resetTimer()
        }

        // Set click listener for the settings button
        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val contentView = LayoutInflater
                .from(this)
                .inflate(R.layout.alert_dialog_layout, null, false)

            AlertDialog.Builder(this)
                .setTitle("Set upper limit in seconds")
                .setView(contentView)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val editText = contentView.findViewById<EditText>(R.id.upperLimitEditText)
                    timerStopInMilliseconds = editText.text.toString().toIntOrNull()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        progressBar = findViewById(R.id.progressBar)

        // Create a Handler associated with the main thread
        handler = Handler(Looper.getMainLooper())
    }

    /**
     * Starts the countdown timer. This method is called when the start button is clicked.
     */

    /*How it works:

    When handler.postDelayed(this, 1000) is called inside the run method of the Runnable object,
    it essentially tells the Handler to:

    Schedule the execution of the current Runnable object's run() method.
    Delay this execution by 1000 milliseconds (1 second).
    The run method of the Runnable object is responsible for updating the timer logic:

    Incrementing milliseconds by 1000.
    Updating the text view with the formatted time string.
    Generating a random color for the progress bar.

    Since the run method itself calls handler.postDelayed(this, 1000) again at the end, it creates a loop.
    The Handler keeps adding the Runnable object to the message queue with a 1-second delay.
    This ensures that the run method is called repeatedly every second, effectively creating a
    countdown timer behavior.

    In summary, handler.postDelayed(this, 1000) is the heart of the timer loop.
    It schedules the execution of the timer logic with a 1-second delay, creating the illusion of a
    continuous countdown.
    */
    private fun startCountdown() {
        progressBar.visibility = View.VISIBLE

        // Create a Runnable object for the timer countdown
        runnable = object : Runnable {
            override fun run() {
                // Increment milliseconds by 1 second (1000 milliseconds)
                milliseconds = milliseconds + 1000

                // Update the text view with the formatted time string
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    displayTime()
                }

                // Post this runnable again with a delay of 1 second to create a timer loop
                handler.postDelayed(this, 1000)

                if (milliseconds == (timerStopInMilliseconds?.times(1000) ?: -1)) {
                    handler.removeCallbacks(runnable)
                    timerView.setTextColor(Color.RED)
                    progressBar.visibility = View.GONE
                    settingsButton.isEnabled = true
                    notifyUser()
                }
            }
        }

        // Schedule the runnable to start after a 1 second delay
        handler.postDelayed(runnable, 1000)
    }

    private fun notifyUser() {
        val name = "Time's up!"
        val descriptionText = "I believe I can fly"
        val channelID = "org.hyperskill"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //create the notification channel
            val channel =
                NotificationChannel(channelID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = descriptionText
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // Create a pending intent for the notification
        val pendingIntent: PendingIntent = getActivity(this, 0, intent, FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(name)
            .setContentText(descriptionText)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Build the notification first
        val notification = notificationBuilder.build()
        // Directly set the flags including FLAG_INSISTENT
        notification.flags = Notification.FLAG_INSISTENT or Notification.FLAG_ONLY_ALERT_ONCE

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(393939, notification)
    }

    /**
     * Resets the timer to 00:00 and stops the countdown loop.
     * This method is called when the reset button is clicked or when the activity is destroyed.
     */
    private fun resetTimer() {
        handler.removeCallbacks(runnable)
        milliseconds = 0
        timerStopInMilliseconds = 0
        timerView.text = "00:00"
        timerView.setTextColor(Color.GRAY)
        progressBar.visibility = View.GONE
        settingsButton.isEnabled = true
    }

    /**
     * Converts milliseconds to minutes and seconds, formats the time string, and updates the text view.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun displayTime() {
        var minutes = milliseconds / 1000 / 60
        val seconds = milliseconds / 1000 % 60

        var timeString: String

        // Format the minutes string with leading zero if necessary
        timeString = if (minutes < 10) "0$minutes" else minutes.toString()

        // Format the seconds string with leading zero if necessary
        timeString =
            if (seconds < 10) timeString + ":" + "0$seconds" else timeString + ":" + seconds.toString()

        timerView.text = timeString

        progressBar.indeterminateTintList = ColorStateList.valueOf(getRandomColor())
    }

    private fun getRandomColor(): Int {
        val random = Random
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }

    override fun onDestroy() {
        super.onDestroy()
        resetTimer()
    }
}