package com.fairtrack.app.data

import androidx.room.TypeConverter

/**
 * Room-TypeConverter für Enums, die Room nicht von Haus aus speichern kann.
 */
class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromMeasureUnit(value: MeasureUnit): String = value.name

    @TypeConverter
    fun toMeasureUnit(value: String): MeasureUnit = MeasureUnit.valueOf(value)
}
