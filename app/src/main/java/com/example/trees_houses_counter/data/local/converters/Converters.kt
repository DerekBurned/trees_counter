package com.example.trees_houses_counter.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromCoordinatesList(value: String): List<Pair<Double, Double>> {
        val type = object : TypeToken<List<Pair<Double, Double>>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toCoordinatesList(list: List<Pair<Double, Double>>): String {
        return gson.toJson(list)
    }
}
