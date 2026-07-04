package com.movtery.zalithlauncher.feature.crash

import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.feature.mod.ModUtils
import com.movtery.zalithlauncher.utils.path.PathManager
import org.apache.commons.io.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile

/**
 * Detects and safely repairs mod-related Minecraft crashes.
 *
 * The analyzer is intentionally heuristic: crash logs rarely expose a single reliable mod id,
 * so it combines loader-specific phrases, Java package names and local mod jar metadata.
 */
object ModCrashAnalyzer {
    private const val TAG = "ModCrashAnalyzer"
    private val MOD_ID_REGEX = Regex("(?i)(?:mod(?:id)?|for mod|in mod|loading mod|mod file)[:=\\s'\\\"]+([a-z0-9_\\-.]{2,64})")
    private val PACKAGE_REGEX = Regex("(?:^|\\s)at\\s+([a-zA-Z_][\\w$]*(?:\\.[a-zA-Z_][\\w$]*){1,})")
    private val LOADER_MARKERS = listOf("forge", "fabric", "quilt", "mod loading", "modlauncher", "fabricloader", "quilt_loader")

    data class SuspectedMod(val file: File, val modId: String?, val reason: String, val score: Int)
    data class AnalysisResult(val isLikelyModCrash: Boolean, val logFile: File?, val suspects: List<SuspectedMod>, val summary: String)

    @JvmStatic
    fun analyze(gameDirPath: String?): AnalysisResult {
        val gameDir = gameDirPath?.let(::File)?.takeIf { it.isDirectory } ?: File(PathManager.DIR_GAME_HOME)
        val logFile = newestReadableLog(gameDir) ?: newestReadableLog(File(PathManager.DIR_GAME_HOME))
        val text = logFile?.readTextSafely().orEmpty()
        if (text.isBlank()) return AnalysisResult(false, logFile, emptyList(), "No readable crash log was found.")

        val lower = text.lowercase(Locale.ROOT)
        val loaderHit = LOADER_MARKERS.any(lower::contains)
        val crashHit = listOf("exception", "crash", "error", "mixin", "mod loading has failed").any(lower::contains)
        val modDir = File(gameDir, "mods")
        val mods = modDir.listFiles { file -> file.isFile && file.name.endsWith(ModUtils.JAR_FILE_SUFFIX, ignoreCase = true) }.orEmpty().toList()
        val modIds = extractModIds(text)
        val packageHints = extractPackageHints(text)

        val suspects = mods.mapNotNull { file ->
            val meta = readModMetadata(file)
            var score = 0
            val reasons = mutableListOf<String>()
            val candidates = (meta.ids + file.nameWithoutExtension).map { it.lowercase(Locale.ROOT) }
            candidates.forEach { candidate ->
                if (candidate.length >= 2 && (candidate in modIds || lower.contains(candidate))) {
                    score += 40
                    reasons += "mentioned in the crash log"
                }
                if (packageHints.any { it.contains(candidate.replace('-', '.')) || candidate.contains(it.substringBefore('.')) }) {
                    score += 25
                    reasons += "matched stack-trace package"
                }
            }
            meta.packages.forEach { pkg ->
                if (packageHints.any { it.startsWith(pkg) || pkg.startsWith(it) }) {
                    score += 35
                    reasons += "matched classes inside the jar"
                }
            }
            if (System.currentTimeMillis() - file.lastModified() < 7L * 24 * 60 * 60 * 1000) {
                score += 10
                reasons += "recently added or updated"
            }
            if (score > 0) SuspectedMod(file, meta.ids.firstOrNull(), reasons.distinct().joinToString(), score) else null
        }.sortedByDescending { it.score }.take(6)

        val likely = crashHit && (loaderHit || suspects.isNotEmpty() || mods.isNotEmpty() && lower.contains("mixin"))
        val summary = if (suspects.isEmpty()) {
            "Game crashed and mod loader patterns were detected, but no single mod could be isolated. Try Safe Mode first."
        } else {
            "Game crashed likely because of mods. Review the suspected mods below before restarting."
        }
        return AnalysisResult(likely, logFile, suspects, summary)
    }

