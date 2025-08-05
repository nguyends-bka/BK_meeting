package com.example.bk_meeting;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

public class DisplayManagerUtil {

    private static final String TAG = "DisplayManagerUtil";

    public static Display[] getAvailableDisplays(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        return displayManager.getDisplays();
    }

    public static boolean isDualScreenAvailable(Context context) {
        return getAvailableDisplays(context).length >= 2;
    }

    public static void logDisplayInfo(Context context) {
        Display[] displays = getAvailableDisplays(context);
        Log.d(TAG, "Total displays: " + displays.length);

        for (int index = 0; index < displays.length; index++) {
            Display display = displays[index];
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            Log.d(TAG, "Display " + index + ":");
            Log.d(TAG, "  ID: " + display.getDisplayId());
            Log.d(TAG, "  Name: " + (display.getName() != null ? display.getName() : "Unknown"));
            Log.d(TAG, "  Size: " + displayMetrics.widthPixels + " x " + displayMetrics.heightPixels);
            Log.d(TAG, "  DPI: " + displayMetrics.densityDpi);
            Log.d(TAG, "  State: " + display.getState());
            Log.d(TAG, "  Valid: " + display.isValid());
        }
    }

    public static boolean launchOnSpecificDisplay(Context context, Intent intent, int displayId, boolean showToast) {
        try {
            // Check API level trước khi sử dụng launchDisplayId
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(displayId);
                context.startActivity(intent, options.toBundle());
            } else {
                // Fallback cho API < 26
                context.startActivity(intent);
                Log.w(TAG, "launchDisplayId not supported on API < 26, using default display");
            }

            if (showToast) {
                String message;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    message = "Launched on display " + displayId;
                } else {
                    message = "Launched on default display (API < 26)";
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "Successfully launched intent");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch intent: " + e.getMessage());
            if (showToast) {
                Toast.makeText(context, "Failed to launch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    public static boolean launchOnSpecificDisplay(Context context, Intent intent, int displayId) {
        return launchOnSpecificDisplay(context, intent, displayId, true);
    }

    public static boolean launchOnSecondaryDisplay(Context context, Intent intent) {
        Display[] displays = getAvailableDisplays(context);

        if (displays.length >= 2) {
            return launchOnSpecificDisplay(context, intent, displays[1].getDisplayId());
        } else {
            Log.w(TAG, "No secondary display available");
            Toast.makeText(context, "No secondary display available", Toast.LENGTH_SHORT).show();
            // Launch on primary display as fallback
            context.startActivity(intent);
            return false;
        }
    }

    public static Display getPrimaryDisplay(Context context) {
        Display[] displays = getAvailableDisplays(context);
        return displays.length > 0 ? displays[0] : null;
    }

    public static Display getSecondaryDisplay(Context context) {
        Display[] displays = getAvailableDisplays(context);
        return displays.length >= 2 ? displays[1] : null;
    }
}