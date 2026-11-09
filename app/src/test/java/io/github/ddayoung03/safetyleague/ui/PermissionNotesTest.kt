package io.github.ddayoung03.safetyleague.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionNotesTest {
    private val speed = "android.car.permission.CAR_SPEED"
    private val fine = "android.permission.ACCESS_FINE_LOCATION"
    private val coarse = "android.permission.ACCESS_COARSE_LOCATION"
    private val fused = "pleos.car.permission.FUSED_LOCATION"
    private val notify = "android.permission.POST_NOTIFICATIONS"

    @Test fun firstLaunchExplainsSpeedShowsAsLocation() {
        val notes = permissionNotes(listOf(speed, notify))
        assertEquals(2, notes.size)
        assertTrue(notes[0].contains("속도") && notes[0].contains("'위치'"))
        assertTrue(notes[1].contains("알림"))
    }

    @Test fun routeConsentExplainsUnknownActionOnce() {
        val notes = permissionNotes(listOf(fine, coarse, fused))
        assertEquals(2, notes.size) // 정밀·대략 위치는 창 하나
        assertTrue(notes[0].contains("경로"))
        assertTrue(notes[1].contains("'알 수 없는 작업'"))
    }

    @Test fun nothingMissingNothingToSay() {
        assertEquals(emptyList<String>(), permissionNotes(emptyList()))
    }
}
