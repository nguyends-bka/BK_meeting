package com.example.bk_meeting;

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.text.format.DateFormat;
import android.os.Handler;
import android.os.Looper;
import java.util.*;

public class SecondScreenActivity extends AppCompatActivity {

    private WebView webView;
    private Button btnBack;
    private Button btnRefresh;
    private Button btnHome;
    private TextView tvStatus;

    // Note taking functionality
    private EditText etNoteTitle;
    private EditText etNoteContent;
    private Button btnSaveNote;
    private Button btnClearNote;
    private Button btnNewNote;
    private ListView lvNotesList;
    private TextView tvCurrentTime;
    private TextView tvNoteCount;

    private List<String> notesList = new ArrayList<>();
    private ArrayAdapter<String> notesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use the XML layout instead of creating programmatically
        setContentView(R.layout.activity_second_screen);

        // Initialize views from XML
        initializeViewsFromXML();

        setupWebView();
        setupNoteTaking();
        setupBackPressed();

        // Load Facebook ngay lập tức
        loadFacebook();

        // Hiển thị thông báo
        Toast.makeText(this, "🌐 Screen 2: Loading Facebook...", Toast.LENGTH_SHORT).show();
    }

    private void initializeViewsFromXML() {
        // Find views from the XML layout
        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        btnClearNote = findViewById(R.id.btnClearNote);
        btnNewNote = findViewById(R.id.btnNewNote);
        lvNotesList = findViewById(R.id.lvNotesList);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvNoteCount = findViewById(R.id.tvNoteCount);

        // Create WebView and toolbar programmatically since they're not in XML
        createWebViewAndToolbar();
    }

    private void createWebViewAndToolbar() {
        // Get the main layout from XML
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main);

        // Create header with controls
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(Color.parseColor("#1877F2")); // Facebook blue
        headerLayout.setPadding(16, 16, 16, 16);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Status text
        tvStatus = new TextView(this);
        tvStatus.setText("📱 Screen 2: Facebook");
        tvStatus.setTextSize(16f);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        // Back button
        btnBack = new Button(this);
        btnBack.setText("←");
        btnBack.setBackgroundColor(Color.parseColor("#166FE5"));
        btnBack.setTextColor(Color.WHITE);

        // Refresh button
        btnRefresh = new Button(this);
        btnRefresh.setText("↻");
        btnRefresh.setBackgroundColor(Color.parseColor("#166FE5"));
        btnRefresh.setTextColor(Color.WHITE);

        // Home button
        btnHome = new Button(this);
        btnHome.setText("🏠");
        btnHome.setBackgroundColor(Color.parseColor("#166FE5"));
        btnHome.setTextColor(Color.WHITE);

        // Add to header
        headerLayout.addView(tvStatus);
        headerLayout.addView(btnBack);
        headerLayout.addView(btnRefresh);
        headerLayout.addView(btnHome);

        // Create WebView
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400 // Fixed height for WebView
        ));

        // Add header and WebView to top of the main layout
        mainLayout.addView(headerLayout, 0);
        mainLayout.addView(webView, 1);
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

        // Bật mixed content
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Bật zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // User agent cho desktop Facebook
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // Bật hardware acceleration
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // Load images
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);

        // Cache settings
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // WebViewClient để handle navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep Facebook navigation within WebView
                if (url.contains("facebook.com") ||
                        url.contains("fb.com") ||
                        url.contains("fbcdn.net")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                tvStatus.setText("🔄 Loading Facebook...");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                tvStatus.setText("📱 Screen 2: Facebook");
                Toast.makeText(SecondScreenActivity.this, "✅ Facebook loaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                tvStatus.setText("❌ Load error");
                Toast.makeText(SecondScreenActivity.this, "Error: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        // Setup button listeners
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        });

        btnRefresh.setOnClickListener(v -> webView.reload());

        btnHome.setOnClickListener(v -> loadFacebook());
    }

    private void setupNoteTaking() {
        // Initialize notes adapter
        notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notesList);
        lvNotesList.setAdapter(notesAdapter);

        // Save note button
        btnSaveNote.setOnClickListener(v -> saveNote());

        // Clear note button
        btnClearNote.setOnClickListener(v -> clearNote());

        // New note button
        btnNewNote.setOnClickListener(v -> newNote());

        // Update time initially
        updateCurrentTime();

        // Update time periodically
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                handler.postDelayed(this, 60000); // Update every minute
            }
        };
        handler.post(updateTimeRunnable);
    }

    private void saveNote() {
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please enter note content", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = DateFormat.format("HH:mm", new Date()).toString();
        String noteText;
        if (!title.isEmpty()) {
            noteText = "[" + timestamp + "] " + title + ": " + content;
        } else {
            noteText = "[" + timestamp + "] " + content;
        }

        notesList.add(noteText);
        notesAdapter.notifyDataSetChanged();

        // Update count
        tvNoteCount.setText("📋 Total: " + notesList.size() + " notes");

        // Clear inputs
        etNoteTitle.setText("");
        etNoteContent.setText("");

        Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
    }

    private void clearNote() {
        etNoteTitle.setText("");
        etNoteContent.setText("");
        Toast.makeText(this, "Note cleared", Toast.LENGTH_SHORT).show();
    }

    private void newNote() {
        clearNote();
        etNoteTitle.requestFocus();
    }

    private void updateCurrentTime() {
        String currentTime = DateFormat.format("dd/MM/yyyy HH:mm", new Date()).toString();
        tvCurrentTime.setText("📅 " + currentTime);
    }

    private void setupBackPressed() {
        // Sử dụng OnBackPressedDispatcher thay vì onBackPressed()
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void loadFacebook() {
        if (tvStatus != null) {
            tvStatus.setText("🔄 Connecting to Facebook...");
        }

        try {
            // Load Facebook mobile site for better compatibility
            webView.loadUrl("https://m.facebook.com/");

            // Fallback to desktop if mobile fails
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (webView.getProgress() < 50) {
                        webView.loadUrl("https://www.facebook.com/");
                    }
                }
            }, 5000);

        } catch (Exception e) {
            Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (tvStatus != null) {
                tvStatus.setText("❌ Cannot connect");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}