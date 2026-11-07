package io.github.ddayoung03.safetyleague.core.repository

import android.content.Context
import io.github.ddayoung03.safetyleague.core.calculator.SettlementCard
import io.github.ddayoung03.safetyleague.core.calculator.TierMovement
import io.github.ddayoung03.safetyleague.core.calculator.Wallet
import java.time.YearMonth

/**
 * 시즌 종료 정산. 티어(VehicleProfileStore의 "tier")와 같은 파일에 정산한 시즌과 포인트를 한 번의 commit으로 함께 써서
 * 정산 도중 앱이 꺼지거나 재시작해도 두 번 정산되거나 티어만 바뀌는 일이 없다.
 */
class SeasonSettlementStore(context: Context) {
    private val profileStore = VehicleProfileStore(context)
    private val prefs = context.getSharedPreferences("vehicle_profile", Context.MODE_PRIVATE)

    /** 달이 바뀌었으면 지난 시즌을 정산하고 true. 처음 실행이면 지금 시즌만 기록한다. */
    fun settleIfNeeded(repository: TripRepository, now: YearMonth = YearMonth.now()): Boolean {
        val last = prefs.getString(KEY_SEASON, null)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        if (last == null) { prefs.edit().putString(KEY_SEASON, now.toString()).commit(); return false }
        if (!last.isBefore(now)) return false
        val profile = profileStore.load()
        if (profile == null) { prefs.edit().putString(KEY_SEASON, now.toString()).commit(); return false }
        val trips = repository.getAllTrips(profile.leagueKey)
        val results = TierMovement.settleSince(trips, last, now, profile.tier)
        val result = TierMovement.summarize(results) ?: return false
        val wallet = Wallet.decode(prefs.getString(PointWalletStore.KEY_WALLET, null))
        val card = SettlementCard.of(trips, results, wallet.balance)
        val edit = prefs.edit().putString(KEY_SEASON, now.toString()).putString("tier", result.toTier)
            .putString(KEY_NOTICE, TierMovement.message(result)).putString(KEY_NOTICE_SEASON, now.toString())
        if (card != null) edit.putString(PointWalletStore.KEY_WALLET, wallet.plus(card.points).encode()).putString(KEY_CARD, card.encode())
        return edit.commit()
    }

    /** 아직 보여 주지 않은 시상대 화면. 지급은 이미 끝났고 표시만 남은 상태다. */
    fun card(): SettlementCard? = SettlementCard.decode(prefs.getString(KEY_CARD, null))

    fun clearCard() { prefs.edit().remove(KEY_CARD).commit() }

    /** 지금 시즌에 보여 줄 지난 시즌 결과 안내 */
    fun notice(now: YearMonth = YearMonth.now()): String? =
        prefs.getString(KEY_NOTICE, null).takeIf { prefs.getString(KEY_NOTICE_SEASON, null) == now.toString() }

    private companion object {
        const val KEY_SEASON = "season"
        const val KEY_NOTICE = "season_notice"
        const val KEY_NOTICE_SEASON = "season_notice_for"
        const val KEY_CARD = "settlement_card"
    }
}
