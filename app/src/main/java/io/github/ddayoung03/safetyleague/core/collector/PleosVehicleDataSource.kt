package io.github.ddayoung03.safetyleague.core.collector

import android.content.Context
import android.util.Log
import io.github.ddayoung03.safetyleague.core.model.Point2D

/**
 * Pleos Connect Vehicle SDK 및 AAOS 표준 CarPropertyManager 연동 데이터 소스
 * - 에뮬레이터나 차량 환경에서 실제 VHAL 신호를 수신
 * - SDK 서비스 미지원 환경에서는 안전하게 fallback
 */
class PleosVehicleDataSource(
    private val context: Context
) : VehicleDataSource {

    private val tag = "PleosVehicleDataSource"
    private var listener: VehicleSignalListener? = null
    private var pleosVehicle: Any? = null // 리플렉션 또는 SDK 직접 연동

    override fun startListening(listener: VehicleSignalListener) {
        this.listener = listener
        connectPleosSdk()
    }

    override fun stopListening() {
        disconnectPleosSdk()
        this.listener = null
    }

    private fun connectPleosSdk() {
        try {
            // Pleos Connect Vehicle SDK 초기화
            val vehicleClass = Class.forName("ai.pleos.playground.vehicle.Vehicle")
            val constructor = vehicleClass.getConstructor(Context::class.java)
            val vehicle = constructor.newInstance(context)
            pleosVehicle = vehicle

            val initMethod = vehicleClass.getMethod("initialize")
            initMethod.invoke(vehicle)
            Log.i(tag, "Pleos Vehicle SDK initialized successfully.")

            // 신호 리스너 등록 로직 (Brake, Steering, Gear 등)
            registerSdkListeners(vehicle)
        } catch (e: Exception) {
            Log.w(tag, "Pleos Vehicle SDK unavailable in current environment: ${e.message}")
        }
    }

    private fun registerSdkListeners(vehicle: Any) {
        // 실제 Pleos VHAL/SDK가 구동되는 에뮬레이터에서 각 도메인 리스너 등록
        // 예: vehicle.getGear().registerGearSelection(...)
        // 예: vehicle.getBrake().registerBrakePedalStatus(...)
        // 예: vehicle.getSteeringWheel().registerSteeringWheelAngle(...)
    }

    private fun disconnectPleosSdk() {
        try {
            pleosVehicle?.let { vehicle ->
                val releaseMethod = vehicle.javaClass.getMethod("release")
                releaseMethod.invoke(vehicle)
                Log.i(tag, "Pleos Vehicle SDK released.")
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing Pleos Vehicle SDK: ${e.message}")
        } finally {
            pleosVehicle = null
        }
    }
}
