package io.github.ddayoung03.safetyleague.ui

import android.Manifest
import android.car.Car
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.core.collector.LocationPolicy
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.repository.LocationConsentStore
import io.github.ddayoung03.safetyleague.databinding.ViewPermissionIntroBinding
import io.github.ddayoung03.safetyleague.ui.view.Pose

/** 위치 권한은 위치 동의 화면에서 동의한 뒤에만 묻는다. */
fun missingPermissions(context: Context): List<String> = buildList {
    if (LocationPolicy.requestsLocationPermission(LocationConsentStore(context).load())) {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add("pleos.car.permission.FUSED_LOCATION")
    }
    add(Car.PERMISSION_SPEED)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }

/**
 * 시스템 권한 창에 앞서 보여 줄 설명. AAOS는 속도 권한을 '위치' 창으로, 이름 없는 Pleos 위치 권한을
 * '알 수 없는 작업' 창으로 띄우므로 미리 알려 둔다.
 */
internal fun permissionNotes(missing: List<String>): List<String> = buildList {
    if (Car.PERMISSION_SPEED in missing) add("속도 신호: 점수를 매기는 데 꼭 필요해요. 안드로이드에서는 '위치' 권한 창으로 보여요.")
    if (Manifest.permission.POST_NOTIFICATIONS in missing) add("알림: 주차하면 결과 카드를 알려 드려요.")
    if (Manifest.permission.ACCESS_FINE_LOCATION in missing || Manifest.permission.ACCESS_COARSE_LOCATION in missing)
        add("위치: 리플레이에 내 주행 경로를 그려요. 경로는 내 화면에만 보여요.")
    if ("pleos.car.permission.FUSED_LOCATION" in missing) add("차량 위치(Pleos): 더 정확한 경로용이에요. '알 수 없는 작업' 창으로 보여요.")
}

/**
 * 권한 창은 MainActivity와 다른 작업 창에서 띄운다.
 * MainActivity 위에 띄우면 Pleos 홈 화면이 그 작업 창을 주행 중에 계속 가린다.
 */
class PermissionActivity : AppCompatActivity() {

    private lateinit var b: ViewPermissionIntroBinding

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // 홈에서 다시 켜려 했는데 시스템이 더는 묻지 않으면(두 번 거부) 그 사실을 알린다.
        // Pleos에는 이 앱의 권한을 바꾸는 설정 화면이 보이지 않아 다시 설치하는 방법만 안내한다.
        if (intent.getBooleanExtra(EXTRA_FROM_HOME, false) &&
            checkSelfPermission(Car.PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED &&
            !shouldShowRequestPermissionRationale(Car.PERMISSION_SPEED)) {
            b.tvNotes.text = getString(R.string.permission_blocked)
            b.btnContinue.visibility = View.GONE
            b.btnLater.setText(R.string.permission_blocked_ok)
            return@registerForActivityResult
        }
        openMain()
    }

    private fun openMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_ASKED, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finishAndRemoveTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val missing = missingPermissions(this)
        b = ViewPermissionIntroBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.rabbit.pose = Pose.BOOK
        b.tvNotes.text = permissionNotes(missing).joinToString("\n\n")
        b.btnContinue.skewButton(R.color.yellow)
        b.btnLater.skewButton(R.color.ink_glow)
        b.btnContinue.setOnClickListener { request.launch(missing.toTypedArray()) }
        b.btnLater.setOnClickListener { openMain() }
    }

    companion object {
        const val EXTRA_ASKED = "extra_permissions_asked"
        private const val EXTRA_FROM_HOME = "extra_from_home"

        /** [from]을 닫고 별도 작업 창에서 권한을 묻는다. 끝나면 MainActivity를 새로 연다. */
        fun askInOwnTask(from: android.app.Activity, fromHome: Boolean = false) {
            from.startActivity(Intent(from, PermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_FROM_HOME, fromHome))
            from.finishAndRemoveTask()
        }
    }
}
