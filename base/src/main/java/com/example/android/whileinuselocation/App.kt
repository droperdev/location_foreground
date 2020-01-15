package com.example.android.whileinuselocation

import android.app.Application
import com.example.android.whileinuselocation.utils.RealmBrowser
import io.realm.Realm
import io.realm.RealmConfiguration

class App: Application() {
    private var realmBrowser: RealmBrowser? = null
    override fun onCreate() {
        super.onCreate()
        initRealm()
        realmBrowser = RealmBrowser(this)
        realmBrowser?.start()
        realmBrowser?.showServerAddress()
    }

    private fun initRealm() {
        Realm.init(this)
        val configuration = RealmConfiguration.Builder()
            .name("Location")
            .schemaVersion(1)
            .build()
        Realm.setDefaultConfiguration(configuration)
    }

    override fun onTerminate() {
        super.onTerminate()
        realmBrowser?.stop()

    }
}