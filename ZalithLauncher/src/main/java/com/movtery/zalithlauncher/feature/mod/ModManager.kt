package com.movtery.zalithlauncher.feature.mod

import com.movtery.zalithlauncher.feature.log.Logging
import com.movtery.zalithlauncher.utils.path.PathManager
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Advanced mod management system with backup, conflict detection, and modpack support.
 */
object ModManager {
    private const val TAG = "ModManager"
    private const val BACKUP_EXTENSION = ".modbak"
    private const val DISABLED_EXTENSION = ".disabled"
    private const val CONFLICT_MARKER = "# CONFLICTS #"

    data class ModInfo(
        val file: File,
        val modId: String,
        val name: String,
        val version: String,
        val description: String,
        val authors: List<String>,
        val dependencies: List<ModDependency>,
        val loaders: List<String>,
        val mcVersions: List<String>,
        val isEnabled: Boolean,
        val fileSize: Long,
        val checksum: String
    )

    data class ModDependency(
        val modId: String,
        val versionRange: String,
        val isRequired: Boolean
    )

    data class ConflictInfo(
        val mod1: ModInfo,
        val mod2: ModInfo,
        val reason: String,
        val severity: Severity
    ) {
        enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
    }

    data class ModpackManifest(
        val name: String,
        val version: String,
        val author: String,
        val description: String,
        val mcVersion: String,
        val loader: String,
        val loaderVersion: String,
        val mods: List<ModpackEntry>
    )

    data class ModpackEntry(
        val modId: String,
        val fileName: String,
        val version: String,
        val downloadUrl: String?,
        val checksum: String?
    )

