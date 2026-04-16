package com.nomyagenda.app

import android.app.Application
import com.nomyagenda.app.data.local.database.NomyAgendaDatabase

class NomyAgendaApp : Application() {

    val database: NomyAgendaDatabase by lazy {
        NomyAgendaDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
