package io.github.ddayoung03.safetyleague.ui

import android.Manifest
import android.car.Car
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.core.collector.LocationPolicy
import io.github.ddayoung03.safetyleague.core.repository.LocationConsentStore

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
 * 권한 창은 MainActivity와 다른 작업 창에서 띄운다.
 * MainActivity 위에 띄우면 Pleos 홈 화면이 그 작업 창을 주행 중에 계속 가린다.
 */
class PermissionActivity : AppCompatActivity() {

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_ASKED, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finishAndRemoveTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) request.launch(missingPermissions(this).toTypedArray())
    }

    companion object {
        const val EXTRA_ASKED = "extra_permissions_asked"

        /** [from]을 닫고 별도 작업 창에서 권한을 묻는다. 끝나면 MainActivity를 새로 연다. */
        fun askInOwnTask(from: android.app.Activity) {
            from.startActivity(Intent(from, PermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            from.finishAndRemoveTask()
        }
    }
}
