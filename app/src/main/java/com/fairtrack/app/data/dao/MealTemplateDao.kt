package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fairtrack.app.data.entity.MealTemplate
import com.fairtrack.app.data.entity.MealTemplateItem
import com.fairtrack.app.data.entity.MealTemplateWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {

    @Transaction
    @Query("SELECT * FROM meal_templates ORDER BY name ASC")
    fun observeAllWithItems(): Flow<List<MealTemplateWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_templates WHERE id = :id LIMIT 1")
    suspend fun getWithItems(id: Long): MealTemplateWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealTemplate): Long

    @Insert
    suspend fun insertItems(items: List<MealTemplateItem>)

    @Query("DELETE FROM meal_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    /** Speichert Vorlage + Elemente in einer Transaktion. Gibt die Vorlagen-ID zurück. */
    @Transaction
    suspend fun saveTemplate(template: MealTemplate, items: List<MealTemplateItem>): Long {
        val templateId = insertTemplate(template).let { if (template.id == 0L) it else template.id }
        insertItems(items.map { it.copy(id = 0, templateId = templateId) })
        return templateId
    }
}