    @JvmStatic
    fun disableSuspects(gameDirPath: String?, suspects: List<SuspectedMod>): Int {
        backupMods(gameDirPath)
        return suspects.count { runCatching { ModUtils.disableMod(it.file); logAction("Disabled ${it.file.name}: ${it.reason}"); true }.getOrElse { false } }
    }

    @JvmStatic
    fun deleteSuspects(gameDirPath: String?, suspects: List<SuspectedMod>): Int {
        backupMods(gameDirPath)
        return suspects.count { runCatching { FileUtils.deleteQuietly(it.file); logAction("Deleted ${it.file.name}: ${it.reason}"); true }.getOrElse { false } }
    }

    @JvmStatic
    fun safeModeDisableAll(gameDirPath: String?): Int {
        val gameDir = gameDirPath?.let(::File) ?: File(PathManager.DIR_GAME_HOME)
        val mods = File(gameDir, "mods").listFiles { f -> f.isFile && f.name.endsWith(ModUtils.JAR_FILE_SUFFIX, true) }.orEmpty()
        backupMods(gameDirPath)
        return mods.count { runCatching { ModUtils.disableMod(it); logAction("Safe Mode disabled ${it.name}"); true }.getOrDefault(false) }
    }

    private fun newestReadableLog(gameDir: File): File? = listOf(
        File(gameDir, "crash-reports"), gameDir, File(PathManager.DIR_GAME_HOME)
    ).flatMap { dir -> dir.listFiles().orEmpty().filter { it.isFile && (it.name.endsWith(".txt") || it.name.endsWith(".log")) } }
        .maxByOrNull { it.lastModified() }

    private fun File.readTextSafely(): String = runCatching { readText().takeLast(256_000) }.getOrDefault("")
    private fun extractModIds(text: String) = MOD_ID_REGEX.findAll(text).map { it.groupValues[1].lowercase(Locale.ROOT) }.toSet()
    private fun extractPackageHints(text: String) = PACKAGE_REGEX.findAll(text).map { it.groupValues[1].substringBeforeLast('.').lowercase(Locale.ROOT) }.toSet()

    private data class ModMeta(val ids: Set<String>, val packages: Set<String>)
    private fun readModMetadata(file: File): ModMeta = runCatching {
        ZipFile(file).use { zip ->
            val ids = mutableSetOf<String>()
            val packages = mutableSetOf<String>()
            listOf("fabric.mod.json", "quilt.mod.json", "META-INF/mods.toml", "mcmod.info").forEach { name ->
                zip.getEntry(name)?.let { entry -> ids += Regex("(?i)(?:\"id\"|modId)\\s*[:=]\\s*[\"']?([a-z0-9_\\-.]+)").findAll(zip.getInputStream(entry).bufferedReader().readText()).map { it.groupValues[1].lowercase(Locale.ROOT) } }
            }
            zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".class") }.take(200).forEach { entry ->
                entry.name.substringBeforeLast('/', "").replace('/', '.').takeIf { it.count { c -> c == '.' } >= 1 }?.let { packages += it.lowercase(Locale.ROOT) }
            }
            ModMeta(ids, packages)
        }
    }.getOrElse { ModMeta(emptySet(), emptySet()) }

    private fun backupMods(gameDirPath: String?) {
        val gameDir = gameDirPath?.let(::File) ?: File(PathManager.DIR_GAME_HOME)
        val mods = File(gameDir, "mods")
        if (!mods.isDirectory) return
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        FileUtils.copyDirectory(mods, File(gameDir, "mods-backup-$stamp"))
        logAction("Backed up mods folder to mods-backup-$stamp")
    }

    private fun logAction(message: String) {
        Logging.i(TAG, message)
        runCatching { File(PathManager.DIR_LAUNCHER_LOG, "mod_crash_recovery.log").appendText("${Date()}: $message\n") }
    }
}
