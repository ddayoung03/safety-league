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

    @Test fun cellsFillOnlyAtWholeKilometres() {
        assertEquals(0, DrivingTrackState.from(0.99, 18.4).filled)
        assertEquals(1, DrivingTrackState.from(1.0, 18.4).filled)
        assertEquals(11, DrivingTrackState.from(11.99, 18.4).filled)
        assertEquals(12, DrivingTrackState.from(12.0, 18.4).filled)
    }

    @Test fun carMovesInTenthsOfAKilometreInsideACell() {
        assertEquals(0.3f, DrivingTrackState.from(0.34, 18.4).carAt, 0.001f)
        assertEquals(11.9f, DrivingTrackState.from(11.99, 18.4).carAt, 0.001f)
        // 같은 0.1km 안에서는 상태가 같아서 다시 그리지 않는다
        assertEquals(DrivingTrackState.from(3.11, null), DrivingTrackState.from(3.19, null))
        assertNotEquals(DrivingTrackState.from(3.1, null), DrivingTrackState.from(3.2, null))
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
        assertEquals(19f, state.carAt, 0f)
        assertTrue(state.hasFinish)
    }
}
