package io.github.ddayoung03.safetyleague.ui

/** 홈 말풍선. 기록 불가 > 승급권 > 오늘 주행 > 시간대 순. 아쉬운 날도 혼내지 않는다. */
internal fun homeGreeting(hour: Int, inPromotionZone: Boolean, todayScore: Double?, canRecord: Boolean = true): String = when {
    !canRecord -> "속도 권한이 없어 주행을 기록할 수 없어요. 눌러서 켜 주세요"
    inPromotionZone -> "승급권이에요! 이대로만 가요"
    todayScore != null && todayScore >= 70.0 -> "오늘 주행 멋졌어요! 이 기세로 가요"
    todayScore != null -> "오늘도 수고했어요. 다음엔 더 부드럽게 가 봐요"
    hour in 5..10 -> "좋은 아침이에요! 오늘도 안전하게 가 봐요"
    hour in 11..17 -> "어서 와요! 오늘도 안전하게 가 봐요"
    else -> "오늘 하루도 수고했어요. 천천히 가요"
}
