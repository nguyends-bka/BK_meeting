package com.example.bk_meeting;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import androidx.activity.OnBackPressedCallback;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private Button btnBack;
    private Button btnRefresh;
    private Button btnTestMedia;
    private Button btnEnableCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.webView);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnTestMedia = findViewById(R.id.btnTestMedia);
        btnEnableCamera = findViewById(R.id.btnEnableCamera);

        setupWebView();

        // Load URL meeting
        String meetingUrl = "https://meet.xbot.vn/HonestReliefsDiscourageRight";
        webView.loadUrl(meetingUrl);

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút refresh
        btnRefresh.setOnClickListener(v -> webView.reload());

        // Nút test media permissions
        btnTestMedia.setOnClickListener(v -> testMediaPermissions());

        // Nút enable camera trong meeting
        btnEnableCamera.setOnClickListener(v -> enableCameraInMeeting());

        // Xử lý nút back với API mới
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Bật JavaScript
        webSettings.setJavaScriptEnabled(true);

        // Bật DOM storage
        webSettings.setDomStorageEnabled(true);

        // Bật database
        webSettings.setDatabaseEnabled(true);

        // Bật file access
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Bật mixed content (HTTP trong HTTPS)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Bật zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // User agent chuẩn cho desktop để tránh mobile restrictions
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // Bật media playback
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Bật hardware acceleration
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // Cài đặt WebViewClient để giữ navigation trong app
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url != null ? url : "");
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject JavaScript để request permissions
                webView.evaluateJavascript(
                        "(function() {" +
                                "    navigator.mediaDevices.getUserMedia({video: true, audio: true})" +
                                "        .then(function(stream) {" +
                                "            console.log('Camera and microphone access granted');" +
                                "        })" +
                                "        .catch(function(err) {" +
                                "            console.log('Error accessing media devices:', err);" +
                                "        });" +
                                "})();", null);
                Toast.makeText(WebViewActivity.this, "Đã tải xong trang meeting", Toast.LENGTH_SHORT).show();
            }
        });

        // Cài đặt WebChromeClient để xử lý camera/microphone permissions
        webView.setWebChromeClient(new WebChromeClient() {
            private boolean permissionGranted = false;

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Log để debug
                for (String resource : request.getResources()) {
                    System.out.println("Requesting permission for: " + resource);
                }

                // Tự động cấp quyền camera và microphone cho website
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            request.grant(request.getResources());
                            permissionGranted = true;
                            Toast.makeText(WebViewActivity.this,
                                    "Đã cấp quyền: " + String.join(", ", request.getResources()),
                                    Toast.LENGTH_SHORT).show();

                        } catch (Exception e) {
                            Toast.makeText(WebViewActivity.this,
                                    "Lỗi cấp quyền: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                super.onPermissionRequestCanceled(request);
                Toast.makeText(WebViewActivity.this,
                        "Quyền bị từ chối - Meeting sẽ không hoạt động đầy đủ",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // Chỉ chạy một lần khi page load xong
                if (newProgress == 100 && !permissionGranted) {
                    // Chờ 5 giây để page ổn định và tự động bật camera
                    webView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript(
                                    "(function() {" +
                                            "    console.log('Page loaded, auto-enabling camera...');" +
                                            "    " +
                                            "    // Tự động bật camera và microphone" +
                                            "    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {" +
                                            "        navigator.mediaDevices.getUserMedia({" +
                                            "            video: { " +
                                            "                width: { ideal: 640 }, " +
                                            "                height: { ideal: 480 }," +
                                            "                facingMode: 'user'" +
                                            "            }," +
                                            "            audio: { " +
                                            "                echoCancellation: true," +
                                            "                noiseSuppression: true," +
                                            "                autoGainControl: true" +
                                            "            }" +
                                            "        })" +
                                            "        .then(function(stream) {" +
                                            "            console.log('Auto-enabled camera/microphone successfully');" +
                                            "            " +
                                            "            // Tìm và click nút camera nếu có" +
                                            "            setTimeout(function() {" +
                                            "                // Thử các selector khác nhau cho nút camera" +
                                            "                var cameraSelectors = [" +
                                            "                    '[data-testid=\"camera-button\"]'," +
                                            "                    '[aria-label*=\"camera\"]'," +
                                            "                    '[aria-label*=\"Camera\"]'," +
                                            "                    'button[title*=\"camera\"]'," +
                                            "                    'button[title*=\"Camera\"]'," +
                                            "                    '.camera-button'," +
                                            "                    '#camera-btn'," +
                                            "                    'button:contains(\"Camera\")'," +
                                            "                    '[class*=\"camera\"]'" +
                                            "                ];" +
                                            "                " +
                                            "                for (var i = 0; i < cameraSelectors.length; i++) {" +
                                            "                    var cameraBtn = document.querySelector(cameraSelectors[i]);" +
                                            "                    if (cameraBtn) {" +
                                            "                        console.log('Found camera button with selector:', cameraSelectors[i]);" +
                                            "                        cameraBtn.click();" +
                                            "                        break;" +
                                            "                    }" +
                                            "                }" +
                                            "                " +
                                            "                // Tương tự cho microphone" +
                                            "                var micSelectors = [" +
                                            "                    '[data-testid=\"microphone-button\"]'," +
                                            "                    '[data-testid=\"mic-button\"]'," +
                                            "                    '[aria-label*=\"microphone\"]'," +
                                            "                    '[aria-label*=\"Microphone\"]'," +
                                            "                    'button[title*=\"microphone\"]'," +
                                            "                    'button[title*=\"Microphone\"]'," +
                                            "                    '.microphone-button'," +
                                            "                    '#microphone-btn'," +
                                            "                    '[class*=\"microphone\"]'" +
                                            "                ];" +
                                            "                " +
                                            "                for (var j = 0; j < micSelectors.length; j++) {" +
                                            "                    var micBtn = document.querySelector(micSelectors[j]);" +
                                            "                    if (micBtn) {" +
                                            "                        console.log('Found microphone button with selector:', micSelectors[j]);" +
                                            "                        micBtn.click();" +
                                            "                        break;" +
                                            "                    }" +
                                            "                }" +
                                            "            }, 2000);" +
                                            "        })" +
                                            "        .catch(function(err) {" +
                                            "            console.error('Error auto-enabling camera/microphone:', err);" +
                                            "        });" +
                                            "    } else {" +
                                            "        console.error('MediaDevices API not available');" +
                                            "    }" +
                                            "})();", null);
                        }
                    }, 5000);
                }
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                if (consoleMessage != null) {
                    System.out.println("WebView Console: " + consoleMessage.message());
                }
                return super.onConsoleMessage(consoleMessage);
            }
        });
    }

    private void testMediaPermissions() {
        webView.evaluateJavascript(
                "(function() {" +
                        "    console.log('=== Testing Media Permissions ===');" +
                        "    " +
                        "    if (!navigator.mediaDevices) {" +
                        "        alert('MediaDevices API không được hỗ trợ');" +
                        "        return;" +
                        "    }" +
                        "    " +
                        "    // Test getUserMedia" +
                        "    navigator.mediaDevices.getUserMedia({" +
                        "        video: { " +
                        "            width: { ideal: 640 }, " +
                        "            height: { ideal: 480 }," +
                        "            facingMode: 'user'" +
                        "        }," +
                        "        audio: { " +
                        "            echoCancellation: true," +
                        "            noiseSuppression: true," +
                        "            autoGainControl: true" +
                        "        }" +
                        "    })" +
                        "    .then(function(stream) {" +
                        "        alert('✅ Camera/Microphone hoạt động OK!\\\\nVideo tracks: ' + stream.getVideoTracks().length + '\\\\nAudio tracks: ' + stream.getAudioTracks().length);" +
                        "        console.log('Stream active:', stream.active);" +
                        "        console.log('Video tracks:', stream.getVideoTracks());" +
                        "        console.log('Audio tracks:', stream.getAudioTracks());" +
                        "        " +
                        "        // Stop stream để không chiếm dụng" +
                        "        stream.getTracks().forEach(function(track) {" +
                        "            console.log('Stopping track:', track.kind, track.label);" +
                        "            track.stop();" +
                        "        });" +
                        "    })" +
                        "    .catch(function(error) {" +
                        "        alert('❌ Lỗi Camera/Microphone:\\\\n' + error.name + ': ' + error.message);" +
                        "        console.error('getUserMedia error:', error);" +
                        "    });" +
                        "    " +
                        "    // Test permissions API nếu có" +
                        "    if (navigator.permissions) {" +
                        "        Promise.all([" +
                        "            navigator.permissions.query({name: 'camera'})," +
                        "            navigator.permissions.query({name: 'microphone'})" +
                        "        ]).then(function(results) {" +
                        "            console.log('Camera permission:', results[0].state);" +
                        "            console.log('Microphone permission:', results[1].state);" +
                        "        }).catch(function(err) {" +
                        "            console.log('Permission query error:', err);" +
                        "        });" +
                        "    }" +
                        "})();", null);
    }

    private void enableCameraInMeeting() {
        webView.evaluateJavascript(
                "(function() {" +
                        "    console.log('=== Manually Enabling Camera in Meeting ===');" +
                        "    " +
                        "    // Tìm tất cả các nút có thể là camera/microphone" +
                        "    var allButtons = document.querySelectorAll('button, [role=\"button\"], .button');" +
                        "    console.log('Found', allButtons.length, 'buttons total');" +
                        "    " +
                        "    // Tìm nút camera" +
                        "    var cameraButton = null;" +
                        "    var micButton = null;" +
                        "    " +
                        "    for (var i = 0; i < allButtons.length; i++) {" +
                        "        var btn = allButtons[i];" +
                        "        var text = btn.textContent || btn.innerText || '';" +
                        "        var ariaLabel = btn.getAttribute('aria-label') || '';" +
                        "        var title = btn.getAttribute('title') || '';" +
                        "        var className = btn.className || '';" +
                        "        " +
                        "        var fullText = (text + ' ' + ariaLabel + ' ' + title + ' ' + className).toLowerCase();" +
                        "        " +
                        "        // Tìm nút camera" +
                        "        if (fullText.includes('camera') || fullText.includes('video') || fullText.includes('cam')) {" +
                        "            cameraButton = btn;" +
                        "            console.log('Found camera button:', btn);" +
                        "        }" +
                        "        " +
                        "        // Tìm nút microphone" +
                        "        if (fullText.includes('microphone') || fullText.includes('mic') || fullText.includes('audio')) {" +
                        "            micButton = btn;" +
                        "            console.log('Found microphone button:', btn);" +
                        "        }" +
                        "    }" +
                        "    " +
                        "    // Click camera button" +
                        "    if (cameraButton) {" +
                        "        console.log('Clicking camera button');" +
                        "        cameraButton.click();" +
                        "        alert('Đã nhấn nút camera');" +
                        "    } else {" +
                        "        console.log('Camera button not found');" +
                        "        alert('Không tìm thấy nút camera');" +
                        "    }" +
                        "    " +
                        "    // Click microphone button" +
                        "    setTimeout(function() {" +
                        "        if (micButton) {" +
                        "            console.log('Clicking microphone button');" +
                        "            micButton.click();" +
                        "            alert('Đã nhấn nút microphone');" +
                        "        } else {" +
                        "            console.log('Microphone button not found');" +
                        "            alert('Không tìm thấy nút microphone');" +
                        "        }" +
                        "    }, 500);" +
                        "    " +
                        "    // Thử force enable với getUserMedia" +
                        "    setTimeout(function() {" +
                        "        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {" +
                        "            navigator.mediaDevices.getUserMedia({" +
                        "                video: { facingMode: 'user' }," +
                        "                audio: true" +
                        "            })" +
                        "            .then(function(stream) {" +
                        "                console.log('Successfully got media stream');" +
                        "                " +
                        "                // Tìm video element và gán stream" +
                        "                var videoElements = document.querySelectorAll('video');" +
                        "                if (videoElements.length > 0) {" +
                        "                    videoElements[0].srcObject = stream;" +
                        "                    videoElements[0].play();" +
                        "                    console.log('Assigned stream to video element');" +
                        "                    alert('Đã gán stream vào video element');" +
                        "                }" +
                        "            })" +
                        "            .catch(function(err) {" +
                        "                console.error('Error getting media stream:', err);" +
                        "                alert('Lỗi lấy media stream: ' + err.message);" +
                        "            });" +
                        "        }" +
                        "    }, 1000);" +
                        "})();", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }
}