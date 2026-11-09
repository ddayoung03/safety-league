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
        assertEquals(listOf(FUSED, NAVI, OS), LocationPolicy.sourceOrder(GRANTED, osPermission = true, tripActive = true))
    }

    @Test
    fun deniedOrUndecidedStartsNoSource() {
        assertEquals(emptyList<LocationProvider>(), LocationPolicy.sourceOrder(DENIED, osPermission = true, tripActive = true))
        assertEquals(emptyList<LocationProvider>(), LocationPolicy.sourceOrder(UNDECIDED, osPermission = true, tripActive = true))
    }

    @Test
    fun parkedStartsNoSourceEvenWithConsent() {
        // 주차 중에는 경로가 필요 없으니 위치를 받지 않는다 (배터리·개인정보)
        assertEquals(emptyList<LocationProvider>(), LocationPolicy.sourceOrder(GRANTED, osPermission = true, tripActive = false))
    }

    @Test
    fun osPermissionDeniedStartsNoSourceEvenWithConsent() {
        assertEquals(emptyList<LocationProvider>(), LocationPolicy.sourceOrder(GRANTED, osPermission = false, tripActive = true))
    }

    @Test
    fun consentWithoutOsPermissionShowsAsNotRecording() {
        assertEquals(DENIED, LocationPolicy.effectiveConsent(GRANTED, osPermission = false))
        assertEquals(GRANTED, LocationPolicy.effectiveConsent(GRANTED, osPermission = true))
        assertEquals(DENIED, LocationPolicy.effectiveConsent(DENIED, osPermission = true))
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