    /**
     * Scan and parse all mods in the mods folder.
     */
    @JvmStatic
    fun scanMods(gameDir: File = File(PathManager.DIR_GAME_HOME)): List<ModInfo> {
        val modsDir = File(gameDir, "mods")
        if (!modsDir.exists() || !modsDir.isDirectory) return emptyList()

        return modsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jar", true) || file.name.endsWith(DISABLED_EXTENSION, true))
        }?.mapNotNull { file ->
            parseModInfo(file)
        }?.sortedBy { it.name } ?: emptyList()
    }

    /**
     * Parse mod metadata from a mod file.
     */
    @JvmStatic
    fun parseModInfo(file: File): ModInfo? {
        return try {
            val isEnabled = !file.name.endsWith(DISABLED_EXTENSION, true)
            val actualFile = if (isEnabled) file else File(file.parent, file.name.removeSuffix(DISABLED_EXTENSION))

            ZipFile(actualFile).use { zip ->
                val fabricModJson = zip.getEntry("fabric.mod.json")
                val quiltModJson = zip.getEntry("quilt.mod.json")
                val modsToml = zip.getEntry("META-INF/mods.toml")
                val mcmodInfo = zip.getEntry("mcmod.info")

                when {
                    fabricModJson != null -> parseFabricMod(zip, fabricModJson, file, isEnabled)
                    quiltModJson != null -> parseQuiltMod(zip, quiltModJson, file, isEnabled)
                    modsToml != null -> parseForgeMod(zip, modsToml, file, isEnabled)
                    mcmodInfo != null -> parseLegacyMod(zip, mcmodInfo, file, isEnabled)
                    else -> createMinimalModInfo(file, isEnabled)
                }
            }
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to parse mod: ${file.name}", e)
            createMinimalModInfo(file, file.name.endsWith(DISABLED_EXTENSION, true))
        }
    }

    private fun parseFabricMod(zip: ZipFile, entry: ZipEntry, file: File, isEnabled: Boolean): ModInfo {
        val json = zip.getInputStream(entry).bufferedReader().readText()
        val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: file.nameWithoutExtension
        val name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: id
        val version = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "Unknown"
        val description = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""

        return ModInfo(
            file = file,
            modId = id,
            name = name,
            version = version,
            description = description,
            authors = emptyList(),
            dependencies = emptyList(),
            loaders = listOf("fabric"),
            mcVersions = emptyList(),
            isEnabled = isEnabled,
            fileSize = file.length(),
            checksum = calculateChecksum(file)
        )
    }

    private fun parseQuiltMod(zip: ZipFile, entry: ZipEntry, file: File, isEnabled: Boolean): ModInfo {
        // Similar to fabric parsing
        return parseFabricMod(zip, entry, file, isEnabled).copy(loaders = listOf("quilt"))
    }

    private fun parseForgeMod(zip: ZipFile, entry: ZipEntry, file: File, isEnabled: Boolean): ModInfo {
        val content = zip.getInputStream(entry).bufferedReader().readText()
        val modId = Regex("modId\\s*=\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: file.nameWithoutExtension
        val displayName = Regex("displayName\\s*=\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: modId
        val version = Regex("version\\s*=\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: "Unknown"

        return ModInfo(
            file = file,
            modId = modId,
            name = displayName,
            version = version,
            description = "",
            authors = emptyList(),
            dependencies = emptyList(),
            loaders = listOf("forge"),
            mcVersions = emptyList(),
            isEnabled = isEnabled,
            fileSize = file.length(),
            checksum = calculateChecksum(file)
        )
    }

    private fun parseLegacyMod(zip: ZipFile, entry: ZipEntry, file: File, isEnabled: Boolean): ModInfo {
        return createMinimalModInfo(file, isEnabled)
    }

    private fun createMinimalModInfo(file: File, isEnabled: Boolean): ModInfo {
        return ModInfo(
            file = file,
            modId = file.nameWithoutExtension.removeSuffix(DISABLED_EXTENSION),
            name = file.nameWithoutExtension.removeSuffix(DISABLED_EXTENSION),
            version = "Unknown",
            description = "",
            authors = emptyList(),
            dependencies = emptyList(),
            loaders = emptyList(),
            mcVersions = emptyList(),
            isEnabled = isEnabled,
            fileSize = file.length(),
            checksum = calculateChecksum(file)
        )
    }

    /**
     * Detect conflicts between mods.
     */
    @JvmStatic
    fun detectConflicts(mods: List<ModInfo>): List<ConflictInfo> {
        val conflicts = mutableListOf<ConflictInfo>()

        for (i in mods.indices) {
            for (j in i + 1 until mods.size) {
                val mod1 = mods[i]
                val mod2 = mods[j]

                // Check for duplicate mod IDs
                if (mod1.modId == mod2.modId && mod1.modId.isNotBlank()) {
                    conflicts.add(ConflictInfo(
                        mod1 = mod1,
                        mod2 = mod2,
                        reason = "Duplicate mod ID: ${mod1.modId}",
                        severity = ConflictInfo.Severity.HIGH
                    ))
                }

                // Check for conflicting dependencies
                val mod1Deps = mod1.dependencies.map { it.modId }.toSet()
                val mod2Deps = mod2.dependencies.map { it.modId }.toSet()
                if (mod1Deps.contains(mod2.modId) && mod2Deps.contains(mod1.modId)) {
                    conflicts.add(ConflictInfo(
                        mod1 = mod1,
                        mod2 = mod2,
                        reason = "Circular dependency between ${mod1.modId} and ${mod2.modId}",
                        severity = ConflictInfo.Severity.CRITICAL
                    ))
                }
            }
        }

        return conflicts
    }

    /**
     * Create a backup of all mods.
     */
    @JvmStatic
    fun createBackup(
        gameDir: File = File(PathManager.DIR_GAME_HOME),
        backupName: String? = null
    ): File? {
        val modsDir = File(gameDir, "mods")
        if (!modsDir.exists()) return null

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val name = backupName ?: "mods_backup_$timestamp"
        val backupFile = File(gameDir, "$name.zip")

        return try {
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                modsDir.listFiles { f -> f.isFile && (f.name.endsWith(".jar") || f.name.endsWith(DISABLED_EXTENSION)) }
                    ?.forEach { file ->
                        FileInputStream(file).use { fis ->
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
            }
            Logging.i(TAG, "Created mod backup: ${backupFile.absolutePath}")
            backupFile
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to create backup", e)
            null
        }
    }

    /**
     * Restore mods from a backup.
     */
    @JvmStatic
    fun restoreBackup(backupFile: File, gameDir: File = File(PathManager.DIR_GAME_HOME)): Boolean {
        val modsDir = File(gameDir, "mods")
        if (!modsDir.exists()) modsDir.mkdirs()

        return try {
            // First, disable all current mods instead of deleting them
            modsDir.listFiles { f -> f.isFile && f.name.endsWith(".jar") }?.forEach { mod ->
                val disabledFile = File(modsDir, "${mod.name}$DISABLED_EXTENSION")
                mod.renameTo(disabledFile)
            }

            // Extract backup contents
            ZipFile(backupFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory) {
                        val destFile = File(modsDir, entry.name)
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            Logging.i(TAG, "Restored mods from backup: ${backupFile.name}")
            true
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to restore backup", e)
            false
        }
    }

    /**
     * Export a modpack from current mods.
     */
    @JvmStatic
    fun exportModpack(
        name: String,
        version: String,
        author: String,
        description: String,
        mcVersion: String,
        loader: String,
        gameDir: File = File(PathManager.DIR_GAME_HOME)
    ): File? {
        val modsDir = File(gameDir, "mods")
        if (!modsDir.exists()) return null

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val modpackFile = File(gameDir, "${name.replace(" ", "_")}_${version}_$timestamp.zlmodpack")

        return try {
            ZipOutputStream(FileOutputStream(modpackFile)).use { zos ->
                // Add manifest
                val mods = scanMods(gameDir).map { mod ->
                    ModpackEntry(
                        modId = mod.modId,
                        fileName = mod.file.name,
                        version = mod.version,
                        downloadUrl = null,
                        checksum = mod.checksum
                    )
                }

                val manifest = ModpackManifest(
                    name = name,
                    version = version,
                    author = author,
                    description = description,
                    mcVersion = mcVersion,
                    loader = loader,
                    loaderVersion = "",
                    mods = mods
                )

                // Write manifest JSON
                val manifestJson = generateManifestJson(manifest)
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifestJson.toByteArray())
                zos.closeEntry()

                // Copy mod files
                modsDir.listFiles { f -> f.isFile && (f.name.endsWith(".jar") || f.name.endsWith(DISABLED_EXTENSION)) }
                    ?.forEach { file ->
                        FileInputStream(file).use { fis ->
                            val entry = ZipEntry("mods/${file.name}")
                            zos.putNextEntry(entry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
            }

            Logging.i(TAG, "Exported modpack: ${modpackFile.absolutePath}")
            modpackFile
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to export modpack", e)
            null
        }
    }

    /**
     * Import a modpack.
     */
    @JvmStatic
    fun importModpack(modpackFile: File, gameDir: File = File(PathManager.DIR_GAME_HOME)): Boolean {
        return try {
            ZipFile(modpackFile).use { zip ->
                // Read manifest
                val manifestEntry = zip.getEntry("manifest.json")
                    ?: throw IllegalStateException("Invalid modpack: manifest.json not found")

                val manifestJson = zip.getInputStream(manifestEntry).bufferedReader().readText()
                val manifest = parseManifestJson(manifestJson)

                // Create backup before importing
                createBackup(gameDir, "pre_import_backup")

                // Clear current mods (disable them)
                val modsDir = File(gameDir, "mods")
                if (modsDir.exists()) {
                    modsDir.listFiles { f -> f.isFile && f.name.endsWith(".jar") }?.forEach { mod ->
                        val disabledFile = File(modsDir, "${mod.name}$DISABLED_EXTENSION")
                        mod.renameTo(disabledFile)
                    }
                } else {
                    modsDir.mkdirs()
                }

                // Extract mod files
                zip.entries().asSequence()
                    .filter { it.name.startsWith("mods/") && !it.isDirectory }
                    .forEach { entry ->
                        val destFile = File(modsDir, entry.name.removePrefix("mods/"))
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                Logging.i(TAG, "Imported modpack: ${manifest.name} v${manifest.version}")
            }
            true
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to import modpack", e)
            false
        }
    }

    /**
     * Toggle a mod's enabled state.
     */
    @JvmStatic
    fun toggleMod(mod: ModInfo): Boolean {
        return try {
            val newFile = if (mod.isEnabled) {
                File(mod.file.parent, "${mod.file.name}$DISABLED_EXTENSION")
            } else {
                File(mod.file.parent, mod.file.name.removeSuffix(DISABLED_EXTENSION))
            }

            val success = mod.file.renameTo(newFile)
            if (success) {
                Logging.i(TAG, "${if (mod.isEnabled) "Disabled" else "Enabled"} mod: ${mod.name}")
            }
            success
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to toggle mod: ${mod.name}", e)
            false
        }
    }

    /**
     * Delete a mod file.
     */
    @JvmStatic
    fun deleteMod(mod: ModInfo, backup: Boolean = true): Boolean {
        return try {
            if (backup) {
                val backupDir = File(mod.file.parent, "deleted_mods")
                backupDir.mkdirs()
                val backupFile = File(backupDir, "${mod.file.name}.${System.currentTimeMillis()}$BACKUP_EXTENSION")
                mod.file.copyTo(backupFile)
            }

            val success = mod.file.delete()
            if (success) {
                Logging.i(TAG, "Deleted mod: ${mod.name}")
            }
            success
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to delete mod: ${mod.name}", e)
            false
        }
    }

    /**
     * Calculate MD5 checksum of a file.
     */
    private fun calculateChecksum(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun generateManifestJson(manifest: ModpackManifest): String {
        // Simplified JSON generation
        return """
        {
            "name": "${manifest.name}",
            "version": "${manifest.version}",
            "author": "${manifest.author}",
            "description": "${manifest.description}",
            "minecraft": "${manifest.mcVersion}",
            "loader": "${manifest.loader}",
            "mods": [
                ${manifest.mods.joinToString(",\n                ") { "{ \"modId\": \"${it.modId}\", \"version\": \"${it.version}\", \"fileName\": \"${it.fileName}\" }" }}
            ]
        }
        """.trimIndent()
    }

    private fun parseManifestJson(json: String): ModpackManifest {
        // Simplified JSON parsing - in production, use Gson or similar
        val name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "Unknown"
        val version = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "1.0.0"
        val author = Regex("\"author\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "Unknown"
        val description = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""
        val mcVersion = Regex("\"minecraft\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "1.20.1"
        val loader = Regex("\"loader\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: "fabric"

        return ModpackManifest(
            name = name,
            version = version,
            author = author,
            description = description,
            mcVersion = mcVersion,
            loader = loader,
            loaderVersion = "",
            mods = emptyList()
        )
    }
}