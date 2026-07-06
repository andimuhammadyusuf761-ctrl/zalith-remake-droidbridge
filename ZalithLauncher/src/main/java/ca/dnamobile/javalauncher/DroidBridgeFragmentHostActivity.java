/*
 * Copyright (c) 2026 DNA Mobile Applications.
 * All rights reserved.
 *
 * This file is DroidBridge project code.
 * It is not part of Minecraft and does not grant rights to Minecraft,
 * Mojang, Microsoft, PojavLauncher, Zalith Launcher, or any third-party project.
 *
 * Files written entirely by DNA Mobile Applications are proprietary unless
 * a file header or separate license notice states otherwise.
 */

package ca.dnamobile.javalauncher;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.ui.fragment.CustomMouseFragment;
import com.movtery.zalithlauncher.ui.fragment.DownloadFragment;
import com.movtery.zalithlauncher.ui.fragment.DownloadModFragment;

import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.utils.FullscreenUtils;

/**
 * A lightweight DroidBridge-styled host activity for Zalith fragments that do not
 * have their own standalone DroidBridge equivalent yet (version installer, mouse cursor
 * picker, etc.). Renders a clean Material3 toolbar on top of the fragment — no
 * Zalith Aurora chrome, no old launcher home screen.
 *
 * <p>Callers open this via {@link #intentFor(Context, String)} with one of the
 * FRAGMENT_* constants defined below.</p>
 */
public final class DroidBridgeFragmentHostActivity extends AppCompatActivity {

    private static final String EXTRA_FRAGMENT = "db_host_fragment";

    /** Fragment target: Minecraft version / modpack installer. */
    public static final String FRAGMENT_INSTALL = "install";

    /** Fragment target: Custom mouse cursor icon picker. */
    public static final String FRAGMENT_CUSTOM_MOUSE = "custom_mouse";

    /**
     * Builds a ready-to-use Intent that opens this activity showing the given fragment.
     *
     * @param context  Calling context.
     * @param fragment One of the FRAGMENT_* constants in this class.
     * @return Intent that can be passed to {@link Context#startActivity(Intent)}.
     */
    @NonNull
    public static Intent intentFor(@NonNull Context context, @NonNull String fragment) {
        Intent intent = new Intent(context, DroidBridgeFragmentHostActivity.class);
        intent.putExtra(EXTRA_FRAGMENT, fragment);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_droidbridge_fragment_host);
        FullscreenUtils.enableImmersive(this);

        MaterialToolbar toolbar = findViewById(R.id.dbHostToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        String fragmentTarget = getIntent().getStringExtra(EXTRA_FRAGMENT);
        String title;
        Fragment fragment;

        if (FRAGMENT_CUSTOM_MOUSE.equals(fragmentTarget)) {
            title = "Mouse Cursor Icon";
            fragment = new CustomMouseFragment();
        } else {
            // Default: version / modpack installer
            title = getString(R.string.version_install_new);
            fragment = new DownloadFragment();
        }

        toolbar.setTitle(title);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.dbHostFragmentContainer, fragment)
                    .commit();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) FullscreenUtils.enableImmersive(this);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
