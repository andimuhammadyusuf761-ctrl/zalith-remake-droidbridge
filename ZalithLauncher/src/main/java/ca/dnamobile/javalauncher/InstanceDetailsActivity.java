package ca.dnamobile.javalauncher;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Stub for InstanceDetailsActivity.
 * Holds the Intent extra keys used to pass instance metadata between activities.
 */
public final class InstanceDetailsActivity {

    public static final String EXTRA_INSTANCE_ID        = "ca.dnamobile.javalauncher.extra.INSTANCE_ID";
    public static final String EXTRA_INSTANCE_NAME      = "ca.dnamobile.javalauncher.extra.INSTANCE_NAME";
    public static final String EXTRA_INSTANCE_LOADER    = "ca.dnamobile.javalauncher.extra.INSTANCE_LOADER";
    public static final String EXTRA_BASE_VERSION_ID    = "ca.dnamobile.javalauncher.extra.BASE_VERSION_ID";
    public static final String EXTRA_MINECRAFT_VERSION_ID = "ca.dnamobile.javalauncher.extra.MINECRAFT_VERSION_ID";
    public static final String EXTRA_VERSION_TYPE       = "ca.dnamobile.javalauncher.extra.VERSION_TYPE";
    public static final String EXTRA_ROOT_DIRECTORY     = "ca.dnamobile.javalauncher.extra.ROOT_DIRECTORY";
    public static final String EXTRA_GAME_DIRECTORY     = "ca.dnamobile.javalauncher.extra.GAME_DIRECTORY";
    public static final String EXTRA_ICON_FILE          = "ca.dnamobile.javalauncher.extra.ICON_FILE";
    public static final String EXTRA_ISOLATED           = "ca.dnamobile.javalauncher.extra.ISOLATED";
    public static final String EXTRA_CURRENT_PACK_VERSION = "ca.dnamobile.javalauncher.extra.CURRENT_PACK_VERSION";

    private InstanceDetailsActivity() {}

    @NonNull
    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, InstanceDetailsActivity.class);
    }
}
