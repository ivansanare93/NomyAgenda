package com.nomyagenda.app.notifications

import android.content.Context
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType

class ReminderService(context: Context) {

    private val appContext = context.applicationContext

    fun syncForSavedEntry(entry: AgendaEntry) {
        if (entry.type == EntryType.REMINDER && entry.dueAt != null) {
            NotificationHelper.scheduleReminder(appContext, entry)
        } else {
            NotificationHelper.cancelReminder(appContext, entry.id)
        }
    }

    fun cancelForDeletedEntry(entry: AgendaEntry) {
        if (entry.type == EntryType.REMINDER) {
            NotificationHelper.cancelReminder(appContext, entry.id)
        }
    }
}
