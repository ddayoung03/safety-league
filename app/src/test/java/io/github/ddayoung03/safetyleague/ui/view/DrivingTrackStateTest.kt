package io.github.ddayoung03.safetyleague.ui.view

import org.junit.Assert.*
import org.junit.Test

class DrivingTrackStateTest {
    @Test fun longTripsKeepPositiveCellWidths() {
        for (km in listOf(185.0, 500.0, 1000.0)) {
            val state = DrivingTrackState.from(km, null)
            val gap = state.cellGap(1109f, 6f)
            assertTrue((1109f - gap * (state.cells - 1)) / state.cells > 0f)
        }
        assertEquals(6f, DrivingTrackState.from(11.2, 18.4).cellGap(1043f, 6f), 0f)
    }

    @Test fun carMovesOnlyAtWholeKilometres() {
        assertEquals(0, DrivingTrackState.from(0.99, 18.4).filled)
        assertEquals(1, DrivingTrackState.from(1.0, 18.4).filled)
        assertEquals(DrivingTrackState.from(11.0, 18.4), DrivingTrackState.from(11.99, 18.4))
        assertEquals(12, DrivingTrackState.from(12.0, 18.4).filled)
    }
    @Test fun unknownDestinationGrowsWithoutFinishFlag() {
        val state = DrivingTrackState.from(14.2, null)
        assertEquals(15, state.cells)
        assertEquals(14, state.filled)
        assertFalse(state.hasFinish)
    }
    @Test fun arrivalCannotPutCarBeyondTrack() {
        val state = DrivingTrackState.from(20.0, 18.4)
        assertEquals(19, state.cells)
        assertEquals(19, state.filled)
        assertTrue(state.hasFinish)
    }
}
