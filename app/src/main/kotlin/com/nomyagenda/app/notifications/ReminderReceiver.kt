package com.nomyagenda.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nomyagenda.app.MainActivity
import com.nomyagenda.app.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ENTRY_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        val isAdvance = intent.getBooleanExtra(EXTRA_IS_ADVANCE, false)

        val notificationTitle = if (isAdvance) "⏰ $title" else title
        val notificationText = content.ifBlank { if (isAdvance) context.getString(R.string.notification_advance_generic) else "Recordatorio" }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPi = PendingIntent.getActivity(
            context, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(id, notification)
    }

    companion object {
        const val EXTRA_ENTRY_ID = "entry_id"
        const val EXTRA_TITLE = "entry_title"
        const val EXTRA_CONTENT = "entry_content"
        const val EXTRA_IS_ADVANCE = "is_advance"
    }
}
