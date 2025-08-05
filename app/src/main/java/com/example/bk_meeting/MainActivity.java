package com.example.bk_meeting;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.content.Context;
import android.widget.Toast;
import android.app.ActivityOptions;
import android.util.DisplayMetrics;

public class MainActivity extends AppCompatActivity {

    private DisplayManager displayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        // Delay một chút để hiển thị splash screen
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkDisplaysAndLaunch();
            }
        }, 1500);
    }

    private void checkDisplaysAndLaunch() {
        Display[] displays = displayManager.getDisplays();

        if (displays.length >= 2) {
            // Có nhiều màn hình - launch dual screen
            launchDualScreenMode(displays);
        } else {
            // Chỉ có 1 màn hình - launch single screen mode
            launchSingleScreenMode();
        }
    }

    private void launchDualScreenMode(Display[] displays) {
        Toast.makeText(this, "🖥️ Dual Screen Mode - " + displays.length + " displays detected", Toast.LENGTH_LONG).show();

        // Display 0: Màn hình chính - Meeting App (FirstScreenActivity)
        Display primaryDisplay = displays[0];

        // Display 1: Màn hình phụ - Facebook (SecondScreenActivity)
        Display secondaryDisplay = displays[1];

        // Launch FirstScreenActivity trên màn hình chính
        Intent meetingIntent = new Intent(this, FirstScreenActivity.class);
        meetingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ActivityOptions primaryOptions = ActivityOptions.makeBasic();
            primaryOptions.setLaunchDisplayId(primaryDisplay.getDisplayId());
            startActivity(meetingIntent, primaryOptions.toBundle());
        } else {
            startActivity(meetingIntent);
        }

        // Launch SecondScreenActivity trên màn hình phụ
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent facebookIntent = new Intent(MainActivity.this, SecondScreenActivity.class);
                facebookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ActivityOptions secondaryOptions = ActivityOptions.makeBasic();
                    secondaryOptions.setLaunchDisplayId(secondaryDisplay.getDisplayId());
                    startActivity(facebookIntent, secondaryOptions.toBundle());
                } else {
                    startActivity(facebookIntent);
                }

                Toast.makeText(MainActivity.this, "✅ Launched on both screens!", Toast.LENGTH_SHORT).show();
            }
        }, 500);

        // Đóng MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1000);
    }

    private void launchSingleScreenMode() {
        Toast.makeText(this, "📱 Single Screen Mode", Toast.LENGTH_SHORT).show();

        // Launch như cũ với FLAG_ACTIVITY_LAUNCH_ADJACENT để simulate dual screen
        Intent facebookIntent = new Intent(this, SecondScreenActivity.class);
        facebookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        startActivity(facebookIntent);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent meetingIntent = new Intent(MainActivity.this, FirstScreenActivity.class);
                startActivity(meetingIntent);
                finish();
            }
        }, 300);
    }

    private void logDisplayInfo() {
        Display[] displays = displayManager.getDisplays();
        for (int index = 0; index < displays.length; index++) {
            Display display = displays[index];
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            System.out.println("Display " + index + ":");
            System.out.println("  Display ID: " + display.getDisplayId());
            System.out.println("  Name: " + (display.getName() != null ? display.getName() : "Unknown"));
            System.out.println("  Size: " + displayMetrics.widthPixels + " x " + displayMetrics.heightPixels);
            System.out.println("  DPI: " + displayMetrics.densityDpi);
            System.out.println("  State: " + display.getState());
        }
    }
}