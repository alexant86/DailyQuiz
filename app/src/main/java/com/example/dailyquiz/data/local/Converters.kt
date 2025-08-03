package com.example.dailyquiz.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.room.TypeConverter

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        } ?: emptyList()
    }
}