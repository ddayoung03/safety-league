package io.github.ddayoung03.safetyleague.ui

internal enum class Resume { STAY, DRIVING, RESULT, HOME }

/**
 * 앱이 앞으로 돌아왔을 때 어떤 화면이어야 하는지. 앱이 뒤에 있는 동안 트립이 시작되거나 끝날 수 있다.
 * 트립이 진행 중이면 신호 대기여도 점수·순위를 보여 주지 않는다.
 */
internal fun resumeAction(screen: Screen, restricted: Boolean, tracking: Boolean, unseenResult: Boolean) = when {
    restricted -> Resume.STAY
    tracking -> if (screen == Screen.DRIVING) Resume.STAY else Resume.DRIVING
    unseenResult -> Resume.RESULT
    screen == Screen.DRIVING -> Resume.HOME
    else -> Resume.STAY
}

/** 시스템 뒤로 가기가 갈 화면. null이면 기본 동작(앱을 내림)이다. 리플레이는 온 곳을 알아야 해서 MainActivity가 맡는다. */
internal fun backTarget(screen: Screen, hasProfile: Boolean): Screen? = when (screen) {
    Screen.REPORT, Screen.SHOP, Screen.GUIDE, Screen.LEAGUE -> Screen.HOME
    Screen.VEHICLE -> Screen.HOME.takeIf { hasProfile }
    else -> null
}
