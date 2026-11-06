package io.github.ddayoung03.safetyleague.ui.motion

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import io.github.ddayoung03.safetyleague.R

enum class Fx(internal val res: Int) {
    LIGHT(R.raw.fx_light), GO(R.raw.fx_go), OVERTAKE(R.raw.fx_overtake), WIN(R.raw.fx_win)
}

/**
 * 결과 연출의 짧은 효과음. 소리를 낼지는 [canPlaySound]가 정하고, 여기서는 포커스를 한 번 얻어 재생만 한다.
 * 로딩은 비동기라 아직 안 불러온 소리는 건너뛴다.
 */
class SoundFx(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attrs).build()
    private val ids = Fx.values().associateWith { pool.load(context, it.res, 1) }
    private val loaded = mutableSetOf<Int>()
    private val streams = mutableListOf<Int>()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs).build()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
    }

    /** 연출 시작에 한 번. 포커스를 못 얻으면 false이고 그 연출은 조용히 돈다. */
    fun requestFocus() = audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    fun play(id: Fx) {
        val sound = ids.getValue(id)
        if (sound in loaded) streams += pool.play(sound, 1f, 1f, 1, 0, 1f)
    }

    /** 재생을 멈추고 포커스를 돌려준다. */
    fun stopAll() {
        streams.forEach(pool::stop)
        streams.clear()
        audio.abandonAudioFocusRequest(focusRequest)
    }

    fun release() {
        stopAll()
        pool.release()
    }
}
