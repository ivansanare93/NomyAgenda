package com.nomyagenda.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nomyagenda.app.R
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.preferences.SettingsRepository

object NotificationHelper {

    const val CHANNEL_ID = "nomy_reminders"
    const val CHANNEL_NAME = "Recordatorios"
    private const val ADVANCE_ID_OFFSET = 1_000_000

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
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(SettingsRepository.KEY_NOTIFICATIONS, true)) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // Schedule the main alarm at the exact due time
        scheduleAlarm(context, alarmManager, entry.id, entry.id, dueAt, entry.title, entry.content, isAdvance = false)

        // Schedule the advance alarm if configured and still in the future
        val advanceMinutes = entry.advanceNoticeMinutes
        if (advanceMinutes > 0) {
            val advanceAt = dueAt - advanceMinutes * 60_000L
            if (advanceAt > System.currentTimeMillis()) {
                val advanceContent = advanceContentText(context, advanceMinutes)
                scheduleAlarm(context, alarmManager, entry.id, entry.id + ADVANCE_ID_OFFSET, advanceAt, entry.title, advanceContent, isAdvance = true)
            }
        }
    }

    fun cancelReminder(context: Context, entryId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        cancelAlarm(context, alarmManager, entryId)
        cancelAlarm(context, alarmManager, entryId + ADVANCE_ID_OFFSET)
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        entryId: Int,
        requestCode: Int,
        triggerAt: Long,
        title: String,
        content: String,
        isAdvance: Boolean
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_CONTENT, content)
            putExtra(ReminderReceiver.EXTRA_IS_ADVANCE, isAdvance)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            else ->
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun advanceContentText(context: Context, advanceMinutes: Int): String = when (advanceMinutes) {
        SettingsRepository.ADVANCE_NOTICE_1H -> context.getString(R.string.notification_advance_1h)
        SettingsRepository.ADVANCE_NOTICE_1D -> context.getString(R.string.notification_advance_1d)
        SettingsRepository.ADVANCE_NOTICE_1W -> context.getString(R.string.notification_advance_1w)
        else -> context.getString(R.string.notification_advance_generic)
    }
}
