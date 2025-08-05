package com.example.bk_meeting;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceDetectionManager {

    private static final String TAG = "VoiceDetectionManager";

    // Audio configuration
    private final int sampleRate = 44100;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    // Voice detection parameters
    private final int silenceThreshold = 6000; // Độ lớn tối thiểu (amplitude)
    private final int silenceTimeoutMs = 1000; // Thời gian im lặng để kết thúc câu (ms)
    private final int minSentenceDurationMs = 15_000; // Thời gian ghi âm tối thiểu (15s)

    // Recording state
    private AudioRecord recorder;
    private boolean isRecording = false;
    private boolean isSpeaking = false;
    private Thread recordingThread;
    private Context context;
    private VoiceDetectionCallback callback;

    // UI Handler
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public enum VoiceState {
        IDLE,
        LISTENING,
        DETECTING_SPEECH,
        RECORDING,
        SAVING
    }

    public interface VoiceDetectionCallback {
        void onStateChanged(VoiceState state, String message);
        void onAudioFileSaved(String fileName, File audioFile);
        void onError(String error);
    }

    public VoiceDetectionManager(Context context, VoiceDetectionCallback callback) {
        this.context = context;
        this.callback = callback != null ? callback : new VoiceDetectionCallback() {
            @Override
            public void onStateChanged(VoiceState state, String message) {
                Log.d(TAG, "State: " + state + ", Message: " + message);
            }

            @Override
            public void onAudioFileSaved(String fileName, File audioFile) {
                Log.d(TAG, "Audio saved: " + fileName);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }
        };
    }

    public boolean startVoiceDetection() {
        if (isRecording) {
            Log.w(TAG, "Voice detection already running");
            return false;
        }

        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            mainHandler.post(() -> callback.onError("Không có quyền ghi âm"));
            return false;
        }

        try {
            initializeAudioRecord();
            startRecordingLoop();

            mainHandler.post(() -> callback.onStateChanged(VoiceState.LISTENING, "Đang lắng nghe..."));
            Log.d(TAG, "Voice detection started successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start voice detection", e);
            mainHandler.post(() -> callback.onError("Lỗi khởi tạo: " + e.getMessage()));
            return false;
        }
    }

    public void stopVoiceDetection() {
        isRecording = false;

        if (recordingThread != null) {
            recordingThread.interrupt();
        }

        cleanup();
        mainHandler.post(() -> callback.onStateChanged(VoiceState.IDLE, "Đã dừng"));
        Log.d(TAG, "Voice detection stopped");
    }

    private void initializeAudioRecord() throws SecurityException {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
        );

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new RuntimeException("Failed to initialize AudioRecord");
        }
    }

    private void startRecordingLoop() {
        isRecording = true;
        recorder.startRecording();

        recordingThread = new Thread(() -> {
            try {
                processAudioLoop();
            } catch (Exception e) {
                Log.e(TAG, "Error in recording loop", e);
                mainHandler.post(() -> callback.onError("Lỗi ghi âm: " + e.getMessage()));
            }
        });

        recordingThread.setName("VoiceDetectionThread");
        recordingThread.start();
    }

    private void processAudioLoop() {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream sentenceBuffer = new ByteArrayOutputStream();

        long lastVoiceTime = System.currentTimeMillis();
        long sentenceStartTime = 0;

        while (isRecording && !Thread.currentThread().isInterrupted()) {
            int bytesRead = recorder.read(buffer, 0, buffer.length);

            if (bytesRead <= 0) {
                continue;
            }

            int amplitude = calculateAmplitude(buffer, bytesRead);
            long currentTime = System.currentTimeMillis();

            if (amplitude > silenceThreshold) {
                // Voice detected
                if (!isSpeaking) {
                    // Start new sentence
                    isSpeaking = true;
                    sentenceBuffer.reset();
                    sentenceStartTime = currentTime;

                    mainHandler.post(() -> callback.onStateChanged(
                            VoiceState.DETECTING_SPEECH,
                            "Phát hiện giọng nói..."
                    ));
                }

                sentenceBuffer.write(buffer, 0, bytesRead);
                lastVoiceTime = currentTime;

                // Update to recording state if minimum duration reached
                long sentenceDuration = currentTime - sentenceStartTime;
                if (sentenceDuration >= minSentenceDurationMs) {
                    mainHandler.post(() -> callback.onStateChanged(
                            VoiceState.RECORDING,
                            "Đang ghi âm... (" + (sentenceDuration / 1000) + "s)"
                    ));
                }

            } else if (isSpeaking) {
                // Silence detected while speaking
                long sentenceDuration = currentTime - sentenceStartTime;
                long silenceDuration = currentTime - lastVoiceTime;

                if (sentenceDuration >= minSentenceDurationMs && silenceDuration > silenceTimeoutMs) {
                    // Sentence complete - save file
                    saveSentenceFile(sentenceBuffer.toByteArray());
                    sentenceBuffer.reset();
                    isSpeaking = false;

                    // Return to listening state
                    mainHandler.post(() -> callback.onStateChanged(
                            VoiceState.LISTENING,
                            "Đang lắng nghe..."
                    ));
                } else {
                    // Continue recording (still within minimum duration or brief pause)
                    sentenceBuffer.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private int calculateAmplitude(byte[] buffer, int bytesRead) {
        int max = 0;
        for (int i = 0; i < bytesRead - 1; i += 2) {
            int value = (buffer[i + 1] << 8) | (buffer[i] & 0xFF);
            max = Math.max(max, Math.abs(value));
        }
        return max;
    }

    private void saveSentenceFile(byte[] pcmData) {
        mainHandler.post(() -> callback.onStateChanged(VoiceState.SAVING, "Đang lưu file..."));

        // Save in background thread
        new Thread(() -> {
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                String fileName = "voice_" + timeStamp + ".wav";

                // Use app's external files directory (no permission required)
                File filesDir = context.getExternalFilesDir(null);
                if (filesDir == null) {
                    filesDir = context.getFilesDir(); // Fallback to internal storage
                }

                File wavFile = new File(filesDir, fileName);
                FileOutputStream fos = new FileOutputStream(wavFile);

                // Write WAV header and PCM data
                writeWavHeader(fos, pcmData.length, sampleRate, 1, 16);
                fos.write(pcmData);
                fos.close();

                Log.d(TAG, "Audio file saved: " + wavFile.getAbsolutePath());

                // Notify callback on main thread
                mainHandler.post(() -> {
                    callback.onAudioFileSaved(fileName, wavFile);
                    callback.onStateChanged(VoiceState.LISTENING, "Đã lưu: " + fileName);
                });

            } catch (IOException e) {
                Log.e(TAG, "Error saving audio file", e);
                mainHandler.post(() -> {
                    callback.onError("Lỗi lưu file: " + e.getMessage());
                    callback.onStateChanged(VoiceState.LISTENING, "Lỗi lưu file, tiếp tục lắng nghe...");
                });
            }
        }).start();
    }

    private void writeWavHeader(OutputStream out, int totalAudioLen, int sampleRate,
                                int channels, int bitsPerSample) throws IOException {
        long totalDataLen = totalAudioLen + 36;
        long byteRate = sampleRate * channels * bitsPerSample / 8;

        byte[] header = new byte[44];

        // RIFF header
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        // WAVE header
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        // fmt chunk
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;

        // Sample rate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        // Byte rate
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        // Block align and bits per sample
        header[32] = (byte) (channels * bitsPerSample / 8);
        header[33] = 0;
        header[34] = (byte) bitsPerSample; header[35] = 0;

        // data chunk
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void cleanup() {
        try {
            if (recorder != null) {
                if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop();
                }
                recorder.release();
                recorder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    // Public getters
    public boolean isRecording() {
        return isRecording;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }
}