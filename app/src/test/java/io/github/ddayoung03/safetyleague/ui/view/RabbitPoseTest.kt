package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.R
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RabbitPoseTest {
    @Test fun frameLoopsWithinCount() {
        for (pose in Pose.entries) {
            val cycle = pose.durationsMs.sum()
            for (t in 0L..cycle * 3 step 10) assertTrue(frameIndex(pose, t, pose.frames.size) in pose.frames.indices)
            assertEquals(frameIndex(pose, 5, pose.frames.size), frameIndex(pose, cycle * 2 + 5, pose.frames.size))
        }
        val count = Pose.WIN.frames.size
        val cycle = Pose.WIN.durationsMs.sum()
        assertEquals(1, frameIndex(Pose.WIN, Pose.WIN.durationsMs.first(), count))
        assertEquals(count - 1, frameIndex(Pose.WIN, cycle - 1, count))
        assertEquals(0, frameIndex(Pose.WIN, cycle, count))
    }

    @Test fun oneShotPoseStopsOnLastFrame() {
        val count = Pose.WIN.frames.size
        val cycle = Pose.WIN.durationsMs.sum()
        assertEquals(count - 1, frameIndex(Pose.WIN, cycle, count, loop = false))
        assertEquals(count - 1, frameIndex(Pose.WIN, cycle * 100, count, loop = false))
    }

    @Test fun zeroTimeIsFirstFrame() {
        for (pose in Pose.entries) {
            assertEquals(0, frameIndex(pose, 0, pose.frames.size))
            assertEquals(0, bobOffset(pose, 0))
        }
    }

    @Test fun everyPoseHasFrames() {
        for (pose in Pose.entries) {
            assertTrue(pose.frames.isNotEmpty())
            assertEquals(pose.frames.size, pose.durationsMs.size)
        }
    }

    @Test fun bobMovesWholePixelsWithinItsHeight() {
        for (pose in Pose.entries) {
            val seen = (0L..5_000L step 10).map { bobOffset(pose, it) }.toSet()
            assertEquals((0..pose.bobHeight).toSet(), seen)
        }
    }

    @Test fun singleFramePosesStillMove() {
        for (pose in Pose.entries.filter { it.frames.size == 1 }) assertTrue(pose.bobHeight > 0)
    }

    @Test fun winBouncesOncePerCycleAtTheAirFrames() {
        val cycle = Pose.WIN.durationsMs.sum()
        assertEquals(cycle, Pose.WIN.bobPeriodMs)
        assertEquals(WIN_BOUNCE, bobOffset(Pose.WIN, cycle / 2))
        val count = Pose.WIN.frames.size
        assertTrue(frameIndex(Pose.WIN, cycle / 2, count) in 1 until count - 1)
    }

    @Test fun winAndOvertakeShareTheirCenter() {
        assertEquals(Pose.WIN.centerX, Pose.OVERTAKE.centerX)
    }

    @Test fun scaleIsLargestWholeNumberThatFits() {
        assertEquals(2, spriteScale(180, 245, 1.31f)) // 화면별 토끼 자리
        assertEquals(3, spriteScale(268, 368, 1.31f)) // 이긴 날
        assertEquals(4, spriteScale(356, 488, 1.31f)) // 아쉬운 날
        assertEquals(3, spriteScale(356, 488, 1f))
        assertEquals(1, spriteScale(10, 10, 1f)) // 너무 작아도 0배는 없다
    }

    /** 어느 프레임도 뷰가 재는 칸(가운데 기준 폭 CONTENT_W, TOP_ROW부터 발끝까지) 밖으로 나가지 않는다. */
    @Test fun everyFrameFitsTheMeasuredBox() {
        val names = R.drawable::class.java.fields.associate { it.getInt(null) to it.name }
        for (pose in Pose.entries) for (id in pose.frames) {
            val image = ImageIO.read(File("src/main/res/drawable-nodpi/${names.getValue(id)}.png"))
            for (x in 0 until image.width) for (y in 0 until image.height) if (image.getRGB(x, y) ushr 24 != 0) {
                assertTrue("${pose.name} x=$x", kotlin.math.abs(x + 0.5f - pose.centerX) <= CONTENT_W / 2f)
                assertTrue("${pose.name} y=$y", y in TOP_ROW..FEET_ROW)
            }
        }
    }

    @Test fun drivingScreenHasNoRabbit() {
        assertFalse(File("src/main/res/layout/view_driving.xml").readText().contains("Rabbit"))
    }
}
