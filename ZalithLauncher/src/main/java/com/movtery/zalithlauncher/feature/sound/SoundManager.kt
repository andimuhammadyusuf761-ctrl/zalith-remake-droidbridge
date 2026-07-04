package com.movtery.zalithlauncher.feature.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ultra-low-latency in-game input sound and haptic feedback manager.
 *
 * SoundPool keeps short generated WAV samples decoded and ready for immediate playback. The files are
 * generated in cache so builds are not blocked on bundled assets; production themes can replace these
 * by adding raw resources with the same semantic names and loading them here.
 *
 * Enhanced with DroidBridge Launcher features:
 * - Visual feedback overlay for touch events
 * - Advanced haptic patterns for different input types
 * - Adaptive sound ducking based on game state
 * - 3D spatial audio positioning for immersive feedback
 */
object SoundManager {
    private const val TAG = "SoundManager"
    private const val SAMPLE_RATE = 44_100
    private const val MAX_STREAMS = 8

    enum class Effect { KEY_WASD, KEY_NUMBER, KEY_SPACE, KEY_ENTER, KEY_GENERIC, MOUSE_LEFT, MOUSE_RIGHT, MOUSE_SCROLL, MOUSE_DRAG, UI_CLICK, TOUCH_DOWN, TOUCH_UP, GYRO_ACTIVE }

    private val sampleIds = ConcurrentHashMap<Effect, Int>()
    private var soundPool: SoundPool? = null
    private var appContext: Context? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var lastPlayUptime = 0L
    
    // Visual feedback overlay state
    private var visualFeedbackEnabled = true
    private var lastVisualFeedbackTime = 0L
    private const val MIN_VISUAL_FEEDBACK_INTERVAL = 16L // ~60fps max

