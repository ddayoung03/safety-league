package io.github.ddayoung03.safetyleague.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.collector.LocationConsent
import io.github.ddayoung03.safetyleague.core.collector.LocationPolicy
import io.github.ddayoung03.safetyleague.databinding.ViewLocationConsentBinding

/**
 * 위치 동의 화면. 차종 선택·배치 뒤, 그리고 차종 화면의 「경로 기록 설정」에서 연다.
 * 주행 중에는 MainActivity.show()가 주행 중 화면 외에는 막으므로 여기서 따로 막지 않는다.
 * [isDriving]은 버튼을 누른 순간 주행 중으로 바뀐 경우를 막는다.
 */
fun inflateLocationConsent(
    inflater: LayoutInflater,
    container: ViewGroup,
    current: LocationConsent,
    isDriving: () -> Boolean,
    onDecided: (LocationConsent) -> Unit
) {
    val b = ViewLocationConsentBinding.inflate(inflater, container, true)
    b.btnConsentAllow.skewButton(R.color.yellow)
    b.btnConsentDeny.skewButton(R.color.ink_glow)
    if (current != LocationConsent.UNDECIDED) {
        b.consentCurrent.visibility = View.VISIBLE
        b.consentCurrent.setText(
            if (current == LocationConsent.GRANTED) R.string.consent_now_granted else R.string.consent_now_denied
        )
    }
    b.btnConsentAllow.setOnClickListener { if (!isDriving()) onDecided(LocationConsent.GRANTED) }
    b.btnConsentDeny.setOnClickListener { if (!isDriving()) onDecided(LocationConsent.DENIED) }
}

internal fun MainActivity.showLocationConsent() =
    inflateLocationConsent(layoutInflater, binding.screenContainer, LocationPolicy.effectiveConsent(consentStore.load(),
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED), { restricted }) { consent ->
        consentStore.save(consent)
        // 위치 권한도 기존 권한 창처럼 별도 작업 창에서 묻는다 (MainActivity 위에 띄우면 주행 중 홈 화면이 앱을 가림)
        if (consent == LocationConsent.GRANTED && missingPermissions(this).isNotEmpty()) {
            PermissionActivity.askInOwnTask(this); return@inflateLocationConsent
        }
        startTracking() // 서비스가 동의에 맞춰 위치 수집을 켜거나 끈다
        demoResultWin = null
        show(Screen.HOME)
    }
