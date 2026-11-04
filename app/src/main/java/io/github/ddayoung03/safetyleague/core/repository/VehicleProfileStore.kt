package io.github.ddayoung03.safetyleague.core.repository

import android.content.Context
import io.github.ddayoung03.safetyleague.core.model.Powertrain
import io.github.ddayoung03.safetyleague.core.model.VehicleProfile

class VehicleProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("vehicle_profile", Context.MODE_PRIVATE)
    fun load(): VehicleProfile? {
        val model = prefs.getString("model", null) ?: return null
        return VehicleProfile(model, Powertrain.valueOf(prefs.getString("powertrain", "EV")!!),
            prefs.getString("tier", "브론즈")!!, prefs.getString("recognition", "manual")!!)
    }
    fun save(profile: VehicleProfile) {
        prefs.edit().putString("model", profile.model).putString("powertrain", profile.powertrain.name)
            .putString("tier", profile.tier).putString("recognition", profile.recognition).apply()
    }
}
