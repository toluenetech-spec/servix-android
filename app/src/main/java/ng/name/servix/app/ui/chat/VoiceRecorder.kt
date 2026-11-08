package ng.name.servix.app.ui.chat

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Hold-to-record voice notes with the platform recorder (AAC in an `.m4a` container, which is on the
 * API's audio allow-list as `audio/mp4`).
 *
 * The file is written to the app cache and handed back as bytes, because the upload endpoint takes a
 * raw body; nothing is uploaded until the user lets go and the message is sent.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    var isRecording by mutableStateOf(false)
        private set
    var elapsedMs by mutableStateOf(0L)
        private set

    fun start(): Boolean {
        if (isRecording) return true
        val target = File.createTempFile("voice-", ".m4a", context.cacheDir)
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            created.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setOutputFile(target.absolutePath)
                prepare()
                start()
            }
            recorder = created
            file = target
            startedAt = SystemClock.elapsedRealtime()
            elapsedMs = 0L
            isRecording = true
            true
        } catch (_: Throwable) {
            runCatching { created.release() }
            target.delete()
            recorder = null
            file = null
            isRecording = false
            false
        }
    }

    /** Stops and returns `(bytes, durationMs)`; null when nothing usable was captured. */
    fun stop(): Pair<ByteArray, Long>? {
        val active = recorder ?: return null
        val duration = SystemClock.elapsedRealtime() - startedAt
        recorder = null
        isRecording = false
        elapsedMs = duration
        runCatching { active.stop() }
        runCatching { active.release() }
        val target = file ?: return null
        file = null
        val bytes = runCatching { target.readBytes() }.getOrDefault(ByteArray(0))
        target.delete()
        // Anything under a second is a mis-tap, not a voice note.
        if (bytes.isEmpty() || duration < 800) return null
        return bytes to duration
    }

    fun cancel() {
        val active = recorder ?: return
        recorder = null
        isRecording = false
        runCatching { active.stop() }
        runCatching { active.release() }
        file?.delete()
        file = null
    }

    fun tick() {
        if (isRecording) elapsedMs = SystemClock.elapsedRealtime() - startedAt
    }
}

/** One voice note playing at a time; the bubble only draws the progress it actually has. */
class VoicePlayer {

    private var player: MediaPlayer? = null
    private var loadedUrl: String? = null
    private var listener: ((String, Boolean, Long) -> Unit)? = null

    fun setListener(block: (String, Boolean, Long) -> Unit) {
        listener = block
    }

    fun toggle(url: String) {
        if (url.isBlank()) return
        val active = player
        if (active != null && loadedUrl == url) {
            if (active.isPlaying) {
                active.pause()
                listener?.invoke(url, false, active.currentPosition.toLong())
            } else {
                active.start()
                listener?.invoke(url, true, active.currentPosition.toLong())
            }
            return
        }
        stop()
        val created = MediaPlayer()
        runCatching {
            created.setDataSource(url)
            created.setOnPreparedListener {
                it.start()
                loadedUrl = url
                listener?.invoke(url, true, 0L)
            }
            created.setOnCompletionListener {
                listener?.invoke(url, false, 0L)
                stop()
            }
            created.prepareAsync()
        }.onFailure {
            stop()
        }
        player = created
    }

    fun stop() {
        player?.let { active ->
            runCatching { if (active.isPlaying) active.stop() }
            runCatching { active.release() }
        }
        player = null
        loadedUrl = null
    }
}
