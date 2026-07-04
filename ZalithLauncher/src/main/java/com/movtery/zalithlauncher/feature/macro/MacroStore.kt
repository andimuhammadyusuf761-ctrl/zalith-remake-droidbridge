package com.movtery.zalithlauncher.feature.macro

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.feature.log.Logging

/**
 * Persistent storage for [Macro]s, backed by SharedPreferences as a single JSON blob.
 *
 * We keep the whole list in one key (`aurora_macros`) instead of per-macro keys to make
 * reordering / bulk import-export trivial. The list is reloaded on every read so config
 * changes from another process (e.g. the in-game overlay) are picked up immediately.
 */
object MacroStore {
    private const val PREFS = "aurora_macros_prefs"
    private const val KEY_MACROS = "aurora_macros"

    private val gson by lazy { Gson() }
    private val listType = object : TypeToken<MutableList<Macro>>() {}.type

    /** Synchronous load. Safe to call on UI thread, dataset is always small. */
    @JvmStatic
    fun load(context: Context): MutableList<Macro> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MACROS, null) ?: return mutableListOf()
        return runCatching {
            val parsed: MutableList<Macro>? = gson.fromJson(raw, listType)
            parsed ?: mutableListOf()
        }.getOrElse {
            Logging.e("MacroStore", "Failed to parse macros: ${it.message}")
            mutableListOf()
        }
    }

    /** Persist the full list. The macros are stored verbatim, no merging. */
    @JvmStatic
    fun save(context: Context, macros: List<Macro>) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MACROS, gson.toJson(macros))
            .apply()
    }

    @JvmStatic
    fun upsert(context: Context, macro: Macro) {
        val all = load(context)
        val idx = all.indexOfFirst { it.id == macro.id }
        if (idx >= 0) all[idx] = macro else all.add(macro)
        save(context, all)
    }

    @JvmStatic
    fun delete(context: Context, id: String) {
        val all = load(context)
        all.removeAll { it.id == id }
        save(context, all)
    }

    @JvmStatic
    fun get(context: Context, id: String): Macro? {
        return load(context).firstOrNull { it.id == id }
    }
}
