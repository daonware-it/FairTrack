package com.fairtrack.app.data.activity

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry aller bekannten [ActivitySource]s. Die Quellen werden per Hilt
 * Multibinding (`@IntoSet`) eingesammelt – ein späterer Google-Fit-Agent
 * registriert seine Quelle einfach zusätzlich, ohne diese Klasse zu ändern.
 */
@Singleton
class ActivitySourceRegistry @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards ActivitySource>
) {
    val all: Set<ActivitySource> get() = sources

    fun byId(id: String): ActivitySource? = sources.firstOrNull { it.id == id }

    fun forType(type: ActivitySourceType): ActivitySource? = byId(type.sourceId)
}
