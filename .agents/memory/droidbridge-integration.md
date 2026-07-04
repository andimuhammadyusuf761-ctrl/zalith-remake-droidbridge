---
name: DroidBridge Integration
description: Key decisions and gotchas for DroidBridge Launcher code merged into Zalith Remake
---

# DroidBridge Integration

## R class and databinding imports
**Rule:** Any DroidBridge file that imports `ca.dnamobile.javalauncher.R` or `ca.dnamobile.javalauncher.databinding.*` must be patched to use `com.movtery.zalithlauncher.R` / `com.movtery.zalithlauncher.databinding.*` — the app namespace is `com.movtery.zalithlauncher`.
**Why:** Adding DroidBridge source under a different package does not change the AGP namespace or R class package. Build fails on import resolution otherwise.
**How to apply:** `sed -i 's/import ca\.dnamobile\.javalauncher\.R;/import com.movtery.zalithlauncher.R;/g' <file>`

## Auth stub must match LauncherSettingsActivity exactly
**Rule:** `MicrosoftAuthManagerPersonal` must expose `(Context, AccountStore)` constructor, `setListener(Listener)`, `signIn()`, `signOut()`, `refreshMicrosoftAccount()`, `dispose()`. `Listener` inner interface uses `onSignedIn(AccountStore.Account)` and `onError(String)`. `MicrosoftAuthConfigPersonal` must have `static boolean isConfigured()`.
**Why:** LauncherSettingsActivity's usage is fixed public-API code; any API divergence in the stub is a hard compile failure.

## String resources are extensive — validate before marking done
**Rule:** Run a ripgrep sweep of all R.string.* references in DroidBridge Java files and new layouts against strings.xml before considering resource work done.
**Why:** DroidBridge has 250+ string references spread across 86+ files. Adding strings piecemeal always leaves stragglers that block aapt2 resource linking.
**How to apply:** `find … -name "*.java" | xargs grep -oh 'R\.string\.[a-z_0-9]*' | sort -u | sed 's/R\.string\.//' | while read id; do grep -q "name=\"$id\"" strings.xml || echo "MISSING: $id"; done`

## Format strings must match call-site argument counts
**Rule:** `vulkan_driver_description_value` takes two args (`%1$s — %2$s`). Check any format-string literal for argument-count matches with its `getString(R.string.x, arg1, arg2)` call sites.
**Why:** Mismatched format-string arity compiles but silently drops or crashes on extra args at runtime.
