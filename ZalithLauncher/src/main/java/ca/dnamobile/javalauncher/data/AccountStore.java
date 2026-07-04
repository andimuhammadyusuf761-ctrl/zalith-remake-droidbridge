/*
 * DroidBridge integration stub — AccountStore.
 * Provides the full API surface called by LauncherSettingsActivity and other
 * DroidBridge classes. Persists account data in SharedPreferences.
 */
package ca.dnamobile.javalauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import ca.dnamobile.javalauncher.skin.SkinModelType;

/**
 * Stores and retrieves account data for DroidBridge:
 * - One active Microsoft or offline account
 * - List of offline accounts
 * - Last-seen Microsoft account (for skin operations even after switching to offline)
 */
public final class AccountStore {

    // ──────────────────────────────────────────────────────────────────────
    //  Account model
    // ──────────────────────────────────────────────────────────────────────

    public static final class Account {
        /** Stable unique ID (UUID string). */
        public final String accountId;

        /** Display name (Minecraft username or Microsoft gamertag). */
        public final String username;

        /** "microsoft" | "offline" */
        public final String type;

        /** Minecraft player UUID (null for offline accounts). */
        @Nullable public final String uuid;

        /** Public skin texture URL (Microsoft skins). */
        @Nullable public final String skinUrl;

        /** Microsoft Bearer token for Minecraft Services API (null for offline). */
        @Nullable public final String minecraftAccessToken;

        /** Skin model for offline accounts. Null if no custom offline skin. */
        @Nullable public final SkinModelType offlineSkinModel;

        /** URI of locally-stored offline skin PNG. Null if no custom skin set. */
        @Nullable public final String offlineSkinUriString;

        public Account(
                @NonNull String accountId,
                @NonNull String username,
                @NonNull String type,
                @Nullable String uuid,
                @Nullable String skinUrl,
                @Nullable String minecraftAccessToken,
                @Nullable SkinModelType offlineSkinModel,
                @Nullable String offlineSkinUriString) {
            this.accountId = accountId;
            this.username = username;
            this.type = type;
            this.uuid = uuid;
            this.skinUrl = skinUrl;
            this.minecraftAccessToken = minecraftAccessToken;
            this.offlineSkinModel = offlineSkinModel;
            this.offlineSkinUriString = offlineSkinUriString;
        }

        public boolean isOfflineAccount()   { return "offline".equals(type); }
        public boolean isMicrosoftAccount() { return "microsoft".equals(type); }

        /** True if this offline account has a locally-stored custom skin. */
        public boolean hasOfflineSkin() {
            return isOfflineAccount() && offlineSkinUriString != null;
        }

        /** True if a valid Minecraft session token is present. */
        public boolean hasMinecraftSession() {
            return minecraftAccessToken != null && !minecraftAccessToken.isEmpty();
        }

        /** Returns username, falling back to UUID, falling back to accountId. */
        @NonNull
        public String getBestDisplayName() {
            if (username != null && !username.isEmpty()) return username;
            if (uuid    != null && !uuid.isEmpty())     return uuid;
            return accountId;
        }

        // ── JSON serialisation ──
        @NonNull JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("accountId",            accountId);
            o.put("username",             username);
            o.put("type",                 type);
            if (uuid                  != null) o.put("uuid",                  uuid);
            if (skinUrl               != null) o.put("skinUrl",               skinUrl);
            if (minecraftAccessToken  != null) o.put("minecraftAccessToken",  minecraftAccessToken);
            if (offlineSkinModel      != null) o.put("offlineSkinModel",      offlineSkinModel.id);
            if (offlineSkinUriString  != null) o.put("offlineSkinUri",        offlineSkinUriString);
            return o;
        }

