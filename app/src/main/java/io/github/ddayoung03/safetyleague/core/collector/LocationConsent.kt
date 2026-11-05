package io.github.ddayoung03.safetyleague.core.collector

/** 경로 기록 동의. 아직 묻지 않았으면 UNDECIDED. */
enum class LocationConsent { UNDECIDED, GRANTED, DENIED }

/** 위치 값을 가져올 출처. 앞에 있을수록 먼저 쓴다. */
enum class LocationProvider { FUSED, NAVI, OS }

/** 위치 동의에 따른 결정. 화면·서비스·권한 요청이 모두 이 순수 함수들을 따른다. */
object LocationPolicy {

    /** FusedLocation → NaviHelper → LocationManager. 실제 대체는 [LocationFallback]이 이 순서대로 한다. */
    val ORDER = listOf(LocationProvider.FUSED, LocationProvider.NAVI, LocationProvider.OS)

    /** 동의했을 때만 출처를 켠다. 거부·미결정이면 위치를 아예 받지 않는다(점수·리그는 위치 없이 동작). */
    fun sourceOrder(consent: LocationConsent): List<LocationProvider> =
        if (consent == LocationConsent.GRANTED) ORDER else emptyList()

    /** 위치 권한(OS·Pleos)은 동의한 뒤에만 요청한다. */
    fun requestsLocationPermission(consent: LocationConsent): Boolean = consent == LocationConsent.GRANTED

    /** 동의 화면은 아직 안 물었고, 주행 중이 아니고, 시연 모드가 아닐 때만 띄운다. */
    fun shouldAsk(consent: LocationConsent, driving: Boolean, demoMode: Boolean): Boolean =
        consent == LocationConsent.UNDECIDED && !driving && !demoMode
}
