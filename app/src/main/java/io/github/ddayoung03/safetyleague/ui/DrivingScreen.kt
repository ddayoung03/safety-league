package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewDrivingBinding
import io.github.ddayoung03.safetyleague.ui.view.DrivingTrackState
import io.github.ddayoung03.safetyleague.weather.*

internal fun MainActivity.showDriving() {
    binding.screenContainer.removeAllViews()
    val b = ViewDrivingBinding.inflate(layoutInflater, binding.screenContainer, true)
    drivingBinding = b
    updateDrivingProgress()
    updateWeather()
}

internal fun MainActivity.updateWeather() {
    val b = drivingBinding ?: return
    // System night appearance is a fallback when lighting data is absent; no invented sunset time.
    val night = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val weather = if (demoMode && !restricted) demoWeather else inferWeather(weatherSignals, night)
    val changed = displayedWeather != null && displayedWeather != weather
    val transition = changed && weatherGate.allow(android.os.SystemClock.uptimeMillis())
    displayedWeather = weather
    b.tvWeatherStatus.text = getString(R.string.weather_recording, weather.label)
    b.weatherAmbient.setState(weather, isAmbientMotionAllowed(weatherSignals.speedMps, weatherSignals.gear), transition)
}

internal fun MainActivity.updateDrivingProgress() {
    val b = drivingBinding ?: return
    val s = service
    val real = s?.isTracking == true && demoResultWin == null
    val doneKm = if (real) s!!.currentTripKm else 11.2
    val destKm: Double? = if (real) null else DemoData.winDayKm // 내비 목적지가 없으면 null
    drivingKmShown = doneKm.toInt()
    b.tvProgress.text = if (destKm != null) "${LeagueCalculator.round1(doneKm)} / ${destKm}km · 해운대까지" else "${LeagueCalculator.round1(doneKm)}km 기록됨"
    b.tvGoal.text = "안전하게, 다음 한 칸까지"

    b.layoutTrackBar.state = DrivingTrackState.from(doneKm, destKm)
}