        @NonNull static Account fromJson(@NonNull JSONObject o) throws Exception {
            String model = o.optString("offlineSkinModel", null);
            return new Account(
                    o.getString("accountId"),
                    o.getString("username"),
                    o.getString("type"),
                    o.optString("uuid",                 null),
                    o.optString("skinUrl",              null),
                    o.optString("minecraftAccessToken", null),
                    model != null ? SkinModelType.fromId(model) : null,
                    o.optString("offlineSkinUri",       null));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Storage keys
    // ──────────────────────────────────────────────────────────────────────

    private static final String PREFS                    = "droidbridge_account";
    private static final String KEY_ACTIVE               = "active_account";
    private static final String KEY_LAST_MICROSOFT       = "last_microsoft_account";
    private static final String KEY_OFFLINE_LIST         = "offline_accounts";
    private static final String KEY_MS_COMPLETED_ONCE    = "ms_login_completed_once";

    private final SharedPreferences prefs;

    public AccountStore(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Active account
    // ──────────────────────────────────────────────────────────────────────

    /** Returns the currently active account, or null if none. */
    @Nullable
    public Account load() {
        String json = prefs.getString(KEY_ACTIVE, null);
        if (json == null) return null;
        try { return Account.fromJson(new JSONObject(json)); }
        catch (Exception e) { return null; }
    }

    /** Persist the active account. */
    public void save(@NonNull Account account) {
        try { prefs.edit().putString(KEY_ACTIVE, account.toJson().toString()).apply(); }
        catch (Exception ignored) {}
    }

    /** Clear the active account (sign out). */
    public void clear() {
        prefs.edit().remove(KEY_ACTIVE).apply();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Microsoft-specific helpers
    // ──────────────────────────────────────────────────────────────────────

    /** True if the user has successfully signed in with Microsoft at least once. */
    public boolean hasMicrosoftLoginCompletedOnce() {
        return prefs.getBoolean(KEY_MS_COMPLETED_ONCE, false);
    }

    /** True if we have a persisted Microsoft account (active or remembered). */
    public boolean hasStoredMicrosoftAccount() {
        Account active = load();
        if (active != null && active.isMicrosoftAccount()) return true;
        return prefs.contains(KEY_LAST_MICROSOFT);
    }

    /**
     * Returns the last Microsoft account that was signed in, even if the user
     * has since switched to an offline account. Null if never signed in.
     */
    @Nullable
    public Account loadLastMicrosoftAccount() {
        String json = prefs.getString(KEY_LAST_MICROSOFT, null);
        if (json == null) return null;
        try { return Account.fromJson(new JSONObject(json)); }
        catch (Exception e) { return null; }
    }

    /**
     * Re-activate the last Microsoft account.
     * Saves it as the active account and clears the active offline account.
     */
    public void useLastMicrosoftAccount() {
        Account ms = loadLastMicrosoftAccount();
        if (ms != null) save(ms);
    }

    /**
     * Persist a signed-in Microsoft account.
     * Also records it as the "last Microsoft account" for skin operations.
     */
    public void saveMicrosoftAccount(@NonNull Account account) {
        if (!account.isMicrosoftAccount()) throw new IllegalArgumentException("Not a Microsoft account");
        save(account);
        try { prefs.edit()
                .putString(KEY_LAST_MICROSOFT, account.toJson().toString())
                .putBoolean(KEY_MS_COMPLETED_ONCE, true)
                .apply();
        } catch (Exception ignored) {}
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Offline account management
    // ──────────────────────────────────────────────────────────────────────

    /** Returns all stored offline accounts. */
    @NonNull
    public ArrayList<Account> listOfflineAccounts() {
        ArrayList<Account> list = new ArrayList<>();
        String json = prefs.getString(KEY_OFFLINE_LIST, null);
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(Account.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        return list;
    }

    /**
     * Create or update an offline account.
     * @param existingId null → create new account; non-null → update that account.
     * @param name       Minecraft username.
     * @param skinUri    URI of skin PNG to associate, or null.
     * @param clearSkin  If true, remove any existing offline skin.
     * @return The created/updated account (also set as active).
     */
    @NonNull
    public Account saveOrUpdateOfflineAccount(
            @Nullable String existingId,
            @NonNull  String name,
            @Nullable Uri skinUri,
            boolean clearSkin) {

        ArrayList<Account> accounts = listOfflineAccounts();

        // Find existing entry if updating
        Account existing = null;
        int existingIdx = -1;
        if (existingId != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (existingId.equals(accounts.get(i).accountId)) {
                    existing = accounts.get(i);
                    existingIdx = i;
                    break;
                }
            }
        }

        String id = (existing != null) ? existing.accountId : UUID.randomUUID().toString();

        // Determine skin
        String skinUriStr;
        SkinModelType skinModel;
        if (clearSkin) {
            skinUriStr = null;
            skinModel  = null;
        } else if (skinUri != null) {
            skinUriStr = skinUri.toString();
            skinModel  = SkinModelType.CLASSIC;
        } else {
            skinUriStr = (existing != null) ? existing.offlineSkinUriString : null;
            skinModel  = (existing != null) ? existing.offlineSkinModel     : null;
        }

        Account updated = new Account(id, name, "offline", null, null, null, skinModel, skinUriStr);

        if (existingIdx >= 0) accounts.set(existingIdx, updated);
        else                  accounts.add(updated);

        persistOfflineList(accounts);
        save(updated); // set as active
        return updated;
    }

    /** Activate an existing offline account by its accountId. */
    public void activateOfflineAccount(@NonNull String accountId) {
        for (Account a : listOfflineAccounts()) {
            if (accountId.equals(a.accountId)) { save(a); return; }
        }
    }

    /** Delete an offline account. If it was active, the active account is cleared. */
    public void deleteOfflineAccount(@NonNull String accountId) {
        ArrayList<Account> accounts = listOfflineAccounts();
        accounts.removeIf(a -> accountId.equals(a.accountId));
        persistOfflineList(accounts);
        Account active = load();
        if (active != null && accountId.equals(active.accountId)) clear();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ──────────────────────────────────────────────────────────────────────

    private void persistOfflineList(@NonNull ArrayList<Account> accounts) {
        try {
            JSONArray arr = new JSONArray();
            for (Account a : accounts) arr.put(a.toJson());
            prefs.edit().putString(KEY_OFFLINE_LIST, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
