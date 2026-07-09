package com.fairtrack.app.data

import com.fairtrack.app.data.dao.MealTemplateDao
import com.fairtrack.app.data.entity.MealTemplate
import com.fairtrack.app.data.entity.MealTemplateItem
import com.fairtrack.app.data.entity.MealTemplateWithItems
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf Mahlzeiten-Vorlagen samt Elementen (v0.6.0). */
@Singleton
class MealTemplateRepository @Inject constructor(private val dao: MealTemplateDao) {

    fun observeTemplates(): Flow<List<MealTemplateWithItems>> = dao.observeAllWithItems()

    suspend fun getTemplate(id: Long): MealTemplateWithItems? = dao.getWithItems(id)

    /** Speichert eine neue Vorlage mit Namen und Elementen. Gibt die ID zurück. */
    suspend fun saveTemplate(name: String, items: List<MealTemplateItem>): Long =
        dao.saveTemplate(MealTemplate(name = name), items)

    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)
}
