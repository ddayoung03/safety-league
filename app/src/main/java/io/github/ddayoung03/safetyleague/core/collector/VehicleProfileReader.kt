package io.github.ddayoung03.safetyleague.core.collector

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import io.github.ddayoung03.safetyleague.core.model.VehicleCatalog
import io.github.ddayoung03.safetyleague.core.model.VehicleProfile

object VehicleProfileReader {
    fun read(context: Context): VehicleProfile? {
        var car: Car? = null
        return try {
            car = Car.createCar(context)
            val properties = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return null
            val make = readValue("INFO_MAKE") { properties.getProperty(String::class.java, VehiclePropertyIds.INFO_MAKE, 0)?.value }
            val model = readValue("INFO_MODEL") { properties.getProperty(String::class.java, VehiclePropertyIds.INFO_MODEL, 0)?.value }
            val fuels = readValue("INFO_FUEL_TYPE") { properties.getIntArrayProperty(VehiclePropertyIds.INFO_FUEL_TYPE, 0).toList() }
            VehicleCatalog.recognize(make, model, fuels)
        } catch (e: Exception) {
            Log.i("VehicleProfile", "Car connection unavailable: ${e.javaClass.simpleName}")
            null
        } finally {
            runCatching { car?.disconnect() }
        }
    }

    private fun <T> readValue(name: String, read: () -> T?): T? = try {
        read().also { Log.i("VehicleProfile", "$name=$it") }
    } catch (e: Exception) {
        Log.i("VehicleProfile", "$name unavailable: ${e.javaClass.simpleName}")
        null
    }
}
