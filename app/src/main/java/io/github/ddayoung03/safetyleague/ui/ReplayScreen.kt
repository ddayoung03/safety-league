package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.RouteNormalizer
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.databinding.ViewReplayBinding
import io.github.ddayoung03.safetyleague.ui.view.AsphaltDrawable
import io.github.ddayoung03.safetyleague.ui.view.replayEventIcon

internal fun MainActivity.showReplay() {
    val b = ViewReplayBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.root.background = AsphaltDrawable(this)
    val t = realTrip
    val win = t == null && demoResultWin == true // 시연 92점 카드에서 왔으면 그날(11-09) 리플레이
    val events: List<ReplayEvent> = t?.events ?: if (win) DemoData.winDayEvents else DemoData.lowDayEvents
    b.tvDemoNote.visibility = if (t == null && !win) View.VISIBLE else View.GONE
    b.tvReplayMeta.text = if (t != null) "${t.startTime} ~ ${t.endTime} · ${t.trip.km}km"
    else if (win) "${DemoData.winDayStart} ~ ${DemoData.winDayEnd} · ${DemoData.winDayKm}km"
    else "${DemoData.lowDayStart} ~ ${DemoData.lowDayEnd} · ${DemoData.lowDayKm}km"
    b.routeReplayView.isBackgroundMode = false
    val moved = t == null || t.route.distinct().size >= 2
    b.routeReplayView.routePoints = if (t == null) DemoData.route else RouteNormalizer.normalize(t.route)
    b.routeReplayView.visibility = if (moved) View.VISIBLE else View.INVISIBLE
    if (!moved) b.tvReplayMeta.text = "${t!!.startTime} ~ ${t.endTime} · ${t.trip.km}km · 위치 변화가 없어 경로를 그리지 못했어요"
    b.routeReplayView.replayEvents = events

    val names = mapOf("accel" to "급가속", "brake" to "급제동", "lane" to "깜빡이 없이 차선 변경")
    b.layoutEventList.removeAllViews()
    for (e in events.sortedBy { it.minutes }) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.cell))
            setPadding(dp(20), dp(10), dp(20), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { setMargins(0, dp(4), 0, dp(4)) }
        }
        item.addView(text(e.at, R.color.grey, 26f, number = true).apply { minWidth = dp(90) })
        item.addView(ImageView(this).apply {
            setImageResource(replayEventIcon(e.kind))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                marginEnd = dp(12)
            }
        })
        item.addView(text(names[e.kind] ?: e.kind, if (e.kind == "lane") R.color.yellow else R.color.white))
        b.layoutEventList.addView(item)
    }
    if (events.isEmpty()) b.layoutEventList.addView(note("감점 사건 없이 달렸어요"))
    b.tvReplayTip.text = pitBoard(events, if (t == null && !win) DemoData.lowDayDetail else t?.trip?.detailScore, demo = t == null).tip
    b.btnBack.setOnClickListener {
        if (realTrip != null) showResult(realTrip!!) else show(if (demoResultWin == true) Screen.WIN else Screen.LOW)
    }
}
