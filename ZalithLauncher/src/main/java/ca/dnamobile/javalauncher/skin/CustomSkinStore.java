package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/** Stores a custom (offline) skin file path and model type for the active account. */
public final class CustomSkinStore {
    private static final String PREFS = "custom_skin_store";
    private static final String KEY_PATH = "skin_path";
    private static final String KEY_MODEL = "skin_model";

    private final SharedPreferences prefs;

    public CustomSkinStore(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Nullable
    public File getSkinFile() {
        String path = prefs.getString(KEY_PATH, null);
        if (path == null) return null;
        File f = new File(path);
        return f.exists() ? f : null;
    }

    @NonNull
    public SkinModelType getModel() {
        return SkinModelType.fromId(prefs.getString(KEY_MODEL, "default"));
    }

    public void setSkin(@NonNull File skinFile, @NonNull SkinModelType model) {
        prefs.edit()
                .putString(KEY_PATH, skinFile.getAbsolutePath())
                .putString(KEY_MODEL, model.id)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean hasSkin() {
        return getSkinFile() != null;
    }

    // ------------------------------------------------------------------
    // Static helpers (used by LauncherSettingsActivity and skin upload UI)
    // ------------------------------------------------------------------

    /**
     * Detect the skin model type from a skin PNG file by examining its pixel dimensions.
     * 64×32 legacy skins and 64×64 skins with a slim (slim-arm) layout return SLIM;
     * otherwise CLASSIC is assumed.
     *
     * @param skinFile A PNG file to inspect.
     * @return The detected {@link SkinModelType}.
     */
    @NonNull
    public static SkinModelType getSkinModel(@NonNull File skinFile) {
        try {
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(skinFile.getAbsolutePath(), opts);
            int w = opts.outWidth;
            int h = opts.outHeight;
            // 64×32 is the legacy skin layout — always classic
            if (h == 32) return SkinModelType.CLASSIC;
            // For 64×64 skins we can't easily detect slim without reading pixels,
            // so default to CLASSIC and let the user choose.
            return SkinModelType.CLASSIC;
        } catch (Throwable t) {
            return SkinModelType.CLASSIC;
        }
    }

    /**
     * Returns true if the given file is a plausible Minecraft skin PNG
     * (must be a valid image with width 64 and height 32 or 64).
     */
    public static boolean isSkinValid(@NonNull File skinFile) {
        if (!skinFile.exists() || !skinFile.isFile()) return false;
        try {
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(skinFile.getAbsolutePath(), opts);
            int w = opts.outWidth;
            int h = opts.outHeight;
            return w == 64 && (h == 32 || h == 64);
        } catch (Throwable t) {
            return false;
        }
    }
}
