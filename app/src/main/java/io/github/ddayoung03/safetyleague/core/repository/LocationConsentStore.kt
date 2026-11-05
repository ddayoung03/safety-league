package io.github.ddayoung03.safetyleague.core.repository

import android.content.Context
import io.github.ddayoung03.safetyleague.core.collector.LocationConsent

/** 경로 기록 동의 여부. 앱을 다시 켜도 유지된다. */
class LocationConsentStore(context: Context) {
    private val prefs = context.getSharedPreferences("location_consent", Context.MODE_PRIVATE)
    fun load(): LocationConsent =
        prefs.getString("consent", null)?.let { runCatching { LocationConsent.valueOf(it) }.getOrNull() }
            ?: LocationConsent.UNDECIDED
    fun save(consent: LocationConsent) { prefs.edit().putString("consent", consent.name).apply() }
}
