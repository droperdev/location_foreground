package com.example.android.whileinuselocation.data.local.dao

import com.example.android.whileinuselocation.data.local.beans.LocationBean

interface ILocationDAO {
    fun insert(location: LocationBean)
    fun deleteAll()
}