package ca.dnamobile.javalauncher.logs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;

import ca.dnamobile.javalauncher.feature.log.Logging;

/** Manages DroidBridge launcher-side diagnostic log files. */
public final class LauncherLogManager {
    private static final String TAG = "LauncherLogManager";
    private static final String PREFS = "launcher_preferences";
    private static final String KEY_ENABLED = "launcher_diagnostic_logs";
    private static final String KEY_KEEP_HISTORY = "launcher_keep_log_history";

    private LauncherLogManager() {}

    public static boolean isEnabled(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    /** Whether old log files should be kept across sessions (vs. cleared on startup). */
    public static boolean isKeepLogHistoryEnabled(@NonNull Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_KEEP_HISTORY, false);
    }

    /** Enable or disable keeping log history across sessions. */
    public static void setKeepLogHistoryEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_KEEP_HISTORY, enabled).apply();
    }

    @NonNull
    public static File getLogFile(@NonNull Context context) {
        return new File(context.getFilesDir(), "droidbridge_launcher.log");
    }

    /** Delete old log files to reclaim storage. */
    public static void clearLogs(@NonNull Context context) {
        File f = getLogFile(context);
        if (f.exists()) f.delete();
    }

    /**
     * Share the latest log file via the Android share sheet.
     * Does nothing if there is no log file to share.
     */
    public static void shareLatestLog(@NonNull Context context) {
        File logFile = getLogFile(context);
        if (!logFile.exists() || logFile.length() == 0) {
            Logging.w(TAG, "No log file to share");
            return;
        }
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".storage_provider",
                    logFile
            );
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_STREAM, fileUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(share, "Share log file"));
        } catch (Throwable throwable) {
            Logging.e(TAG, "Failed to share log file", throwable);
        }
    }
}
