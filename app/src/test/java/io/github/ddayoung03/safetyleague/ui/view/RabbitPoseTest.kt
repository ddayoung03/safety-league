package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.R
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertArrayEquals
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
        assertEquals(1, frameIndex(Pose.WIN, 140, 4))
        assertEquals(3, frameIndex(Pose.WIN, 759, 4))
        assertEquals(0, frameIndex(Pose.WIN, 760, 4))
    }

    @Test fun oneShotPoseStopsOnLastFrame() {
        assertEquals(3, frameIndex(Pose.WIN, 760, 4, loop = false))
        assertEquals(3, frameIndex(Pose.WIN, 100_000, 4, loop = false))
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

    /** 표가 frames.json과 같고, 앱에 복사한 그림이 원본과 같은 파일인지 본다. 똑같은 그림은 한 장만 복사했다. */
    @Test fun tableMatchesForgeFrames() {
        val json = File("../design/rabbit-forge/frames.json").readText()
        val action = Regex(""""([A-Za-z_]+)":\s*[{]\s*"frames":\s*[\[](.*?)],\s*"durations_ms":\s*[\[](.*?)],\s*"loop":\s*([a-z]+)""", RegexOption.DOT_MATCHES_ALL)
        val names = R.drawable::class.java.fields.associate { it.getInt(null) to it.name }
        val actions = action.findAll(json).toList()
        assertEquals(Pose.entries.size, actions.size)
        for (m in actions) {
            val (name, frames, durations, loop) = m.destructured
            val pose = Pose.valueOf(name)
            val files = Regex("rabbit_[a-z0-9_]+[.]png").findAll(frames).map { it.value }.toList()
            assertEquals(files.size, pose.frames.size)
            files.zip(pose.frames).forEach { (source, id) ->
                val copy = File("src/main/res/drawable-nodpi/${names.getValue(id)}.png")
                assertArrayEquals(source, File("../design/rabbit-forge/frames/$source").readBytes(), copy.readBytes())
            }
            assertEquals(Regex("[0-9]+").findAll(durations).map { it.value.toLong() }.toList(), pose.durationsMs)
            assertEquals(loop.toBoolean(), pose.loop)
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

    @Test fun winBouncesHighestInTheAirFrames() {
        val top = (0L until Pose.WIN.bobPeriodMs).maxBy { bobOffset(Pose.WIN, it) }
        assertTrue(frameIndex(Pose.WIN, top, 4) in 1..2)
        assertEquals(WIN_BOUNCE, bobOffset(Pose.WIN, 380))
    }

    @Test fun scaleIsLargestWholeNumberThatFits() {
        assertEquals(1, spriteScale(128, 128, 1.32f)) // 빈 자리 128dp, 시연 창
        assertEquals(2, spriteScale(176, 235, 1.32f)) // 이긴 날
        assertEquals(3, spriteScale(264, 380, 1.32f)) // 아쉬운 날
        assertEquals(2, spriteScale(264, 380, 1f))
        assertEquals(1, spriteScale(10, 10, 1f)) // 너무 작아도 0배는 없다
    }

    /** 어느 프레임도 뷰가 재는 칸(가운데 기준 폭 CONTENT_W, 발끝 위 144칸) 밖으로 나가지 않는다. */
    @Test fun everyFrameFitsTheMeasuredBox() {
        val names = R.drawable::class.java.fields.associate { it.getInt(null) to it.name }
        for (pose in Pose.entries) for (id in pose.frames) {
            val image = ImageIO.read(File("src/main/res/drawable-nodpi/${names.getValue(id)}.png"))
            for (x in 0 until image.width) for (y in 0 until image.height) if (image.getRGB(x, y) ushr 24 != 0) {
                assertTrue("${pose.name} x=$x", kotlin.math.abs(x + 0.5f - pose.centerX) <= CONTENT_W / 2f)
                assertTrue("${pose.name} y=$y", y in 24 until 169)
            }
        }
    }

    @Test fun drivingScreenHasNoRabbit() {
        assertFalse(File("src/main/res/layout/view_driving.xml").readText().contains("Rabbit"))
    }
}
