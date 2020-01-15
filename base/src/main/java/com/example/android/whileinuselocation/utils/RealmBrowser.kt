package com.example.android.whileinuselocation.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import io.realm.DynamicRealm
import io.realm.Realm
import io.realm.RealmObject
import java.util.*

class RealmBrowser(application: Context) {
    private val mApplication: Context = application

    private var server: RealmBrowserHTTPD? = null
    companion object{
        const val TAG = "RealmBrowser"
        const val DEFAULT_PORT = 8765
    }

    fun start() = start(DEFAULT_PORT)

    private fun start(port: Int){
        if (server == null){
            server = RealmBrowserHTTPD(port)
            server?.start()
        }
    }

    fun stop() {if(server != null) server?.stop()}

    fun showServerAddress(){
        val wifiManager:WifiManager = mApplication.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        val formatedIpAddress = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)
        Log.d("-->", "Please access! http://$formatedIpAddress:${server?.listeningPort}")
    }

    private inner class RealmBrowserHTTPD internal constructor(port: Int) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val method = session.method
            val uri = session.uri
            Log.d(TAG, "$method '$uri' ")
            val realm = Realm.getDefaultInstance()
            val dynamicRealm = DynamicRealm.getInstance(realm.configuration)
            val modelClasses = realm.configuration.realmObjectClasses.toMutableList()
            val params = session.parms
            val className = params["class_name"]
            val selectedView = params["selected_view"]

            val htmlBuilder = HtmlBuilder()
            htmlBuilder.start()

            htmlBuilder.showSidebar(modelClasses, dynamicRealm)

            htmlBuilder.startMainDisplay()

            if (className != null) {
                try {
                    val clazz = Class.forName(className)
                    if (RealmObject::class.java.isAssignableFrom(clazz)) {
                        htmlBuilder.setSelectedTableName(clazz)
                        htmlBuilder.startMainDisplayNavigation(selectedView)
                        when (selectedView) {
                            null -> {
                                val fieldName = params["field_name"]
                                val queryValue = params["query_value"]
                                val queryMap = HashMap<String, String>()
                                if (fieldName != null && queryValue != null) {
                                    queryMap[fieldName] = queryValue
                                }
                                htmlBuilder.showTableContent(dynamicRealm, queryMap)
                            }
                            HtmlBuilder.CONTENT_VIEW -> {
                                val fieldName = params["field_name"]
                                val queryValue = params["query_value"]
                                val queryMap = HashMap<String, String>()
                                if (fieldName != null && queryValue != null) {
                                    queryMap[fieldName] = queryValue
                                }
                                htmlBuilder.showTableContent(dynamicRealm, queryMap)
                            }
                            else -> htmlBuilder.showTableStructure(dynamicRealm)
                        }
                        htmlBuilder.closeMainDisplayNavigation()
                    }
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }

            } else {
                htmlBuilder.showEmptyView()
            }
            htmlBuilder.closeMainDisplay()

            realm.close()
            return newFixedLengthResponse(htmlBuilder.finish())
        }
    }
}