package ca.dnamobile.javalauncher.launcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * A fully-resolved launch plan ready to hand to the JRE.
 * Produced by combining instance settings, renderer config, and FPS-boost JVM flags.
 */
public final class LaunchPlan {
    @NonNull public final List<String> jvmArgs;
    @NonNull public final String rendererIdentifier;
    public final int resolutionScalePercent;
    public final boolean forceFullscreen;
    @Nullable public final String customMainClass;
    @Nullable public final java.io.File gameDirectory;
    @Nullable public final java.io.File runtimeDirectory;

    public LaunchPlan(
            @NonNull List<String> jvmArgs,
            @NonNull String rendererIdentifier,
            int resolutionScalePercent,
            boolean forceFullscreen,
            @Nullable String customMainClass) {
        this(jvmArgs, rendererIdentifier, resolutionScalePercent, forceFullscreen, customMainClass, null, null);
    }

    public LaunchPlan(
            @NonNull List<String> jvmArgs,
            @NonNull String rendererIdentifier,
            int resolutionScalePercent,
            boolean forceFullscreen,
            @Nullable String customMainClass,
            @Nullable java.io.File gameDirectory,
            @Nullable java.io.File runtimeDirectory) {
        this.jvmArgs = jvmArgs;
        this.rendererIdentifier = rendererIdentifier;
        this.resolutionScalePercent = resolutionScalePercent;
        this.forceFullscreen = forceFullscreen;
        this.customMainClass = customMainClass;
        this.gameDirectory = gameDirectory;
        this.runtimeDirectory = runtimeDirectory;
    }

    /** Returns the game directory for this launch, or null if unset. */
    @Nullable
    public java.io.File getGameDirectory() {
        return gameDirectory;
    }

    /** Returns the JRE runtime directory for this launch, or null if unset. */
    @Nullable
    public java.io.File getRuntimeDirectory() {
        return runtimeDirectory;
    }

    @NonNull @Override
    public String toString() {
        return "LaunchPlan{renderer=" + rendererIdentifier
                + ", scale=" + resolutionScalePercent
                + "%, jvmArgs=" + jvmArgs.size() + " args}";
    }
}
