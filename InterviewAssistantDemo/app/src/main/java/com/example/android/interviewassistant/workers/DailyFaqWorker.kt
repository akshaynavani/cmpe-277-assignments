package com.example.android.interviewassistant.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.android.interviewassistant.MainActivity
import com.example.android.interviewassistant.R
import com.example.android.interviewassistant.data.local.AppDatabase
import com.example.android.interviewassistant.data.remote.RetrofitClient
import com.example.android.interviewassistant.domain.AppRepository
import com.example.android.interviewassistant.domain.Result
import kotlinx.coroutines.flow.first

class DailyFaqWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = AppRepository(db, RetrofitClient.apiService)

        // Skip if no profile set up yet
        val profile = db.userProfileDao().getProfile().first() ?: return Result.success()

        // Skip if we already fetched FAQs today
        if (repository.alreadyFetchedToday()) return Result.success()

        // Use role as the study topic; targetCompany adds context on the backend
        val topic = profile.role
        val result = repository.fetchAndStoreDailyFaqs(
            topic = topic,
            role = profile.role,
            level = profile.level
        )

        return when (result) {
            is com.example.android.interviewassistant.domain.Result.Success -> {
                showNotification(result.data, topic)
                Result.success()
            }
            is com.example.android.interviewassistant.domain.Result.Error -> Result.retry()
        }
    }

    private fun showNotification(count: Int, topic: String) {
        ensureChannel()

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Interview FAQs Ready")
            .setContentText("$count new $topic interview questions from the internet")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your daily digest of $count $topic interview FAQs is ready. Tap to review them in the Study screen.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — notification silently dropped
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Interview FAQs",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily digest of interview FAQs fetched from the internet"
            }
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_faq_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "daily_faq_worker"
    }
}
