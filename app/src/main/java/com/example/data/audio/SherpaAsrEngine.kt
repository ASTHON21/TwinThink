package com.example.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.data.engine.LocalDialecticEngine
import com.example.data.model.AcousticMode
import com.example.data.model.DictionaryTermEntity
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.math.log10
import kotlin.math.sqrt

enum class AsrEngineStatus(val labelZh: String) {
    IDLE("待命"),
    LOADING_MODEL("正在装载端侧声学模型…"),
    READY_STREAMING("SHERPA-ONNX 中英双语流式就绪"),
    PURE_WAVE_ONLY("纯声波本地归档模式"),
    MODEL_UNAVAILABLE("本地识别不可用 · 已降级声波速记")
}

data class AsrCaptureState(
    val engineStatus: AsrEngineStatus = AsrEngineStatus.IDLE,
    val committedText: String = "",
    val partialText: String = "",
    val isCapturing: Boolean = false,
    val isPaused: Boolean = false,
    val amplitude: Float = 0.08f,
    val dbfsLabel: String = "-48.0 dBFS",
    val elapsedMillis: Long = 0L,
    val silenceProgress: Float = 0f,
    val statusNotice: String? = null
)

data class AsrCaptureResult(
    val transcript: String,
    val audioFile: File?,
    val durationSec: Int
)

private object SherpaRecognizerHolder {
    private const val TAG = "SherpaRecognizerHolder"
    private const val MODEL_DIR = "sherpa-onnx-streaming-zipformer-small-bilingual-zh-en"

    private val mutex = Mutex()
    @Volatile
    private var cachedRecognizer: OnlineRecognizer? = null
    @Volatile
    private var loadFailedReason: String? = null

    suspend fun getOrCreate(context: Context): Result<OnlineRecognizer> {
        cachedRecognizer?.let { return Result.success(it) }
        return mutex.withLock {
            cachedRecognizer?.let { return Result.success(it) }
            withContext(Dispatchers.IO) {
                try {
                    val config = OnlineRecognizerConfig(
                        featConfig = FeatureConfig(
                            sampleRate = 16000,
                            featureDim = 80,
                            dither = 0.0f
                        ),
                        modelConfig = OnlineModelConfig(
                            transducer = OnlineTransducerModelConfig(
                                encoder = "$MODEL_DIR/encoder-epoch-99-avg-1.int8.onnx",
                                decoder = "$MODEL_DIR/decoder-epoch-99-avg-1.onnx",
                                joiner = "$MODEL_DIR/joiner-epoch-99-avg-1.int8.onnx"
                            ),
                            tokens = "$MODEL_DIR/tokens.txt",
                            numThreads = 2,
                            debug = false,
                            provider = "cpu",
                            modelType = "zipformer"
                        ),
                        endpointConfig = EndpointConfig(
                            rule1 = EndpointRule(
                                mustContainNonSilence = false,
                                minTrailingSilence = 2.4f,
                                minUtteranceLength = 0.0f
                            ),
                            rule2 = EndpointRule(
                                mustContainNonSilence = true,
                                minTrailingSilence = 1.2f,
                                minUtteranceLength = 0.0f
                            ),
                            rule3 = EndpointRule(
                                mustContainNonSilence = false,
                                minTrailingSilence = 0.0f,
                                minUtteranceLength = 20.0f
                            )
                        ),
                        enableEndpoint = true,
                        decodingMethod = "greedy_search",
                        maxActivePaths = 4
                    )
                    val recognizer = OnlineRecognizer(
                        assetManager = context.applicationContext.assets,
                        config = config
                    )
                    cachedRecognizer = recognizer
                    loadFailedReason = null
                    Result.success(recognizer)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to initialize Sherpa-ONNX OnlineRecognizer", t)
                    loadFailedReason = t.message ?: t::class.java.simpleName
                    Result.failure(t)
                }
            }
        }
    }
}

