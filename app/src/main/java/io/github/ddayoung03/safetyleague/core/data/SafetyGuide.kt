package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.weather.Weather

enum class GuideCategory(val title: String) {
    DISASTER("재난 대처"),
    SITUATION("상황별 주의"),
    BEGINNER("초보 꿀팁")
}

/** [score]는 이 습관이 닿는 점수 항목. 앱 점수와 이어 주는 한 줄이다. */
data class GuideCard(
    val id: String,
    val category: GuideCategory,
    val title: String,
    val steps: List<String>,
    val source: String,
    val score: String? = null
)

/**
 * 안전 가이드. 사람의 안전이 걸린 내용이라 법령과 공공기관 자료에서 확인한 것만 쓴다.
 * 근거 목록은 docs/safety-guide-sources.md. 문구를 고치면 그 자료와 다시 맞춘다.
 */
object SafetyGuide {
    val cards = listOf(
        GuideCard("quake", GuideCategory.DISASTER, "지진이 났을 때", listOf(
            "비상등을 켜고 속도를 서서히 줄여요",
            "도로 오른쪽에 차를 세워요",
            "라디오로 재난 정보를 들어요",
            "대피할 땐 키를 꽂아 두고 걸어서 이동해요"
        ), "행정안전부 지진 국민행동요령(서울시·한국수자원공사 안내)"),
        GuideCard("flood", GuideCategory.DISASTER, "폭우로 침수될 때", listOf(
            "물이 흘러드는 도로와 지하차도에는 들어가지 않아요",
            "차가 움직이면 타이어가 2/3 잠기기 전에 안전한 곳으로 옮겨요",
            "지하차도나 지하주차장에 물이 들면 차를 두고 바로 대피해요",
            "문이 안 열리면 목받침 철봉으로 창문 모서리를 깨요",
            "깰 수 없으면 안팎 수위 차가 30cm 이하일 때 문을 열어요"
        ), "행정안전부 침수 대비 국민행동요령(보도 자료)"),
        GuideCard("tunnel", GuideCategory.DISASTER, "터널에서 불이 났을 때", listOf(
            "지나갈 수 있으면 차를 몰고 터널 밖으로 나가요",
            "못 나가면 갓길에 세우고 엔진을 끈 뒤 키를 꽂아 둬요",
            "비상벨이나 긴급전화, 휴대전화로 알려요",
            "불이 작으면 터널 소화기와 소화전으로 꺼요",
            "불이 커지면 연기 반대쪽으로 유도등을 따라 대피해요"
        ), "국민행동요령 도로터널 화재(세종시·파주시 안내)"),
        GuideCard("breakdown", GuideCategory.DISASTER, "고속도로에서 멈췄을 때", listOf(
            "차를 갓길로 옮겨요",
            "비상등을 켜고 트렁크를 열어요",
            "가드레일 밖으로 먼저 대피해요",
            "대피한 뒤 112, 119, 도로공사 1588-2504로 신고해요"
        ), "한국도로공사 2차사고 예방 행동요령(비트밖스)"),

        GuideCard("rain", GuideCategory.SITUATION, "빗길", listOf(
            "노면이 젖으면 제한속도의 20%를 줄여요",
            "폭우로 100m 앞이 안 보이면 50%를 줄여요",
            "젖은 길의 제동거리는 마른 길의 약 1.8배예요"
        ), "도로교통법 시행규칙 제19조, 한국교통안전공단"),
        GuideCard("snow", GuideCategory.SITUATION, "눈길과 빙판길", listOf(
            "눈이 20mm 미만이면 제한속도의 20%를 줄여요",
            "20mm 이상 쌓이거나 얼었으면 50%를 줄여요",
            "시속 60km 빙판길 제동거리는 마른 길의 약 4.9배예요",
            "다리 위, 터널 출입구, 그늘진 길의 살얼음을 조심해요"
        ), "도로교통법 시행규칙 제19조, 한국교통안전공단"),
        GuideCard("fog", GuideCategory.SITUATION, "안개길", listOf(
            "100m 앞이 안 보이면 제한속도의 50%를 줄여요",
            "낮에도 전조등을 켜서 내 차를 알려요"
        ), "도로교통법 시행규칙 제19조, 인천광역시 안전 안내"),
        GuideCard("school", GuideCategory.SITUATION, "어린이보호구역", listOf(
            "제한속도는 대개 시속 30km 이내예요",
            "신호 없는 횡단보도 앞에서는 사람이 없어도 일시정지해요"
        ), "도로교통법 제12조, 제27조"),
        GuideCard("right", GuideCategory.SITUATION, "교차로 우회전", listOf(
            "우회전 신호등이 있으면 그 신호를 따라요",
            "앞 신호가 빨간불이면 먼저 멈춰요",
            "보행자가 없으면 천천히 돌아요",
            "돌고 난 횡단보도에 건너려는 사람이 있으면 멈춰요"
        ), "경찰청 우회전 통행 방법 안내"),
        GuideCard("drowsy", GuideCategory.SITUATION, "졸릴 때", listOf(
            "2시간마다 15분씩 쉬어요",
            "30~40분마다 창문을 열어 환기해요",
            "졸리면 졸음쉼터에 들러 쉬어 가요"
        ), "경찰청·국토교통부 졸음운전 예방 수칙"),

        GuideCard("signal", GuideCategory.BEGINNER, "깜빡이는 미리", listOf(
            "일반도로는 30m 전부터 켜요",
            "고속도로는 100m 전부터 켜요",
            "차선을 다 옮길 때까지 켜 둬요"
        ), "도로교통법 시행령 별표 2", "매너 점수"),
        GuideCard("lane", GuideCategory.BEGINNER, "차선 바꾸기 전에", listOf(
            "옮길 차선의 차를 먼저 확인해요",
            "그 차의 진행을 막을 것 같으면 기다려요"
        ), "도로교통법 제19조"),
        GuideCard("distance", GuideCategory.BEGINNER, "앞차와의 거리", listOf(
            "앞차가 갑자기 서도 피할 수 있게 떨어져요",
            "위험할 때가 아니면 급제동하지 않아요"
        ), "도로교통법 제19조", "부드러움 점수"),
        GuideCard("belt", GuideCategory.BEGINNER, "출발 전 안전벨트", listOf(
            "모든 도로에서 전 좌석이 매야 해요",
            "동승자도 맸는지 운전자가 확인해요"
        ), "도로교통법 제50조", "안전 기본기 점수"),
        GuideCard("crosswalk", GuideCategory.BEGINNER, "신호 없는 횡단보도", listOf(
            "건너려는 사람이 보이면 멈춰요",
            "어린이보호구역에서는 사람이 없어도 일시정지해요"
        ), "도로교통법 제27조")
    )

    /** 상황별 주의는 지금 날씨에 맞는 카드를 맨 앞에 둔다. */
    fun of(category: GuideCategory, weather: Weather? = null): List<GuideCard> {
        val list = cards.filter { it.category == category }
        val first = suggested(weather)
        return if (category == GuideCategory.SITUATION && first != null) listOf(first) + (list - first) else list
    }

    /** 차량 신호로 판정한 날씨에 맞는 카드. 와이퍼가 돌면 빗길을 먼저 보여 줄 때 쓴다. */
    fun suggested(weather: Weather?): GuideCard? = when (weather) {
        Weather.RAIN -> cards.first { it.id == "rain" }
        Weather.SNOW -> cards.first { it.id == "snow" }
        else -> null
    }
}
