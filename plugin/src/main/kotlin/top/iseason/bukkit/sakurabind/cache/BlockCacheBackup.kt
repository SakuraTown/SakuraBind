package top.iseason.bukkit.sakurabind.cache

import org.ehcache.Cache
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import java.io.FileOutputStream
import java.nio.file.*
import java.util.*

/**
 * 使用两个正常关闭的 Ehcache 存储轮换保存方块绑定快照。
 * 只有成功发布完成标记的存储才允许用于恢复。
 */
internal class BlockCacheBackup(
    private val root: Path,
    private val logger: (String, Throwable?) -> Unit = { _, _ -> }
) {
    data class Snapshot(val slot: String, val completedAt: Long, val entryCount: Long)

    @Synchronized
    fun backup(source: Cache<String, String>): Snapshot {
        Files.createDirectories(root)
        val previousSnapshots = completedSnapshots()
        val target = selectBackupTarget(previousSnapshots)
        Files.deleteIfExists(markerPath(target))

        val entryCount = withCache(target) { cache ->
            cache.clear()
            var count = 0L
            source.iterator().forEachRemaining { entry ->
                cache.put(entry.key, entry.value)
                count++
            }
            count
        }

        val previousCompletedAt = previousSnapshots.maxOfOrNull { it.completedAt } ?: Long.MIN_VALUE
        val nextCompletedAt = if (previousCompletedAt == Long.MAX_VALUE) Long.MAX_VALUE else previousCompletedAt + 1L
        val completedAt = maxOf(System.currentTimeMillis(), nextCompletedAt)
        val snapshot = Snapshot(target, completedAt, entryCount)
        publish(snapshot)
        return snapshot
    }

    @Synchronized
    fun restore(target: Cache<String, String>): Snapshot? {
        val candidates = completedSnapshots().sortedByDescending { it.completedAt }
        for (snapshot in candidates) {
            if (!validate(snapshot)) continue
            try {
                target.clear()
                val restored = withCache(snapshot.slot) { source ->
                    var count = 0L
                    val batch = LinkedHashMap<String, String>(RESTORE_BATCH_SIZE)
                    source.iterator().forEachRemaining { entry ->
                        batch[entry.key] = entry.value
                        count++
                        if (batch.size >= RESTORE_BATCH_SIZE) {
                            target.putAll(batch)
                            batch.clear()
                        }
                    }
                    if (batch.isNotEmpty()) target.putAll(batch)
                    count
                }
                if (restored != snapshot.entryCount) {
                    target.clear()
                    invalidate(snapshot)
                    logger(
                        "方块绑定备份 ${snapshot.slot} 条目数不匹配，期望 ${snapshot.entryCount}，实际 $restored",
                        null
                    )
                    continue
                }
                return snapshot
            } catch (throwable: Exception) {
                try {
                    target.clear()
                } catch (clearFailure: Exception) {
                    throwable.addSuppressed(clearFailure)
                }
                logger("写入主方块绑定缓存失败，已保留现有备份供下次恢复", throwable)
                return null
            }
        }
        return null
    }

    private fun validate(snapshot: Snapshot): Boolean {
        try {
            Files.deleteIfExists(markerPath(snapshot.slot))
        } catch (throwable: Exception) {
            logger("无法锁定方块绑定备份 ${snapshot.slot} 进行验证，尝试上一份备份", throwable)
            return false
        }
        return try {
            val actualCount = withCache(snapshot.slot) { cache ->
                var count = 0L
                val iterator = cache.iterator()
                while (iterator.hasNext()) {
                    iterator.next()
                    count++
                }
                count
            }
            if (actualCount != snapshot.entryCount) {
                logger(
                    "方块绑定备份 ${snapshot.slot} 条目数不匹配，期望 ${snapshot.entryCount}，实际 $actualCount",
                    null
                )
                false
            } else {
                publish(snapshot)
                true
            }
        } catch (throwable: Exception) {
            logger("验证方块绑定备份 ${snapshot.slot} 失败，尝试上一份备份", throwable)
            false
        }
    }

    private fun invalidate(snapshot: Snapshot) {
        try {
            Files.deleteIfExists(markerPath(snapshot.slot))
        } catch (throwable: Exception) {
            logger("无法撤销损坏的方块绑定备份标记 ${snapshot.slot}", throwable)
        }
    }

    private fun completedSnapshots(): List<Snapshot> = SLOTS.mapNotNull(::readSnapshot)

    private fun selectBackupTarget(completed: List<Snapshot>): String {
        return SLOTS.firstOrNull { slot -> completed.none { it.slot == slot } }
            ?: completed.minByOrNull { it.completedAt }!!.slot
    }

    private fun readSnapshot(slot: String): Snapshot? {
        val marker = markerPath(slot)
        if (!Files.isRegularFile(marker)) return null
        return try {
            val properties = Properties()
            Files.newInputStream(marker).use(properties::load)
            if (properties.getProperty(KEY_FORMAT_VERSION)?.toInt() != FORMAT_VERSION) return null
            Snapshot(
                slot,
                properties.getProperty(KEY_COMPLETED_AT).toLong(),
                properties.getProperty(KEY_ENTRY_COUNT).toLong()
            )
        } catch (throwable: Exception) {
            logger("忽略无效的方块绑定备份标记: $marker", throwable)
            null
        }
    }

    private fun publish(snapshot: Snapshot) {
        val slotRoot = slotRoot(snapshot.slot)
        Files.createDirectories(slotRoot)
        val marker = markerPath(snapshot.slot)
        val temporary = slotRoot.resolve("$MARKER_NAME.tmp")
        val properties = Properties().apply {
            setProperty(KEY_FORMAT_VERSION, FORMAT_VERSION.toString())
            setProperty(KEY_COMPLETED_AT, snapshot.completedAt.toString())
            setProperty(KEY_ENTRY_COUNT, snapshot.entryCount.toString())
        }
        FileOutputStream(temporary.toFile()).use { output ->
            properties.store(output, "SakuraBind 方块绑定缓存备份")
            output.fd.sync()
        }
        atomicReplace(temporary, marker)
    }

    private fun <T> withCache(slot: String, action: (Cache<String, String>) -> T): T {
        val storeRoot = slotRoot(slot).resolve(STORE_DIRECTORY)
        Files.createDirectories(storeRoot)
        val manager = CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence(storeRoot.toFile()))
            .withCache(CACHE_ALIAS, cacheConfiguration())
            .build(true)
        return manager.use {
            val cache = manager.getCache(CACHE_ALIAS, String::class.java, String::class.java)
                ?: error("Ehcache backup cache $CACHE_ALIAS was not created")
            action(cache)
        }
    }

    private fun slotRoot(slot: String) = root.resolve(slot)

    private fun markerPath(slot: String) = slotRoot(slot).resolve(MARKER_NAME)

    companion object {
        const val CACHE_ALIAS = "Block-owner"
        private const val FORMAT_VERSION = 1
        private const val MARKER_NAME = "completed.properties"
        private const val STORE_DIRECTORY = "store"
        private const val KEY_FORMAT_VERSION = "format.version"
        private const val KEY_COMPLETED_AT = "completed.at"
        private const val KEY_ENTRY_COUNT = "entry.count"
        private const val RESTORE_BATCH_SIZE = 512
        private val SLOTS = listOf("a", "b")

        fun cacheConfiguration() = CacheConfigurationBuilder.newCacheConfigurationBuilder(
            String::class.java,
            String::class.java,
            ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(8192, EntryUnit.ENTRIES)
                .offheap(10, MemoryUnit.MB)
                .disk(500, MemoryUnit.MB, true)
        ).withDispatcherConcurrency(2)
            .withExpiry(ExpiryPolicyBuilder.noExpiration())
            .build()

        fun writeRunningMarker(path: Path) {
            Files.createDirectories(path.parent)
            Files.write(
                path,
                System.currentTimeMillis().toString().toByteArray(Charsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        }

        private fun atomicReplace(source: Path, target: Path) {
            try {
                Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
