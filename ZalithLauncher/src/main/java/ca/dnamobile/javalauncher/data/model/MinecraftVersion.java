package ca.dnamobile.javalauncher.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Represents a Minecraft version entry in DroidBridge's instance list. */
public final class MinecraftVersion {
    @NonNull public final String id;
    @NonNull public final String type; // "release", "snapshot", "old_beta", "old_alpha"
    @Nullable public final String releaseTime;

    public MinecraftVersion(@NonNull String id, @NonNull String type, @Nullable String releaseTime) {
        this.id = id;
        this.type = type;
        this.releaseTime = releaseTime;
    }

    @NonNull public String getId()   { return id; }
    @NonNull public String getType() { return type; }

    public boolean isRelease()  { return "release".equals(type); }
    public boolean isSnapshot() { return "snapshot".equals(type); }

    @NonNull @Override public String toString() { return id; }
}
