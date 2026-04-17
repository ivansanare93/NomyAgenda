package com.nomyagenda.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nomyagenda.app.data.local.database.NomyAgendaDatabase
import com.nomyagenda.app.data.repository.AgendaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val repository = AgendaRepository(
            NomyAgendaDatabase.getDatabase(context).agendaEntryDao()
        )
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                repository.getFutureReminders(now).forEach { entry ->
                    NotificationHelper.scheduleReminder(context, entry)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
