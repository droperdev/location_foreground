package com.example.android.whileinuselocation.data.local.beans

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class LocationBean(
    @PrimaryKey
    open var id: Int = 0,
    open var latitude: Double = 0.0,
    open var longitude: Double = 0.0,
    open var createdAt: Date = Date()
): RealmObject()