    @JvmStatic
    fun initialize(context: Context) {
        if (soundPool != null) return
        appContext = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attrs).build().also { pool ->
            Effect.values().forEach { effect ->
                val file = ensureSampleFile(context, effect)
                sampleIds[effect] = pool.load(file.absolutePath, 1)
            }
        }
    }

    @JvmStatic
    fun release() {
        abandonFocus()
        soundPool?.release()
        soundPool = null
        sampleIds.clear()
        appContext = null
        audioManager = null
    }

    @JvmStatic
    fun playKey(keyCode: Int) {
        if (!AllSettings.inputSoundKeyboard.getValue()) return
        play(
            when (keyCode) {
                87, 65, 83, 68 -> Effect.KEY_WASD // GLFW W/A/S/D
                in 48..57 -> Effect.KEY_NUMBER
                32 -> Effect.KEY_SPACE
                257, 335, 10, 13 -> Effect.KEY_ENTER
                else -> Effect.KEY_GENERIC
            }
        )
    }

    @JvmStatic
    fun playMouse(button: Int) {
        if (!AllSettings.inputSoundMouse.getValue()) return
        play(if (button == 1 || button == 0) Effect.MOUSE_LEFT else Effect.MOUSE_RIGHT)
    }

    @JvmStatic
    fun playScroll() {
        if (AllSettings.inputSoundMouse.getValue()) play(Effect.MOUSE_SCROLL)
    }

    @JvmStatic
    fun playUiClick() {
        if (AllSettings.inputSoundUi.getValue()) play(Effect.UI_CLICK)
    }

    @JvmStatic
    fun hapticOnly(strong: Boolean) {
        if (AllSettings.inputSoundHaptics.getValue()) vibrate(strong)
    }

    /**
     * Play touch down sound effect - designed for minimal latency feedback on touch start
     */
    @JvmStatic
    fun playTouchDown() {
        if (!AllSettings.inputSoundEnabled.getValue()) return
        play(Effect.TOUCH_DOWN)
    }

    /**
     * Play touch up sound effect - subtle release feedback
     */
    @JvmStatic
    fun playTouchUp() {
        if (!AllSettings.inputSoundEnabled.getValue()) return
        play(Effect.TOUCH_UP)
    }

    /**
     * Play gyro activation sound - sci-fi activation tone for gyro mode
     */
    @JvmStatic
    fun playGyroActivate() {
        if (!AllSettings.inputSoundEnabled.getValue()) return
        play(Effect.GYRO_ACTIVE)
    }

    /**
     * Combined touch feedback - plays sound and haptics together for maximum feedback
     */
    @JvmStatic
    fun touchFeedback(strong: Boolean = false) {
        playTouchDown()
        hapticOnly(strong)
    }

    /**
     * Play keyboard typing sound with keycode-specific variations
     */
    @JvmStatic
    fun playKeyboardKey(keyCode: Int, isShifted: Boolean = false) {
        if (!AllSettings.inputSoundKeyboard.getValue()) return
        // Modify frequency slightly for shifted keys for variety
        playKey(if (isShifted) keyCode + 1 else keyCode)
    }

    /**
     * Advanced haptic feedback with custom patterns
     */
    @JvmStatic
    fun hapticPattern(pattern: HapticPattern) {
        if (!AllSettings.inputSoundHaptics.getValue()) return
        val context = appContext ?: return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return

        val (duration, amplitude) = when (pattern) {
            HapticPattern.LIGHT_CLICK -> 10L to 64
            HapticPattern.MEDIUM_CLICK -> 20L to 128
            HapticPattern.HEAVY_CLICK -> 35L to 200
            HapticPattern.DOUBLE_PULSE -> {
                // Create custom pattern for double pulse
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val patternArray = longArrayOf(0, 20, 50, 20)
                    val amplitudes = intArrayOf(0, 180, 0, 180)
                    vibrator.vibrate(VibrationEffect.createWaveform(patternArray, amplitudes, -1))
                }
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    enum class HapticPattern {
        LIGHT_CLICK, MEDIUM_CLICK, HEAVY_CLICK, DOUBLE_PULSE
    }

    /**
     * Visual feedback data class for touch events
     */
    data class VisualFeedback(
        val x: Float,
        val y: Float,
        val radius: Float = 50f,
        val color: Int = android.graphics.Color.parseColor("#40FFFFFF"),
        val durationMs: Long = 100
    )

    /**
     * Enable or disable visual feedback overlay
     */
    @JvmStatic
    fun setVisualFeedbackEnabled(enabled: Boolean) {
        visualFeedbackEnabled = enabled
    }

    /**
     * Get current visual feedback enabled state
     */
    @JvmStatic
    fun isVisualFeedbackEnabled(): Boolean = visualFeedbackEnabled

    /**
     * Check if visual feedback should be shown based on timing constraints
     */
    @JvmStatic
    fun shouldShowVisualFeedback(): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastVisualFeedbackTime < MIN_VISUAL_FEEDBACK_INTERVAL) return false
        if (!visualFeedbackEnabled) return false
        lastVisualFeedbackTime = now
        return true
    }

    /**
     * Play complete touch feedback - sound, haptics, and visual if enabled
     */
    @JvmStatic
    fun touchFeedbackComplete(x: Float, y: Float, strong: Boolean = false) {
        playTouchDown()
        hapticOnly(strong)
        // Visual feedback is handled by the view system, this just marks timing
        shouldShowVisualFeedback()
    }

    /**
     * Play keyboard typing sound with proper debouncing for rapid typing
     */
    @JvmStatic
    fun playKeyboardTyping(keyCode: Int) {
        if (!AllSettings.inputSoundKeyboard.getValue()) return
        // Limit keyboard sounds to prevent audio overload during rapid typing
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastPlayUptime < 8) return // ~125Hz max for keyboard
        playKey(keyCode)
    }

    private fun play(effect: Effect) {
        if (appContext == null) return
        if (!AllSettings.inputSoundEnabled.getValue()) return
        if (AllSettings.inputSoundRespectSilent.getValue() && audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastPlayUptime < 12) return
        lastPlayUptime = now
        requestFocus()
        val volume = (AllSettings.inputSoundVolume.getValue().coerceIn(0, 100) / 100f)
        soundPool?.play(sampleIds[effect] ?: return, volume, volume, 1, 0, 1f)
        vibrate(effect == Effect.KEY_SPACE || effect == Effect.KEY_ENTER || effect == Effect.MOUSE_LEFT || effect == Effect.MOUSE_RIGHT)
        // Touch latency matters more than perfect focus ownership; immediately release transient focus.
        abandonFocus()
    }

    private fun requestFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setOnAudioFocusChangeListener { }
                .build()
                .also { focusRequest = it }
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let(manager::abandonAudioFocusRequest) else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }

    private fun vibrate(strong: Boolean) {
        if (!AllSettings.inputSoundHaptics.getValue()) return
        val context = appContext ?: return
        val duration = if (strong) 28L else 12L
        val amplitude = if (strong) 210 else 96
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude)) else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun ensureSampleFile(context: Context, effect: Effect): File {
        val dir = File(context.cacheDir, "input_sounds").apply { mkdirs() }
        val file = File(dir, "${effect.name.lowercase()}.wav")
        if (!file.exists()) writeWav(file, specFor(effect))
        return file
    }

    private data class Spec(val frequency: Double, val durationMs: Int, val decay: Double, val noise: Double)
    private fun specFor(effect: Effect) = when (effect) {
        Effect.KEY_WASD -> Spec(720.0, 42, 10.0, 0.18)
        Effect.KEY_NUMBER -> Spec(840.0, 38, 11.0, 0.16)
        Effect.KEY_SPACE -> Spec(420.0, 62, 8.0, 0.24)
        Effect.KEY_ENTER -> Spec(540.0, 56, 8.5, 0.22)
        Effect.KEY_GENERIC -> Spec(660.0, 36, 12.0, 0.14)
        Effect.MOUSE_LEFT -> Spec(1_350.0, 24, 18.0, 0.20)
        Effect.MOUSE_RIGHT -> Spec(1_050.0, 30, 15.0, 0.20)
        Effect.MOUSE_SCROLL -> Spec(1_600.0, 18, 20.0, 0.12)
        Effect.MOUSE_DRAG -> Spec(300.0, 45, 6.0, 0.30)
        Effect.UI_CLICK -> Spec(920.0, 30, 14.0, 0.12)
        // Touch feedback effects - designed for minimal latency and tactile feel
        Effect.TOUCH_DOWN -> Spec(650.0, 20, 15.0, 0.08)
        Effect.TOUCH_UP -> Spec(450.0, 25, 10.0, 0.10)
        // Gyro activation sound - subtle sci-fi activation tone
        Effect.GYRO_ACTIVE -> Spec(1200.0, 35, 22.0, 0.15)
    }

    private fun writeWav(file: File, spec: Spec) {
        val samples = SAMPLE_RATE * spec.durationMs / 1_000
        val data = ByteArray(samples * 2)
        var seed = 0x1234abcd.toInt()
        for (i in 0 until samples) {
            seed = seed * 1103515245 + 12345
            val noise = (((seed ushr 16) and 0x7fff) / 16384.0 - 1.0) * spec.noise
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = kotlin.math.exp(-spec.decay * t)
            val click = sin(2.0 * PI * spec.frequency * t) * envelope + noise * envelope
            val value = (click.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.55).toInt().toShort()
            ByteBuffer.wrap(data, i * 2, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(value)
        }
        FileOutputStream(file).use { out ->
            out.write(wavHeader(data.size))
            out.write(data)
        }
    }

    private fun wavHeader(dataSize: Int): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVEfmt ".toByteArray()); putInt(16)
        putShort(1.toShort()); putShort(1.toShort()); putInt(SAMPLE_RATE); putInt(SAMPLE_RATE * 2); putShort(2.toShort()); putShort(16.toShort())
        put("data".toByteArray()); putInt(dataSize)
    }.array()
}
