// design/rabbit-forge/sync_app.py가 만든 파일이다. 직접 고치지 않는다.
package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.R

internal class RabbitClip(val frames: List<Int>, val durationsMs: List<Long>, val loop: Boolean)

internal object RabbitFrames {
    val IDLE = RabbitClip(listOf(R.drawable.rabbit_idle_00, R.drawable.rabbit_idle_01, R.drawable.rabbit_idle_02, R.drawable.rabbit_idle_03), listOf(500, 500, 500, 140), true)
    val WIN = RabbitClip(listOf(R.drawable.rabbit_win_00, R.drawable.rabbit_win_01, R.drawable.rabbit_win_02, R.drawable.rabbit_win_03, R.drawable.rabbit_win_04, R.drawable.rabbit_win_05, R.drawable.rabbit_win_06, R.drawable.rabbit_win_07, R.drawable.rabbit_win_08, R.drawable.rabbit_win_09), listOf(110, 100, 60, 60, 70, 110, 60, 60, 90, 130), true)
    val OVERTAKE = RabbitClip(listOf(R.drawable.rabbit_overtake_00, R.drawable.rabbit_overtake_01, R.drawable.rabbit_overtake_02, R.drawable.rabbit_overtake_03, R.drawable.rabbit_overtake_04, R.drawable.rabbit_overtake_05, R.drawable.rabbit_overtake_06, R.drawable.rabbit_overtake_07), listOf(70, 70, 70, 70, 70, 70, 70, 70), true)
    val RADIO = RabbitClip(listOf(R.drawable.rabbit_radio_00, R.drawable.rabbit_radio_01), listOf(220, 220), true)
    val LOAD = RabbitClip(listOf(R.drawable.rabbit_load_00, R.drawable.rabbit_load_01, R.drawable.rabbit_load_02, R.drawable.rabbit_load_03, R.drawable.rabbit_load_04, R.drawable.rabbit_load_05, R.drawable.rabbit_load_06, R.drawable.rabbit_load_07), listOf(90, 90, 90, 90, 90, 90, 90, 90), true)
    val WELCOME = RabbitClip(listOf(R.drawable.rabbit_welcome_00, R.drawable.rabbit_welcome_01, R.drawable.rabbit_welcome_02, R.drawable.rabbit_welcome_03, R.drawable.rabbit_welcome_04, R.drawable.rabbit_welcome_05), listOf(110, 110, 110, 110, 110, 110), true)
    val NOD = RabbitClip(listOf(R.drawable.rabbit_nod_00, R.drawable.rabbit_nod_01, R.drawable.rabbit_nod_02, R.drawable.rabbit_nod_03, R.drawable.rabbit_nod_04, R.drawable.rabbit_nod_05, R.drawable.rabbit_nod_06, R.drawable.rabbit_nod_07), listOf(310, 90, 90, 90, 90, 90, 90, 90), true)
    val POINT = RabbitClip(listOf(R.drawable.rabbit_point_00), listOf(800), true)
    val CHEER = RabbitClip(listOf(R.drawable.rabbit_cheer_00, R.drawable.rabbit_cheer_01, R.drawable.rabbit_cheer_02, R.drawable.rabbit_cheer_03), listOf(500, 500, 500, 160), true)
    val SIT = RabbitClip(listOf(R.drawable.rabbit_sit_00, R.drawable.rabbit_sit_01), listOf(900, 900), true)
    val COUPON = RabbitClip(listOf(R.drawable.rabbit_coupon_00, R.drawable.rabbit_coupon_01, R.drawable.rabbit_coupon_02, R.drawable.rabbit_coupon_03), listOf(500, 500, 500, 160), true)
    val BOOK = RabbitClip(listOf(R.drawable.rabbit_book_00), listOf(800), true)
    val CLIP = RabbitClip(listOf(R.drawable.rabbit_clip_00, R.drawable.rabbit_clip_01), listOf(600, 600), true)
}
