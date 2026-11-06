package io.github.ddayoung03.safetyleague.ui

import android.view.View
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.ui.motion.MotionScope
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens

/** 오른쪽 아래 체커기 로딩을 잠깐 보인다. 화면이 바뀌거나 멈추면 motion.finishAll()이 숨긴다. 주행 제한 중에는 보이지 않는다. */
internal fun MainActivity.showFlagLoader(durationMs: Long = MotionTokens.LOADER_MAX, text: Int = R.string.loader_tally) {
    if (restricted) return
    val loader = binding.flagLoader
    binding.flagLoaderText.setText(text)
    lateinit var handle: MotionScope.Handle
    val hide = Runnable { loader.visibility = View.GONE; binding.flagLoaderFlag.stop(); handle.done() }
    handle = motion.add(onFinish = hide::run) { loader.removeCallbacks(hide) }
    loader.visibility = View.VISIBLE
    binding.flagLoaderFlag.start()
    loader.postDelayed(hide, durationMs.coerceAtMost(MotionTokens.LOADER_MAX))
}
