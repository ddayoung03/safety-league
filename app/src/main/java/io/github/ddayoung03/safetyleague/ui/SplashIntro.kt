package io.github.ddayoung03.safetyleague.ui

import android.animation.ValueAnimator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled

/**
 * 시작 연출 2초: 아이콘이 튀어나오고, 글자가 모이고, 노란 바가 찬 뒤 걷힌다. 그 아래 첫 화면은 평소대로 만들어진다.
 * 화면 전환의 motion.finishAll()이 지우지 않게 motion에 등록하지 않고 뷰 자체 애니메이션으로 돈다.
 */
internal fun MainActivity.playSplash() {
    val s = binding.splash
    if (restricted || !animationsEnabled()) { hideSplash(); return }
    binding.splashWord.text = SpannableString("SAFETY LEAGUE").apply {
        setSpan(ForegroundColorSpan(color(R.color.yellow)), 7, 13, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    s.visibility = View.VISIBLE
    s.alpha = 1f
    s.setOnClickListener { hideSplash() }
    binding.splashIcon.apply { scaleX = .6f; scaleY = .6f; alpha = 0f }
    binding.splashWord.alpha = 0f
    binding.splashBar.scaleX = 0f
    // 창이 열리는 시스템 애니메이션 동안은 가려지므로, 창이 포커스를 받은 뒤 시작한다
    if (s.hasWindowFocus()) runSplash() else s.viewTreeObserver.addOnWindowFocusChangeListener(object : ViewTreeObserver.OnWindowFocusChangeListener {
        override fun onWindowFocusChanged(hasFocus: Boolean) {
            if (!hasFocus) return
            s.post { s.viewTreeObserver.removeOnWindowFocusChangeListener(this) }
            if (s.visibility == View.VISIBLE) runSplash()
        }
    })
}

private fun MainActivity.runSplash() {
    val s = binding.splash
    binding.splashIcon.apply {
        animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(450).setInterpolator(OvershootInterpolator(2.2f)).start()
    }
    binding.splashWord.apply {
        animate().alpha(1f).setStartDelay(300).setDuration(500).start()
        val spacing = ValueAnimator.ofFloat(.4f, .12f).setDuration(500)
        spacing.startDelay = 300
        spacing.interpolator = DecelerateInterpolator(2f)
        spacing.addUpdateListener { letterSpacing = it.animatedValue as Float }
        spacing.start()
    }
    binding.splashBar.apply {
        animate().scaleX(1f).setStartDelay(400).setDuration(1200).setInterpolator(DecelerateInterpolator()).start()
    }
    s.postDelayed({ if (s.visibility == View.VISIBLE) s.animate().alpha(0f).setDuration(300).withEndAction { hideSplash() }.start() }, 1700)
}

/** 바로 걷는다. 주행 제한이 걸리거나 앱이 뒤로 가거나 터치하면 부른다. */
internal fun MainActivity.hideSplash() {
    val s = binding.splash
    s.animate().cancel()
    s.visibility = View.GONE
}
