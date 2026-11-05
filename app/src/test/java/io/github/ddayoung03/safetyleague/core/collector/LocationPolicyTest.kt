package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.collector.LocationConsent.DENIED
import io.github.ddayoung03.safetyleague.core.collector.LocationConsent.GRANTED
import io.github.ddayoung03.safetyleague.core.collector.LocationConsent.UNDECIDED
import io.github.ddayoung03.safetyleague.core.collector.LocationProvider.FUSED
import io.github.ddayoung03.safetyleague.core.collector.LocationProvider.NAVI
import io.github.ddayoung03.safetyleague.core.collector.LocationProvider.OS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPolicyTest {

    @Test
    fun grantedUsesFusedThenNaviThenLocationManager() {
        assertEquals(listOf(FUSED, NAVI, OS), LocationPolicy.sourceOrder(GRANTED))
    }

    @Test
    fun deniedOrUndecidedStartsNoSource() {
        assertEquals(emptyList<LocationProvider>(), LocationPolicy.sourceOrder(DENIED))
        assertEquals(emptyList<LocationProvider>(), LocationPolicy.sourceOrder(UNDECIDED))
    }

    @Test
    fun locationPermissionIsRequestedOnlyAfterConsent() {
        assertTrue(LocationPolicy.requestsLocationPermission(GRANTED))
        assertFalse(LocationPolicy.requestsLocationPermission(DENIED))
        assertFalse(LocationPolicy.requestsLocationPermission(UNDECIDED))
    }

    @Test
    fun consentScreenIsNeverShownWhileDrivingOrInDemo() {
        assertTrue(LocationPolicy.shouldAsk(UNDECIDED, driving = false, demoMode = false))
        assertFalse(LocationPolicy.shouldAsk(UNDECIDED, driving = true, demoMode = false))
        assertFalse(LocationPolicy.shouldAsk(UNDECIDED, driving = false, demoMode = true))
    }

    @Test
    fun decidedConsentIsNotAskedAgain() {
        assertFalse(LocationPolicy.shouldAsk(GRANTED, driving = false, demoMode = false))
        assertFalse(LocationPolicy.shouldAsk(DENIED, driving = false, demoMode = false))
    }
}
