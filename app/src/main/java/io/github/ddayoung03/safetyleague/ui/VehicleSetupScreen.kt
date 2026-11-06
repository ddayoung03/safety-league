package io.github.ddayoung03.safetyleague.ui

import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.core.view.children
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.collector.LocationConsent
import io.github.ddayoung03.safetyleague.core.collector.LocationPolicy
import io.github.ddayoung03.safetyleague.core.model.Powertrain
import io.github.ddayoung03.safetyleague.core.model.VehicleCatalog
import io.github.ddayoung03.safetyleague.core.model.VehicleProfile
import io.github.ddayoung03.safetyleague.databinding.ViewLeaguePlacementBinding
import io.github.ddayoung03.safetyleague.databinding.ViewVehicleSelectionBinding
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens
import io.github.ddayoung03.safetyleague.ui.motion.ScreenTransition
import io.github.ddayoung03.safetyleague.ui.motion.stagger
import io.github.ddayoung03.safetyleague.ui.view.AsphaltDrawable
import java.time.LocalDate

internal fun MainActivity.selectVehicle(profile: VehicleProfile) {
    if (restricted) return
    profileStore.save(profile.copy(tier = myTier))
    repository.assignUnassignedTrips(profile.leagueKey)
    hideKeyboard()
    demoResultWin = null
    show(Screen.PLACEMENT)
}

internal fun MainActivity.showVehicleSelection() {
    val b = ViewVehicleSelectionBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.root.background = AsphaltDrawable(this)
    fun addChoice(label: String, action: () -> Unit) {
        val button = Button(this).apply {
            text = label; textSize = 26f; isAllCaps = false
            setTextColor(color(R.color.white))
            backgroundTintList = android.content.res.ColorStateList.valueOf(color(R.color.cell))
            setOnClickListener { if (!restricted) action() }
        }
        b.vehicleGrid.addView(button, GridLayout.LayoutParams().apply {
            width = dp(368); height = dp(96); setMargins(0, 0, dp(16), dp(12))
        })
    }
    VehicleCatalog.vehicles.forEach { v ->
        addChoice("${v.model} · ${if (v.powertrain == Powertrain.EV) "EV" else "내연기관"}") {
            selectVehicle(VehicleProfile(v.model, v.powertrain))
        }
    }
    addChoice("기타 차종") {
        b.vehicleGrid.visibility = View.GONE
        b.customForm.visibility = View.VISIBLE
    }
    if (consentStore.load() != LocationConsent.UNDECIDED) addChoice(getString(R.string.consent_settings)) { show(Screen.CONSENT) }
    b.btnVehicleList.setOnClickListener {
        hideKeyboard(); b.customForm.visibility = View.GONE; b.vehicleGrid.visibility = View.VISIBLE
    }
    b.btnCustomConfirm.setOnClickListener {
        if (restricted) return@setOnClickListener
        val profile = VehicleCatalog.manual(b.vehicleName.text.toString(),
            if (b.powerEv.isChecked) Powertrain.EV else Powertrain.ICE)
        if (profile == null) b.nameError.text = "차종 이름을 1~20자로 입력해 주세요"
        else selectVehicle(profile)
    }
}

internal fun MainActivity.showPlacement() {
    val b = ViewLeaguePlacementBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.checkerStrip.background = checkerTile()
    b.btnStart.background = skew(R.color.yellow)
    b.btnChangeVehicle.background = skew(R.color.ink_glow)
    val profile = profileStore.load() ?: VehicleProfile("아이오닉 5", Powertrain.EV)
    val now = LocalDate.now()
    b.placementSeason.text = "${now.year}년 ${now.monthValue}월 시즌"
    b.placementModel.text = "${profile.model} 리그"
    b.placementTier.text = "${profile.tier} 단계에 배치되었습니다"
    b.placementRecognition.visibility = if (profile.recognition == "auto") View.VISIBLE else View.GONE
    b.btnChangeVehicle.setOnClickListener { show(Screen.VEHICLE) }
    b.btnStart.setOnClickListener {
        if (profileStore.load() == null) { show(Screen.VEHICLE); return@setOnClickListener }
        if (LocationPolicy.shouldAsk(consentStore.load(), restricted, demoMode)) { show(Screen.CONSENT); return@setOnClickListener }
        demoResultWin = null
        repository.latestCompletedTrip.value?.let { showResult(it) } ?: show(Screen.LEAGUE)
    }
    ScreenTransition.stagger(b.root, b.root.children.drop(1).toList(), motion, MotionTokens.BASE) // 맨 앞 체커 띠는 그대로 둔다
}
