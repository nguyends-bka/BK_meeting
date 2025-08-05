package com.example.bk_meeting;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;
import android.content.pm.PackageManager;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.List;

// Voice Recording imports
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Locale;

public class FirstScreenActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button btnToggleCamera;
    private Button btnToggleMicrophone;
    private Button btnJoinMeeting;
    private TextView tvRecordingStatus;
    private LinearLayout sentenceListLayout;

    private boolean isCameraRunning = false;
    private boolean isMicrophoneRunning = false;

    // Voice Recording variables - nguyên bản từ MainActivity
    private AudioRecord recorder;
    private boolean isRecording = false;
    private boolean isSpeaking = false;
    private Thread recordingThread;

    private final int minSentenceDurationMs = 15_000;
    private final int sampleRate = 44100;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private final int silenceThreshold = 6000; // độ lớn tối thiểu (amplitude)
    private final int silenceTimeoutMs = 1000; // thời gian im lặng để kết thúc câu(ms)

    private static final int PERMISSION_REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_screen);

        initializeViews();
        checkPermissions();
        setupButtonListeners();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        btnToggleCamera = findViewById(R.id.btnToggleCamera);
        btnToggleMicrophone = findViewById(R.id.btnToggleMicrophone);
        btnJoinMeeting = findViewById(R.id.btnJoinMeeting);
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus);
        sentenceListLayout = findViewById(R.id.sentenceListLayout);

        // Set initial microphone status
        tvRecordingStatus.setText("🔴 Microphone tắt");
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissions.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private void setupButtonListeners() {
        btnToggleCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (isCameraRunning) {
                    stopCamera();
                } else {
                    startCamera();
                }
            } else {
                Toast.makeText(this, "Cần quyền camera để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });

        // Microphone button with integrated Voice Recording
        btnToggleMicrophone.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (isMicrophoneRunning) {
                    stopMicrophone();
                } else {
                    startMicrophone();
                }
            } else {
                Toast.makeText(this, "Cần quyền microphone để tiếp tục", Toast.LENGTH_SHORT).show();
            }
        });

        // Join Meeting Button
        btnJoinMeeting.setOnClickListener(v -> {
            if (hasRequiredPermissions()) {
                Intent intent = new Intent(this, WebViewActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Vui lòng cấp quyền Camera và Microphone trước khi tham gia meeting", Toast.LENGTH_LONG).show();
                checkPermissions();
            }
        });
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void startMicrophone() {
        try {
            startListening();
            isMicrophoneRunning = true;
            btnToggleMicrophone.setText("Tắt Micro");
            Toast.makeText(this, "Microphone đã bật - Bắt đầu voice recording", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi microphone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMicrophone() {
        try {
            isRecording = false;
            if (recordingThread != null) {
                recordingThread.interrupt();
            }
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }

            isMicrophoneRunning = false;
            btnToggleMicrophone.setText("Bật Micro");
            tvRecordingStatus.setText("🔴 Microphone tắt");
            Toast.makeText(this, "Microphone đã tắt", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Reset state even on error
            isMicrophoneRunning = false;
            btnToggleMicrophone.setText("Bật Micro");
            tvRecordingStatus.setText("🔴 Microphone tắt");
            Toast.makeText(this, "Microphone đã tắt", Toast.LENGTH_SHORT).show();
        }
    }

    // Voice Recording Methods - nguyên bản từ MainActivity
    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bạn cần cấp quyền ghi âm", Toast.LENGTH_SHORT).show();
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, bufferSize);

        recorder.startRecording();
        isRecording = true;

        runOnUiThread(() -> tvRecordingStatus.setText("🔴 Đang lắng nghe..."));

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            ByteArrayOutputStream sentenceBuffer = new ByteArrayOutputStream();
            long lastVoiceTime = System.currentTimeMillis();
            long sentenceStartTime = 0;

            while (isRecording) {
                int read = recorder.read(buffer, 0, buffer.length);
                if (read <= 0) continue;

                int amplitude = calculateAmplitude(buffer, read);
                long now = System.currentTimeMillis();

                if (amplitude > silenceThreshold) {
                    if (!isSpeaking) {
                        isSpeaking = true;
                        sentenceBuffer.reset();
                        sentenceStartTime = now;
                        runOnUiThread(() -> tvRecordingStatus.setText("🎙️ Đang phát hiện giọng nói..."));
                    }
                    sentenceBuffer.write(buffer, 0, read);
                    lastVoiceTime = now;

                    // Update status during recording
                    long currentDuration = now - sentenceStartTime;
                    if (currentDuration >= minSentenceDurationMs) {
                        runOnUiThread(() -> tvRecordingStatus.setText("🎙️ Đang ghi âm... (" + (currentDuration / 1000) + "s)"));
                    }
                } else if (isSpeaking) {
                    long sentenceDuration = now - sentenceStartTime;

                    if (sentenceDuration >= minSentenceDurationMs && now - lastVoiceTime > silenceTimeoutMs) {
                        // đủ 15s và người đã ngừng nói → lưu file
                        runOnUiThread(() -> tvRecordingStatus.setText("💾 Đang lưu file..."));
                        saveWavFile(sentenceBuffer.toByteArray());
                        sentenceBuffer.reset();
                        isSpeaking = false;
                        runOnUiThread(() -> tvRecordingStatus.setText("🔴 Đang lắng nghe..."));
                    } else {
                        // vẫn đang trong 15s hoặc người tạm dừng → tiếp tục ghi
                        sentenceBuffer.write(buffer, 0, read);
                    }
                }
            }

            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
        });

        recordingThread.start();
    }

    private int calculateAmplitude(byte[] buffer, int read) {
        int max = 0;
        for (int i = 0; i < read - 1; i += 2) {
            int value = (buffer[i + 1] << 8) | (buffer[i] & 0xFF);
            max = Math.max(max, Math.abs(value));
        }
        return max;
    }

    private void saveWavFile(byte[] pcmData) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File wavFile = new File(getExternalFilesDir(null), "voice_" + timeStamp + ".wav");
            FileOutputStream fos = new FileOutputStream(wavFile);

            String displayName = wavFile.getName();

            runOnUiThread(() -> {
                TextView tv = new TextView(this);
                tv.setText("🟢 " + displayName);
                tv.setTextSize(14f);
                tv.setPadding(8, 4, 8, 4);
                sentenceListLayout.addView(tv);

                Toast.makeText(this, "Đã lưu: " + displayName, Toast.LENGTH_SHORT).show();
            });

            writeWavHeader(fos, pcmData.length, sampleRate, 1, 16);
            fos.write(pcmData);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void writeWavHeader(OutputStream out, int totalAudioLen, int sampleRate, int channels, int bitsPerSample) throws IOException {
        long totalDataLen = totalAudioLen + 36;
        long byteRate = sampleRate * channels * bitsPerSample / 8;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * bitsPerSample / 8);
        header[33] = 0;
        header[34] = (byte) bitsPerSample; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    // Tạo preview
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    // Chọn camera (trước)
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                    // Ngắt kết nối các use case trước khi gắn lại
                    cameraProvider.unbindAll();

                    // Gắn các use case vào camera
                    cameraProvider.bindToLifecycle(
                            FirstScreenActivity.this, cameraSelector, preview
                    );

                    isCameraRunning = true;
                    btnToggleCamera.setText("Tắt Camera");
                    Toast.makeText(FirstScreenActivity.this, "Camera đã mở", Toast.LENGTH_SHORT).show();

                } catch (ExecutionException | InterruptedException exc) {
                    Toast.makeText(FirstScreenActivity.this, "Lỗi khởi tạo camera: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                    isCameraRunning = false;
                    btnToggleCamera.setText("Mở Camera");
                    Toast.makeText(FirstScreenActivity.this, "Camera đã tắt", Toast.LENGTH_SHORT).show();
                } catch (ExecutionException | InterruptedException exc) {
                    Toast.makeText(FirstScreenActivity.this, "Lỗi tắt camera: " + exc.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Toast.makeText(this, "Tất cả quyền đã được cấp", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Một số quyền chưa được cấp. App có thể không hoạt động đầy đủ.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup voice recording
        isRecording = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}