class SherpaCaptureController(private val context: Context) {
    companion object {
        private const val TAG = "SherpaCaptureController"
        const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val READ_CHUNK_SHORTS = 1600 // 100ms per chunk at 16kHz
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(AsrCaptureState())
    val state: StateFlow<AsrCaptureState> = _state.asStateFlow()

    private val streamLock = Any()
    private var recognizer: OnlineRecognizer? = null
    private var onlineStream: OnlineStream? = null
    private var audioRecord: AudioRecord? = null
    private var wavFile: File? = null
    private var wavOutputStream: FileOutputStream? = null
    private var pcmDataBytes: Long = 0L

    private var captureJob: Job? = null
    @Volatile
    private var isPausedFlag: Boolean = false
    @Volatile
    private var isFinalized: Boolean = false

    private var currentAutoPunctuate: Boolean = true
    private var currentSilenceThresholdSec: Float = 3.0f
    private var currentDictionary: List<DictionaryTermEntity> = emptyList()
    private var onAutoSealCallback: (() -> Unit)? = null
    private var onWaveformSampleCallback: ((Float) -> Unit)? = null

    private var autoSealDispatched: Boolean = false

    fun updateRuntimeConfig(
        autoPunctuate: Boolean,
        silenceThresholdSec: Float,
        dictionary: List<DictionaryTermEntity>
    ) {
        currentAutoPunctuate = autoPunctuate
        currentSilenceThresholdSec = silenceThresholdSec
        currentDictionary = dictionary
    }

    fun updateCommittedText(text: String) {
        _state.update { it.copy(committedText = text, silenceProgress = 0f) }
    }

    @SuppressLint("MissingPermission")
    fun startCapture(
        acousticMode: AcousticMode,
        autoPunctuate: Boolean,
        silenceThresholdSec: Float,
        dictionary: List<DictionaryTermEntity>,
        onWaveformSample: (Float) -> Unit,
        onAutoSealTriggered: () -> Unit
    ) {
        if (captureJob?.isActive == true) return
        isFinalized = false
        isPausedFlag = false
        autoSealDispatched = false
        currentAutoPunctuate = autoPunctuate
        currentSilenceThresholdSec = silenceThresholdSec
        currentDictionary = dictionary
        onWaveformSampleCallback = onWaveformSample
        onAutoSealCallback = onAutoSealTriggered

        captureJob = scope.launch {
            if (acousticMode == AcousticMode.OFFLINE_RECOGNIZER) {
                _state.update {
                    it.copy(
                        engineStatus = AsrEngineStatus.LOADING_MODEL,
                        statusNotice = null
                    )
                }
                val loaded = SherpaRecognizerHolder.getOrCreate(context)
                loaded.onSuccess { rec ->
                    synchronized(streamLock) {
                        recognizer = rec
                        onlineStream = rec.createStream("")
                    }
                    _state.update {
                        it.copy(
                            engineStatus = AsrEngineStatus.READY_STREAMING,
                            statusNotice = null
                        )
                    }
                }.onFailure { err ->
                    _state.update {
                        it.copy(
                            engineStatus = AsrEngineStatus.MODEL_UNAVAILABLE,
                            statusNotice = "端侧语音模型加载受限（${err.message ?: "硬件不支持"}），已保留本地声波录音与手动文字速记。"
                        )
                    }
                }
            } else {
                _state.update {
                    it.copy(
                        engineStatus = AsrEngineStatus.PURE_WAVE_ONLY,
                        statusNotice = "当前处于纯声波本地归档模式，仅保存高保真 WAV 波形录音。"
                    )
                }
            }

            val minBufBytes = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(READ_CHUNK_SHORTS * 4)

            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufBytes * 2
                )
            } catch (t: Throwable) {
                Log.e(TAG, "AudioRecord creation failed", t)
                null
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                _state.update {
                    it.copy(
                        isCapturing = false,
                        statusNotice = "麦克风采集通道初始化失败，你仍可直接在上方输入文字完成思辨。"
                    )
                }
                return@launch
            }

            val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val outFile = File(recordingsDir, "dialectic_${UUID.randomUUID().toString().take(8)}.wav")
            val fos = try {
                FileOutputStream(outFile).also { writeWavPlaceholderHeader(it) }
            } catch (t: Throwable) {
                Log.e(TAG, "WAV file creation failed", t)
                null
            }

            audioRecord = record
            wavFile = if (fos != null) outFile else null
            wavOutputStream = fos
            pcmDataBytes = 0L

            try {
                record.startRecording()
            } catch (t: Throwable) {
                Log.e(TAG, "AudioRecord startRecording failed", t)
                record.release()
                audioRecord = null
                _state.update {
                    it.copy(
                        isCapturing = false,
                        statusNotice = "无法启动麦克风采集，请检查录音权限或改用文字速记。"
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    isCapturing = true,
                    isPaused = false,
                    elapsedMillis = 0L,
                    silenceProgress = 0f
                )
            }

            val shortBuffer = ShortArray(READ_CHUNK_SHORTS)
            val byteBuffer = ByteArray(READ_CHUNK_SHORTS * 2)
            var lastTickAt = System.currentTimeMillis()
            var silenceAccumulatorSec = 0f

            while (isActive && !isFinalized) {
                val now = System.currentTimeMillis()
                val deltaMs = (now - lastTickAt).coerceIn(0L, 250L)
                lastTickAt = now

                if (isPausedFlag) {
                    try {
                        Thread.sleep(60L)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                val readShorts = try {
                    record.read(shortBuffer, 0, shortBuffer.size)
                } catch (_: Throwable) {
                    -1
                }

                if (readShorts <= 0) {
                    try {
                        Thread.sleep(30L)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                // 1. Write PCM16 LE bytes to WAV stream & compute RMS amplitude
                var sumSquares = 0.0
                for (i in 0 until readShorts) {
                    val sample = shortBuffer[i].toInt()
                    byteBuffer[i * 2] = (sample and 0xFF).toByte()
                    byteBuffer[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
                    val norm = sample / 32768.0
                    sumSquares += norm * norm
                }

                wavOutputStream?.let { stream ->
                    try {
                        stream.write(byteBuffer, 0, readShorts * 2)
                        pcmDataBytes += (readShorts * 2).toLong()
                    } catch (e: Throwable) {
                        Log.w(TAG, "WAV write error: ${e.message}")
                    }
                }

                val rms = sqrt(sumSquares / readShorts.toDouble()).toFloat()
                // Speech RMS typically sits around 0.02..0.35; boost gently for crisp UI meter response
                val normalizedAmp = (rms * 3.8f).coerceIn(0.04f, 1.0f)
                val dbfs = amplitudeToDbfs(normalizedAmp)
                onWaveformSampleCallback?.invoke(normalizedAmp)

                // 2. Feed samples into Sherpa-ONNX streaming recognizer
                var committedDelta: String? = null
                var currentPartial = _state.value.partialText

                synchronized(streamLock) {
                    val rec = recognizer
                    val stream = onlineStream
                    if (rec != null && stream != null) {
                        val floatSamples = FloatArray(readShorts) { idx ->
                            shortBuffer[idx] / 32768.0f
                        }
                        stream.acceptWaveform(floatSamples, SAMPLE_RATE)
                        while (rec.isReady(stream)) {
                            rec.decode(stream)
                        }
                        val recognizedText = rec.getResult(stream).text.trim()
                        if (rec.isEndpoint(stream)) {
                            if (recognizedText.isNotEmpty()) {
                                committedDelta = LocalDialecticEngine.formatPhilosophicalText(
                                    raw = recognizedText,
                                    autoPunctuate = currentAutoPunctuate,
                                    dictionary = currentDictionary
                                )
                            }
                            rec.reset(stream)
                            currentPartial = ""
                        } else {
                            currentPartial = recognizedText
                        }
                    }
                }

                // 3. Track VAD silence auto-seal countdown once speech text exists
                val hasSpeechContent = committedDelta != null ||
                    _state.value.committedText.isNotBlank() ||
                    currentPartial.isNotBlank()

                val chunkSec = readShorts.toFloat() / SAMPLE_RATE.toFloat()
                if (hasSpeechContent && normalizedAmp < 0.12f) {
                    silenceAccumulatorSec += chunkSec
                } else if (normalizedAmp >= 0.12f || committedDelta != null) {
                    silenceAccumulatorSec = 0f
                }

                val threshold = currentSilenceThresholdSec.coerceIn(1.5f, 8.0f)
                val silenceProg = if (hasSpeechContent) {
                    (silenceAccumulatorSec / threshold).coerceIn(0f, 1f)
                } else {
                    0f
                }

                _state.update { prev ->
                    val nextCommitted = if (committedDelta != null) {
                        appendSegment(prev.committedText, committedDelta!!)
                    } else {
                        prev.committedText
                    }
                    prev.copy(
                        committedText = nextCommitted,
                        partialText = currentPartial,
                        amplitude = normalizedAmp,
                        dbfsLabel = dbfs,
                        elapsedMillis = prev.elapsedMillis + deltaMs,
                        silenceProgress = silenceProg
                    )
                }

                if (silenceProg >= 1f && !autoSealDispatched && !isFinalized) {
                    autoSealDispatched = true
                    onAutoSealCallback?.invoke()
                    break
                }
            }
        }
    }

    fun pause() {
        isPausedFlag = true
        _state.update { it.copy(isPaused = true, isCapturing = false, amplitude = 0.04f) }
    }

    fun resume() {
        isPausedFlag = false
        _state.update { it.copy(isPaused = false, isCapturing = true) }
    }

    suspend fun stopAndKeep(): AsrCaptureResult = withContext(Dispatchers.IO) {
        if (isFinalized) {
            val s = _state.value
            return@withContext AsrCaptureResult(
                transcript = s.committedText.trim(),
                audioFile = wavFile?.takeIf { it.exists() && it.length() > 44L },
                durationSec = (s.elapsedMillis / 1000L).toInt().coerceAtLeast(1)
            )
        }
        isFinalized = true
        captureJob?.cancel()
        try {
            captureJob?.join()
        } catch (_: Throwable) {
        }

        // Flush any remaining partial frames in Sherpa-ONNX stream
        var flushedTail = ""
        synchronized(streamLock) {
            val rec = recognizer
            val stream = onlineStream
            if (rec != null && stream != null) {
                try {
                    stream.inputFinished()
                    while (rec.isReady(stream)) {
                        rec.decode(stream)
                    }
                    val tailRaw = rec.getResult(stream).text.trim()
                    if (tailRaw.isNotEmpty()) {
                        flushedTail = LocalDialecticEngine.formatPhilosophicalText(
                            raw = tailRaw,
                            autoPunctuate = currentAutoPunctuate,
                            dictionary = currentDictionary
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Stream flush error: ${t.message}")
                }
            }
        }

        releaseAudioAndStream()
        val savedWav = finalizeWavFile()

        val snapshot = _state.value
        val fallbackPartial = if (flushedTail.isNotEmpty()) {
            flushedTail
        } else if (snapshot.partialText.isNotBlank()) {
            LocalDialecticEngine.formatPhilosophicalText(
                raw = snapshot.partialText,
                autoPunctuate = currentAutoPunctuate,
                dictionary = currentDictionary
            )
        } else {
            ""
        }

        val finalTranscript = appendSegment(snapshot.committedText, fallbackPartial).trim()
        val durationSec = (snapshot.elapsedMillis / 1000L).toInt().coerceAtLeast(1)

        _state.update {
            it.copy(
                committedText = finalTranscript,
                partialText = "",
                isCapturing = false,
                isPaused = false
            )
        }

        AsrCaptureResult(
            transcript = finalTranscript,
            audioFile = savedWav,
            durationSec = durationSec
        )
    }

    fun stopAndDiscard() {
        if (isFinalized) return
        isFinalized = true
        captureJob?.cancel()
        runBlocking(Dispatchers.IO) {
            try {
                captureJob?.join()
            } catch (_: Throwable) {
            }
            releaseAudioAndStream()
            closeWavStreamQuietly()
            wavFile?.delete()
            wavFile = null
        }
        _state.update { it.copy(isCapturing = false, isPaused = false, partialText = "") }
    }

    fun releaseOnDispose() {
        if (!isFinalized) {
            stopAndDiscard()
        }
        scope.cancel()
    }

    private fun releaseAudioAndStream() {
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }
        try {
            audioRecord?.release()
        } catch (_: Throwable) {
        }
        audioRecord = null

        synchronized(streamLock) {
            try {
                onlineStream?.release()
            } catch (_: Throwable) {
            }
            onlineStream = null
        }
    }

    private fun closeWavStreamQuietly() {
        try {
            wavOutputStream?.flush()
            wavOutputStream?.close()
        } catch (_: Throwable) {
        }
        wavOutputStream = null
    }

    private fun finalizeWavFile(): File? {
        closeWavStreamQuietly()
        val file = wavFile ?: return null
        if (!file.exists() || pcmDataBytes <= 0L) {
            file.delete()
            return null
        }
        return try {
            patchWavHeader(file, pcmDataBytes)
            file
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to patch WAV header: ${t.message}")
            file.takeIf { it.exists() && it.length() > 44L }
        }
    }

    private fun appendSegment(existing: String, segment: String): String {
        val a = existing.trim()
        val b = segment.trim()
        if (b.isEmpty()) return a
        if (a.isEmpty()) return b
        val lastChar = a.last()
        val firstChar = b.first()
        val needsSpace = lastChar.code < 128 && firstChar.code < 128 &&
            !lastChar.isWhitespace() && !firstChar.isWhitespace()
        return if (needsSpace) "$a $b" else "$a$b"
    }

    fun amplitudeToDbfs(normalized: Float): String {
        val clamped = normalized.coerceIn(0.001f, 1f)
        val db = 20.0 * log10(clamped.toDouble())
        return String.format(Locale.US, "%.1f dBFS", db.coerceAtLeast(-54.0))
    }

    private fun writeWavPlaceholderHeader(fos: FileOutputStream) {
        val header = ByteArray(44)
        fos.write(header)
    }

    private fun patchWavHeader(file: File, dataSize: Long) {
        val byteRate = (SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE) / 8
        val blockAlign = (CHANNELS * BITS_PER_SAMPLE) / 8
        val chunkSize = (36L + dataSize).toInt()
        val subchunk2Size = dataSize.toInt()

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(chunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1.toShort()) // PCM format
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(subchunk2Size)
        }.array()

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0L)
            raf.write(header)
        }
    }
}
