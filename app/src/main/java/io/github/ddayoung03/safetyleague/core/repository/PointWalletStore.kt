package io.github.ddayoung03.safetyleague.core.repository

import android.content.Context
import io.github.ddayoung03.safetyleague.core.calculator.Wallet

/** 티어와 같은 파일에 둔다. 시즌 정산이 티어와 포인트를 한 번의 commit으로 쓰기 위해서다. */
class PointWalletStore(context: Context) {
    private val prefs = context.getSharedPreferences("vehicle_profile", Context.MODE_PRIVATE)
    fun load(): Wallet = Wallet.decode(prefs.getString(KEY_WALLET, null))
    fun save(wallet: Wallet): Boolean = prefs.edit().putString(KEY_WALLET, wallet.encode()).commit()

    companion object {
        const val KEY_WALLET = "point_wallet"
    }
}
