package com.nomyagenda.app

import android.app.Application
import com.nomyagenda.app.data.local.database.NomyAgendaDatabase
import com.nomyagenda.app.notifications.NotificationHelper

class NomyAgendaApp : Application() {

    val database: NomyAgendaDatabase by lazy {
        NomyAgendaDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
