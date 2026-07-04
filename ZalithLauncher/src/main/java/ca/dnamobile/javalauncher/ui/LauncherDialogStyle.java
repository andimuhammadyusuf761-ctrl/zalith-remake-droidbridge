package ca.dnamobile.javalauncher.ui;

import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Provides DroidBridge-styled Material3 dialogs and shared color constants.
 */
public final class LauncherDialogStyle {

    // -----------------------------------------------------------------------
    // Shared color constants
    // -----------------------------------------------------------------------

    public static final int COLOR_DIALOG_BG       = Color.rgb(12,  10,  8);
    public static final int COLOR_CARD_BG         = Color.rgb(24,  24,  25);
    public static final int COLOR_CARD_BG_PRESSED = Color.rgb(35,  30,  24);
    public static final int COLOR_CARD_STROKE     = Color.rgb(66,  48,  26);
    public static final int COLOR_TEXT_PRIMARY    = Color.rgb(241, 238, 232);
    public static final int COLOR_TEXT_SECONDARY  = Color.rgb(205, 198, 188);
    public static final int COLOR_TEXT_MUTED      = Color.rgb(157, 147, 132);
    public static final int COLOR_ACCENT          = Color.rgb(145, 91,  14);

    private LauncherDialogStyle() {}

    // -----------------------------------------------------------------------
    // Dialog chrome helpers
    // -----------------------------------------------------------------------

    /** Build a Material3 styled alert dialog builder. */
    @NonNull
    public static MaterialAlertDialogBuilder builder(@NonNull Context context) {
        return new MaterialAlertDialogBuilder(context);
    }

    /**
     * Apply DroidBridge chrome styling to an already-created {@link AlertDialog}.
     * Call this inside {@code setOnShowListener} so the window is available.
     */
    public static void styleDialogChrome(@NonNull Context context, @Nullable AlertDialog dialog) {
        if (dialog == null) return;
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawableResource(android.R.color.transparent);
    }

    public static void styleDialogChrome(@NonNull Context context,
                                         @Nullable androidx.appcompat.app.AlertDialog dialog) {
        if (dialog == null) return;
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawableResource(android.R.color.transparent);
    }

    /**
     * Create a pre-styled dialog root {@link android.widget.LinearLayout}.
     *
     * @param context The context.
     * @param title   Dialog title (ignored if null/empty).
     * @param summary Dialog summary subtitle (ignored if null/empty).
     * @return The root layout ready to add content views to.
     */
    @NonNull
    public static android.widget.LinearLayout createDialogRoot(
            @NonNull Context context,
            @Nullable String title,
            @Nullable String summary) {
        android.widget.LinearLayout root = new android.widget.LinearLayout(context);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_DIALOG_BG);
        int pad = dp(context, 20);
        root.setPadding(pad, pad, pad, pad);

        if (title != null && !title.isEmpty()) {
            android.widget.TextView titleView = new android.widget.TextView(context);
            titleView.setText(title);
            titleView.setTextColor(COLOR_TEXT_PRIMARY);
            titleView.setTextSize(18);
            titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
            root.addView(titleView);
        }
        if (summary != null && !summary.isEmpty()) {
            android.widget.TextView summaryView = new android.widget.TextView(context);
            summaryView.setText(summary);
            summaryView.setTextColor(COLOR_TEXT_SECONDARY);
            summaryView.setTextSize(13);
            android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(context, 6);
            root.addView(summaryView, p);
        }
        return root;
    }

    // -----------------------------------------------------------------------
    // Drawable helpers
    // -----------------------------------------------------------------------

    /**
     * Create a rounded-rectangle {@link GradientDrawable} with the given fill,
     * stroke color, and corner radius (in dp).
     */
    @NonNull
    public static GradientDrawable roundedDrawable(
            @NonNull Context context,
            @ColorInt int fillColor,
            @ColorInt int strokeColor,
            int cornerRadiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fillColor);
        d.setCornerRadius(dp(context, cornerRadiusDp));
        d.setStroke(dp(context, 1), strokeColor);
        return d;
    }

    // -----------------------------------------------------------------------
    // Dialog helpers
    // -----------------------------------------------------------------------

    /** Show a simple message dialog with an OK button. */
    public static void showMessage(@NonNull Context context,
                                   @NonNull String title,
                                   @NonNull String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** Show a confirmation dialog. */
    public static void showConfirm(@NonNull Context context,
                                   @NonNull String title,
                                   @NonNull String message,
                                   @NonNull String positiveLabel,
                                   @NonNull DialogInterface.OnClickListener onPositive) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveLabel, onPositive)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // -----------------------------------------------------------------------
    // Util
    // -----------------------------------------------------------------------

    private static int dp(@NonNull Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
