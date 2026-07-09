package com.fairtrack.app.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room-Relation: eine Mahlzeiten-Vorlage mit allen zugehörigen Elementen.
 */
data class MealTemplateWithItems(
    @Embedded val template: MealTemplate,
    @Relation(parentColumn = "id", entityColumn = "templateId") val items: List<MealTemplateItem>
)
