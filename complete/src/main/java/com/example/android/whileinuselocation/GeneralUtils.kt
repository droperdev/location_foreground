package com.example.android.whileinuselocation

import java.text.SimpleDateFormat
import java.util.*

class GeneralUtils {
    companion object{
        @JvmStatic
        fun getTodayWithHours(): String {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("GMT-5")
            return dateFormat.format(Calendar.getInstance().time)
        }
    }
}