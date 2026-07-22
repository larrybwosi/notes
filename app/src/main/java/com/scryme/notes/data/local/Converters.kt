package com.scryme.notes.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scryme.notes.domain.model.Block

/**
 * Room TypeConverters using Gson to store lists of rich text Blocks as JSON strings.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromBlockList(blocks: List<Block>?): String? {
        if (blocks == null) return null
        return gson.toJson(blocks)
    }

    @TypeConverter
    fun toBlockList(blocksJson: String?): List<Block>? {
        if (blocksJson == null) return null
        val type = object : TypeToken<List<Block>>() {}.type
        return gson.fromJson(blocksJson, type)
    }
}
