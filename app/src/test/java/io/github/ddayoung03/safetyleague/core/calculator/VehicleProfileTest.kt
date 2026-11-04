package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Powertrain
import io.github.ddayoung03.safetyleague.core.model.VehicleCatalog
import io.github.ddayoung03.safetyleague.core.model.VehicleProfile
import org.junit.Assert.*
import org.junit.Test

class VehicleProfileTest {
    @Test fun recognizesOnlySupportedMakeAndModel() {
        assertEquals("아이오닉 5", VehicleCatalog.recognize("Hyundai", "IONIQ 5", null)?.model)
        assertEquals("아이오닉 5", VehicleCatalog.recognize("현대", "아이오닉 5", null)?.model)
        assertNull(VehicleCatalog.recognize("Kia", "IONIQ 5", null))
    }
    @Test fun ignoresCaseAndWhitespace() {
        assertNotNull(VehicleCatalog.recognize(" hYUnDai ", "  ioniq   5 ", null))
    }
    @Test fun missingDefaultOrUnknownValuesAreNotRecognized() {
        for ((make, model) in listOf(null to null, "" to "", "Hyundai" to "", "" to "IONIQ 5",
            "Default" to "Model", "Hyundai" to "Connect on x86_64 emulator", "Hyundai" to "Unknown")) {
            assertNull(VehicleCatalog.recognize(make, model, null))
        }
    }
    @Test fun fuelOverridesCatalogAndMissingFuelFallsBack() {
        assertEquals(Powertrain.EV, VehicleCatalog.recognize("Hyundai", "IONIQ 5", null)?.powertrain)
        assertEquals(Powertrain.EV, VehicleCatalog.recognize("Hyundai", "IONIQ 5", emptyList())?.powertrain)
        assertEquals(Powertrain.ICE, VehicleCatalog.recognize("Hyundai", "IONIQ 5", listOf(1))?.powertrain)
        assertEquals(Powertrain.EV, VehicleCatalog.recognize("Kia", "Sportage", listOf(1, 10))?.powertrain)
    }
    @Test fun leagueKeySeparatesPowertrainsAndEfficiency() {
        val ev = VehicleProfile("테스트카", Powertrain.EV)
        val ice = ev.copy(powertrain = Powertrain.ICE)
        assertNotEquals(ev.leagueKey, ice.leagueKey)
        assertEquals(5.8, ev.averageEfficiency, 0.001)
        assertEquals(12.0, ice.averageEfficiency, 0.001)
    }
    @Test fun customNameIsTrimmedBoundedAndRequired() {
        assertEquals("테스트카", VehicleCatalog.manual(" 테스트카 ", Powertrain.EV)?.model)
        assertNull(VehicleCatalog.manual("  ", Powertrain.ICE))
        assertNull(VehicleCatalog.manual("a".repeat(21), Powertrain.EV))
        assertEquals(20, VehicleCatalog.manual("a".repeat(20), Powertrain.EV)?.model?.length)
    }
}
