package com.acrovox.core.player

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.media3.common.Player
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface SleepTimerState {
    data object Off : SleepTimerState

    /** Arrêt à [endsAt] (epoch ms). */
    data class Until(val endsAt: Long) : SleepTimerState

    /** Arrêt à la fin de l'épisode en cours. */
    data object EndOfEpisode : SleepTimerState
}

/**
 * Minuteur de sommeil : fondu sur les 10 dernières secondes, puis pause.
 * Secouer le téléphone pendant que le minuteur tourne le prolonge de 5 minutes.
 */
@Singleton
class SleepTimer @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var player: Player? = null
    private var job: Job? = null
    private var scope: CoroutineScope? = null
    private val sensors = context.getSystemService(SensorManager::class.java)
    private var shakeRegistered = false

    fun set(minutes: Int, now: Long = System.currentTimeMillis()) {
        _state.value = SleepTimerState.Until(now + minutes * 60_000L)
        restart()
    }

    fun setEndOfEpisode() {
        _state.value = SleepTimerState.EndOfEpisode
        restart()
    }

    fun cancel() {
        _state.value = SleepTimerState.Off
        restart()
    }

    fun extend(minutes: Int = EXTEND_MINUTES, now: Long = System.currentTimeMillis()) {
        val current = _state.value as? SleepTimerState.Until ?: return
        _state.value = SleepTimerState.Until(maxOf(current.endsAt, now) + minutes * 60_000L)
    }

    /** Appelé par le service en fin d'épisode : vrai s'il faut s'arrêter là. */
    internal fun consumeEndOfEpisode(): Boolean {
        if (_state.value != SleepTimerState.EndOfEpisode) return false
        cancel()
        return true
    }

    internal fun attach(player: Player, scope: CoroutineScope) {
        this.player = player
        this.scope = scope
        restart()
    }

    internal fun detach() {
        job?.cancel()
        unregisterShake()
        player = null
        scope = null
    }

    private fun restart() {
        job?.cancel()
        player?.volume = 1f
        val state = _state.value
        if (state == SleepTimerState.Off) {
            unregisterShake()
            return
        }
        registerShake()
        if (state !is SleepTimerState.Until) return
        job = scope?.launch {
            while (isActive) {
                val current = _state.value as? SleepTimerState.Until ?: break
                val remaining = current.endsAt - System.currentTimeMillis()
                val p = player ?: break
                if (remaining <= 0) {
                    p.pause()
                    p.volume = 1f
                    _state.value = SleepTimerState.Off
                    unregisterShake()
                    break
                }
                p.volume = if (remaining < FADE_MS) remaining.toFloat() / FADE_MS else 1f
                delay(TICK_MS)
            }
        }
    }

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x, y, z) = event.values.map { it / SensorManager.GRAVITY_EARTH }
            if (sqrt(x * x + y * y + z * z) > SHAKE_G_FORCE) extend()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerShake() {
        if (shakeRegistered) return
        val accelerometer = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        shakeRegistered = sensors.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterShake() {
        if (!shakeRegistered) return
        sensors?.unregisterListener(shakeListener)
        shakeRegistered = false
    }

    private companion object {
        const val FADE_MS = 10_000L
        const val TICK_MS = 250L
        const val EXTEND_MINUTES = 5
        const val SHAKE_G_FORCE = 2.5f
    }
}
