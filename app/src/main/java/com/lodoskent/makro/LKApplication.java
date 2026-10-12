package com.lodoskent.makro;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Records fatal Android process crashes so device-specific failures can be diagnosed. */
public final class LKApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler previous =
            Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StringWriter buffer = new StringWriter();
                error.printStackTrace(new PrintWriter(buffer));
                String report = buffer.toString();
                if (report.length() > 2200) report = report.substring(0, 2200);
                SharedPreferences prefs = getSharedPreferences(
                    MacroAccessibilityService.PREFS, MODE_PRIVATE);
                prefs.edit().putString("last_service_error", report).commit();
                Log.e("LKMacro", "Fatal error saved for diagnostics", error);
            } catch (Throwable ignored) {
                // Never hide the original Android crash.
            }
            if (previous != null) previous.uncaughtException(thread, error);
            else android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
}
