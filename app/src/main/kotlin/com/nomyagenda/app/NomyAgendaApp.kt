package com.nomyagenda.app

import android.app.Application
import com.nomyagenda.app.data.local.database.NomyAgendaDatabase
import com.nomyagenda.app.data.preferences.SettingsRepository
import com.nomyagenda.app.data.remote.FirestoreDataSource
import com.nomyagenda.app.data.repository.AgendaRepository
import com.nomyagenda.app.data.repository.DiaryRepository
import com.nomyagenda.app.notifications.NotificationHelper
import com.nomyagenda.app.security.AppLockManager

class NomyAgendaApp : Application() {

    val database: NomyAgendaDatabase by lazy {
        NomyAgendaDatabase.getDatabase(this)
    }

    val firestoreDataSource: FirestoreDataSource by lazy {
        FirestoreDataSource()
    }

    val agendaRepository: AgendaRepository by lazy {
        AgendaRepository(database.agendaEntryDao(), firestoreDataSource, database.pendingDeleteDao())
    }

    val diaryRepository: DiaryRepository by lazy {
        DiaryRepository(database.diaryEntryDao())
    }

    val lockManager: AppLockManager by lazy {
        AppLockManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        val settings = SettingsRepository(this)
        settings.applyTheme()
        settings.applyLanguage()
    }
}
