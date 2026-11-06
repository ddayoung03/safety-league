package io.github.ddayoung03.safetyleague.ui.motion

/** 진행 중인 움직임을 모아 두었다가 화면이 바뀔 때 한꺼번에 끝낸다. 안드로이드에 의존하지 않는다. */
class MotionScope {
    inner class Handle internal constructor(internal val onFinish: () -> Unit, internal val cancel: () -> Unit) {
        /** 스스로 끝난 항목을 지운다. */
        fun done() { items.remove(this) }
    }

    private val items = mutableListOf<Handle>()

    fun add(onFinish: () -> Unit = {}, cancel: () -> Unit): Handle =
        Handle(onFinish, cancel).also { items.add(it) }

    /** 남은 항목을 멈추고 마지막 상태로 맞춘다. 끝내는 중에 추가된 항목도 끝낸다. */
    fun finishAll() {
        while (items.isNotEmpty()) {
            val h = items.removeAt(0)
            h.cancel()
            h.onFinish()
        }
    }
}
