package com.example.android.whileinuselocation.data.local.dao

import com.example.android.whileinuselocation.data.local.beans.LocationBean
import io.realm.Realm

class LocationDAO(private val realm: Realm):ILocationDAO {
    override fun insert(location: LocationBean) {
        realm.executeTransaction { realm ->
            var id: Long = 0
            val query = realm.where(LocationBean::class.java).max("id")
            if (query != null) id = query.toLong()

            location.id = id.toInt() + 1
            realm.insert(location)
            //realm.close()
        }
    }

    override fun deleteAll() {
        realm.executeTransaction { realm ->
            realm.delete(LocationBean::class.java)
        }
    }
}