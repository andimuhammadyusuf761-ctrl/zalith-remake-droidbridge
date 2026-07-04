package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.movtery.zalithlauncher.R;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ca.dnamobile.javalauncher.data.AccountStore;

/** Loads and caches player head textures for display in the DroidBridge UI. */
public final class PlayerHeadLoader {
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private PlayerHeadLoader() {}

    /**
     * Asynchronously load a player head from Crafatar and set it on an ImageView.
     * Falls back to a default drawable on failure.
     */
    public static void load(@NonNull Context context, @Nullable String uuid,
                            @NonNull ImageView target, int fallbackDrawable) {
        if (uuid == null) {
            target.setImageResource(fallbackDrawable);
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bmp = downloadHead(uuid);
            target.post(() -> {
                if (bmp != null) target.setImageBitmap(bmp);
                else target.setImageResource(fallbackDrawable);
            });
        });
    }

    /**
     * Asynchronously load a player head for the given {@link AccountStore.Account} into
     * an ImageView.  Falls back to the player-head placeholder.
     *
     * @param context  Any context (Activity, Application, etc.)
     * @param target   ImageView to populate
     * @param account  Account whose head to load (may be null → placeholder shown)
     * @param callback Optional {@link Runnable} called on the UI thread after load (may be null)
     */
    /**
     * Overload that accepts any {@link android.view.View} subclass (e.g. PlayerModelPreviewView).
     * If the view is an {@link ImageView} the bitmap/drawable is set directly;
     * otherwise the view's background is updated.
     */
    public static void loadInto(@NonNull Context context,
                                @NonNull android.view.View target,
                                @Nullable AccountStore.Account account,
                                @Nullable Runnable callback) {
        if (target instanceof ImageView) {
            loadInto(context, (ImageView) target, account, callback);
        } else {
            // For non-ImageView targets (e.g. PlayerModelPreviewView) do nothing —
            // those views load their own textures via their own rendering pipeline.
            if (callback != null) target.post(callback);
        }
    }

    public static void loadInto(@NonNull Context context,
                                @NonNull ImageView target,
                                @Nullable AccountStore.Account account,
                                @Nullable Runnable callback) {
        String uuid = account != null ? account.uuid : null;
        if (uuid == null || uuid.isEmpty()) {
            target.post(() -> {
                target.setImageResource(R.drawable.ic_player_head_placeholder);
                if (callback != null) callback.run();
            });
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bmp = downloadHead(uuid);
            target.post(() -> {
                if (bmp != null) target.setImageBitmap(bmp);
                else target.setImageResource(R.drawable.ic_player_head_placeholder);
                if (callback != null) callback.run();
            });
        });
    }

    /**
     * Synchronously extract the player-head (8×8 region from the top-left of a skin
     * texture) from a skin PNG file and return it as a 64×64 Bitmap.
     * Returns null on any error.
     */
    @Nullable
    public static Bitmap loadHeadFromSkinFile(@NonNull File skinFile) {
        try {
            Bitmap skin = BitmapFactory.decodeFile(skinFile.getAbsolutePath());
            if (skin == null) return null;
            // Head region: x=8, y=8, w=8, h=8 on the skin texture; scale to 64×64
            Bitmap head = Bitmap.createBitmap(skin, 8, 8, 8, 8);
            Bitmap scaled = Bitmap.createScaledBitmap(head, 64, 64, false);
            if (!head.sameAs(scaled)) head.recycle();
            skin.recycle();
            return scaled;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns true if the given account has an offline skin set in the custom skin store.
     */
    public static boolean hasOfflineSkin(@Nullable AccountStore.Account account) {
        return account != null && account.hasOfflineSkin();
    }

    @Nullable
    private static Bitmap downloadHead(String uuid) {
        try {
            URL url = new URL("https://crafatar.com/avatars/" + uuid + "?size=64&overlay");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (InputStream is = conn.getInputStream()) {
                return BitmapFactory.decodeStream(is);
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
