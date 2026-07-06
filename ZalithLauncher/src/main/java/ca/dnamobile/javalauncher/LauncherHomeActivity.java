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

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.movtery.zalithlauncher.databinding.FragmentLauncherDroidbridgeBinding;
import com.movtery.zalithlauncher.event.single.AccountUpdateEvent;
import com.movtery.zalithlauncher.event.single.RefreshVersionsEvent;
import com.movtery.zalithlauncher.feature.accounts.AccountsManager;
import com.movtery.zalithlauncher.feature.version.Version;
import com.movtery.zalithlauncher.feature.version.VersionInfo;
import com.movtery.zalithlauncher.feature.version.VersionsManager;
import com.movtery.zalithlauncher.launch.LaunchGame;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import ca.dnamobile.javalauncher.instance.LauncherInstance;
import ca.dnamobile.javalauncher.ui.instance.LauncherInstanceAdapter;

/**
 * DroidBridge-style home screen. This replaces Zalith's original fragment-based
 * main menu as the app's primary GUI: it lists real installed Minecraft versions
 * (via {@link VersionsManager}) as DroidBridge "instances", shows the active
 * account (via {@link AccountsManager}), and launches the game through Zalith's
 * existing {@link LaunchGame} pipeline. Account login and version installation
 * still happen inside {@link LauncherActivity}, which this screen delegates to.
 */
public final class LauncherHomeActivity extends AppCompatActivity implements LauncherInstanceAdapter.Listener {

    private FragmentLauncherDroidbridgeBinding binding;
    private LauncherInstanceAdapter adapter;
    private final List<Version> currentVersions = new ArrayList<>();
    private Version selectedVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentLauncherDroidbridgeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new LauncherInstanceAdapter(this, this);
        binding.dbInstanceList.setLayoutManager(new LinearLayoutManager(this));
        binding.dbInstanceList.setAdapter(adapter);

        binding.dbSettingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, LauncherSettingsActivity.class)));

        binding.dbAccountButton.setOnClickListener(v -> openAccountManagement());

        binding.dbNewInstanceButton.setOnClickListener(v -> openInstanceInstall());

        binding.dbVersionPickerButton.setOnClickListener(v -> showVersionPicker());

        binding.dbPlayButton.setOnClickListener(v -> playSelectedVersion());

        refreshEverything();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Account/version state may have changed while the user was in LauncherActivity
        refreshEverything();
    }

    private void refreshEverything() {
        VersionsManager.INSTANCE.refresh("LauncherHomeActivity", false);
        refreshAccountUi();
        refreshInstanceList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshVersions(RefreshVersionsEvent event) {
        if (event.getMode() == RefreshVersionsEvent.MODE.END) {
            refreshInstanceList();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountUpdate(AccountUpdateEvent event) {
        refreshAccountUi();
    }

    private void refreshAccountUi() {
        MinecraftAccount account = AccountsManager.INSTANCE.getCurrentAccount();
        if (account != null) {
            binding.dbAccountName.setText(account.username);
            binding.dbAccountType.setText(
                    account.accountType != null ? account.accountType : "Local");
        } else {
            binding.dbAccountName.setText(com.movtery.zalithlauncher.R.string.status_signed_out);
            binding.dbAccountType.setText("Tap to sign in");
        }
    }

    private void refreshInstanceList() {
        currentVersions.clear();
        currentVersions.addAll(VersionsManager.INSTANCE.getVersions());

        List<LauncherInstance> displayItems = new ArrayList<>();
        for (Version version : currentVersions) {
            displayItems.add(toLauncherInstance(version));
        }
        adapter.submitList(displayItems);

        Version current = VersionsManager.INSTANCE.getCurrentVersion();
        selectedVersion = current;
        if (current != null) {
            adapter.setSelectedInstanceId(LauncherInstance.sharedInstanceId(
                    current.getVersionName(), current.getVersionPath()));
        } else {
            adapter.clearSelectedInstance();
        }
        updateVersionCard(current);
    }

    private LauncherInstance toLauncherInstance(Version version) {
        VersionInfo info = version.getVersionInfo();
        String loader = "Vanilla";
        if (info != null && info.getLoaderInfo() != null && info.getLoaderInfo().length > 0) {
            String name = info.getLoaderInfo()[0].getName();
            if (name != null && !name.isBlank()) loader = name;
        }
        return LauncherInstance.sharedInstalledVersion(
                version.getVersionName(),
                "installed",
                version.getVersionPath(),
                "",
                loader
        );
    }

    private void updateVersionCard(Version version) {
        if (version == null) {
            binding.dbVersionName.setText("No version selected");
            binding.dbVersionType.setText("Tap an instance to select");
            return;
        }
        binding.dbVersionName.setText(version.getVersionName());
        VersionInfo info = version.getVersionInfo();
        binding.dbVersionType.setText(info != null ? info.getInfoString() : "Installed");
    }

    private Version findVersionByName(String versionName) {
        for (Version version : currentVersions) {
            if (version.getVersionName().equals(versionName)) return version;
        }
        return null;
    }

    @Override
    public void onInstanceSelected(LauncherInstance instance) {
        selectVersion(instance);
    }

    @Override
    public void onInstanceQuickPlayRequested(LauncherInstance instance) {
        selectVersion(instance);
        playSelectedVersion();
    }

    @Override
    public void onInstanceDeleteRequested(LauncherInstance instance) {
        //Deleting installed versions is a destructive, multi-step operation already
        //handled by Zalith's version manager UI; route there instead of duplicating it.
        Toast.makeText(this, "Manage or delete versions from Settings", Toast.LENGTH_SHORT).show();
    }

    private void selectVersion(LauncherInstance instance) {
        Version version = findVersionByName(instance.getBaseVersionId());
        if (version == null) return;
        selectedVersion = version;
        VersionsManager.INSTANCE.saveCurrentVersion(version.getVersionName());
        adapter.setSelectedInstance(instance);
        updateVersionCard(version);
    }

    private void showVersionPicker() {
        if (currentVersions.isEmpty()) {
            Toast.makeText(this, "No installed versions found. Tap New to install one.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[currentVersions.size()];
        for (int i = 0; i < currentVersions.size(); i++) names[i] = currentVersions.get(i).getVersionName();

        new AlertDialog.Builder(this)
                .setTitle("Select instance")
                .setItems(names, (dialog, which) -> {
                    Version version = currentVersions.get(which);
                    selectedVersion = version;
                    VersionsManager.INSTANCE.saveCurrentVersion(version.getVersionName());
                    adapter.setSelectedInstanceId(LauncherInstance.sharedInstanceId(
                            version.getVersionName(), version.getVersionPath()));
                    updateVersionCard(version);
                })
                .show();
    }

    private void playSelectedVersion() {
        if (selectedVersion == null) {
            Toast.makeText(this, "Select an instance first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (AccountsManager.INSTANCE.getCurrentAccount() == null) {
            Toast.makeText(this, "Sign in to an account first", Toast.LENGTH_SHORT).show();
            openAccountManagement();
            return;
        }
        LaunchGame.preLaunch(this, selectedVersion);
    }

    private void openAccountManagement() {
        startActivity(new Intent(this, LauncherSettingsActivity.class));
    }

    private void openInstanceInstall() {
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.putExtra(LauncherActivity.EXTRA_OPEN_FRAGMENT, LauncherActivity.FRAGMENT_INSTALL);
        startActivity(intent);
    }
}
