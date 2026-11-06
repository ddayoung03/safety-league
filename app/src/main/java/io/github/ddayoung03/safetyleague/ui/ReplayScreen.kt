package io.github.ddayoung03.safetyleague.ui

import android.animation.ValueAnimator
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.RouteNormalizer
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.databinding.ViewReplayBinding
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens
import io.github.ddayoung03.safetyleague.ui.motion.ScreenTransition
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import io.github.ddayoung03.safetyleague.ui.motion.settleShown
import io.github.ddayoung03.safetyleague.ui.motion.skippableSequence
import io.github.ddayoung03.safetyleague.ui.motion.stagger
import io.github.ddayoung03.safetyleague.ui.view.replayEventIcon
import io.github.ddayoung03.safetyleague.ui.view.Pose

internal fun MainActivity.showReplay() {
    val b = ViewReplayBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.RADIO
    b.checkerStrip.background = checkerTile()
    val t = realTrip
    val win = t == null && demoResultWin == true // 시연 92점 카드에서 왔으면 그날(11-09) 리플레이
    val events: List<ReplayEvent> = t?.events ?: if (win) DemoData.winDayEvents else DemoData.lowDayEvents
    b.tvDemoNote.visibility = if (t == null && !win) View.VISIBLE else View.GONE
    b.tvReplayMeta.text = if (t != null) "${t.startTime} ~ ${t.endTime} · ${t.trip.km}km"
    else if (win) "${DemoData.winDayStart} ~ ${DemoData.winDayEnd} · ${DemoData.winDayKm}km"
    else "${DemoData.lowDayStart} ~ ${DemoData.lowDayEnd} · ${DemoData.lowDayKm}km"
    val routeNote = t?.let { routeNote(it.route) }
    val moved = routeNote == null
    b.routeReplayView.routePoints = if (t == null) DemoData.route else RouteNormalizer.normalize(t.route)
    b.routeReplayView.visibility = if (moved) View.VISIBLE else View.INVISIBLE
    if (!moved) b.tvReplayMeta.text = "${t!!.startTime} ~ ${t.endTime} · ${t.trip.km}km · $routeNote"
    // 트립 안에서의 위치 순서. 시각과 같은 순서이고, 자정을 넘긴 트립도 차가 지나는 순서대로 뜬다.
    val ordered = events.sortedBy { it.t }
    b.routeReplayView.replayEvents = ordered

    val names = mapOf("accel" to "급가속", "brake" to "급제동", "lane" to "깜빡이 없이 차선 변경")
    b.layoutEventList.removeAllViews()
    val items = ordered.map { e ->
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = skew(R.color.ink_glow)
            setPadding(dp(18), 0, dp(18), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
                .apply { bottomMargin = dp(6) }
            addView(text(e.at, R.color.ink_muted, 26f, number = true).apply { minWidth = dp(90) })
            addView(ImageView(this@showReplay).apply {
                setImageResource(replayEventIcon(e.kind))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                    marginEnd = dp(12)
                }
            })
            addView(text(names[e.kind] ?: e.kind, if (e.kind == "lane") R.color.yellow else R.color.ink_text, 22f))
        }.also(b.layoutEventList::addView)
    }
    if (events.isEmpty()) b.layoutEventList.addView(note("감점 사건 없이 달렸어요"))
    b.tvReplayTip.text = if (t != null && !t.isEligible) SHORT_TRIP_TIP
    else pitBoard(events, if (t == null && !win) DemoData.lowDayDetail else t?.trip?.detailScore, demo = t == null).tip
    b.btnBack.pressFeedback()
    b.btnBack.setOnClickListener {
        if (realTrip != null) showResult(realTrip!!) else show(if (demoResultWin == true) Screen.WIN else Screen.LOW)
    }
    if (!moved) ScreenTransition.stagger(b.root, items, motion, MotionTokens.BASE)
    else if (animationsEnabled()) playReplay(b, items)
}

/**
 * 경로를 그리며 차가 지나는 사건을 핀과 목록 줄로 함께 띄운다. 길이와 상관없이 같은 시간이 걸린다.
 * 뷰에는 이미 최종 값이 들어 있고, 화면을 누르거나 화면이 바뀌면 motion.finishAll()이 최종 상태를 맞춘다.
 */
private fun MainActivity.playReplay(b: ViewReplayBinding, items: List<View>) {
    val route = b.routeReplayView
    val drive = ValueAnimator.ofFloat(0f, 1f).setDuration(MotionTokens.REPLAY)
    val seq = skippableSequence(b.root) {
        drive.cancel()
        b.scrollEvents.setOnTouchListener(null)
        route.finishReplay()
        items.forEach { it.settleShown() }
    }
    // ScrollView는 터치를 삼켜 root의 클릭까지 가지 않는다.
    b.scrollEvents.setOnTouchListener { _, _ -> motion.finishAll(); false }

    route.progress = 0f
    val fromX = dp(44).toFloat()
    for (item in items) { item.alpha = 0f; item.translationX = fromX }

    var shown = 0
    drive.addUpdateListener {
        route.progress = it.animatedValue as Float
        val count = route.visibleCount
        for (i in shown until count) {
            route.popPin(i)
            items[i].animate().alpha(1f).translationX(0f).setStartDelay((i - shown) * MotionTokens.STAGGER)
                .setDuration(MotionTokens.BASE).setInterpolator(DecelerateInterpolator(2f)).start()
            b.scrollEvents.smoothScrollTo(0, items[i].bottom - b.scrollEvents.height)
        }
        shown = count
    }
    seq.at(MotionTokens.BASE) { drive.start() }
    seq.endAt(MotionTokens.BASE + MotionTokens.REPLAY + MotionTokens.BASE)
}

/** 경로를 그릴 수 없을 때 이유. 그릴 수 있으면 null. */
internal fun routeNote(route: List<Point2D>): String? =
    when {
        route.isEmpty() -> "경로를 기록하지 않았어요"
        route.distinct().size >= 2 -> null
        else -> "위치 변화가 없어 경로를 그리지 못했어요"
    }
