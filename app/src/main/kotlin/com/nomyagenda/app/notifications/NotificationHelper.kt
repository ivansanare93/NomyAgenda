package com.nomyagenda.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nomyagenda.app.data.local.entity.AgendaEntry

object NotificationHelper {

    const val CHANNEL_ID = "nomy_reminders"
    const val CHANNEL_NAME = "Recordatorios"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de recordatorios de NomyAgenda"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, entry: AgendaEntry) {
        val dueAt = entry.dueAt ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ENTRY_ID, entry.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, entry.title)
            putExtra(ReminderReceiver.EXTRA_CONTENT, entry.content)
        }
        val pi = PendingIntent.getBroadcast(
            context, entry.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, dueAt, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pi)
        }
    }

    fun cancelReminder(context: Context, entryId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, entryId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
