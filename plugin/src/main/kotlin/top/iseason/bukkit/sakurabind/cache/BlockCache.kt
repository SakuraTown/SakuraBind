package top.iseason.bukkit.sakurabind.cache

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Item
import org.bukkit.scheduler.BukkitTask
import org.ehcache.Cache
import org.ehcache.PersistentCacheManager
import org.ehcache.config.builders.CacheManagerBuilder
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkit.sakurabind.config.DefaultItemSetting
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.other.submit
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BlockCache : BaseCache() {
    lateinit var blockCache: Cache<String, String>

    var blockFilter: CuckooFilter = newFilter()
        private set

    @Volatile
    private var filterReady = false

    private val backupRoot by lazy {
        BukkitTemplate.getPlugin().dataFolder.toPath().resolve("backup").resolve("block-cache")
    }
    private val runningMarker by lazy { backupRoot.resolve("running.marker") }
    private var previousRunUnclean = false
    private val backupService by lazy {
        BlockCacheBackup(backupRoot) { message, throwable ->
            warn(message)
            throwable?.printStackTrace()
        }
    }
    private var backupTask: BukkitTask? = null
    private val backupLock = Any()
    private var shuttingDown = false

    private data class CacheLocation(val cacheKey: String, val world: String, val x: Int, val y: Int, val z: Int)

    val tempBlockCache2: com.google.common.cache.Cache<String?, BlockInfo?> = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .expireAfterAccess(3000L, TimeUnit.MILLISECONDS)
        .build<String, BlockInfo>()

    private val breakingCache: MutableMap<String, BlockInfo> = HashMap()

    init {
        Bukkit.getScheduler()
            .runTaskTimer(BukkitTemplate.getPlugin(), Runnable { breakingCache.clear() }, 0, 1)
    }

    val containerCache: MutableMap<String, String> = ConcurrentHashMap()

    private val emptyInfo = BlockInfo("empty", DefaultItemSetting)

    override fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager> {
        return builder.withCache(
            BlockCacheBackup.CACHE_ALIAS,
            BlockCacheBackup.cacheConfiguration()
        )
    }

    override fun beforeManagerBuild() {
        previousRunUnclean = Files.exists(runningMarker)
        BlockCacheBackup.writeRunningMarker(runningMarker)
    }

    override fun init(cacheManager: org.ehcache.CacheManager) {
        blockCache = cacheManager.getCache(BlockCacheBackup.CACHE_ALIAS, String::class.java, String::class.java)!!
        if (previousRunUnclean && Config.block_cache_backup__enable) {
            if (blockCache.iterator().hasNext()) {
                warn("检测到上次 JVM 异常退出，但主方块绑定缓存仍有数据，已跳过自动恢复")
            } else {
                val snapshot = backupService.restore(blockCache)
                if (snapshot != null) {
                    info("&a已从方块绑定备份 ${snapshot.slot} 恢复 ${snapshot.entryCount} 条数据")
                    removeMissingRestoredBlocks()
                } else {
                    warn("检测到上次 JVM 异常退出，但没有可用的方块绑定备份")
                }
            }
        }
        reloadFilter()
        startBackupTask()
    }

    override fun reloadFilter() {
        val rebuilt = newFilter()
        var complete = true
        blockCache.iterator().forEachRemaining { entry ->
            if (!rebuilt.put(string2FilterKey(entry.key))) complete = false
        }
        blockFilter = rebuilt
        filterReady = complete
        if (!complete) warn("方块绑定过滤器容量不足，已自动绕过过滤器以避免绑定数据误判")
    }

    override fun onSave() {
        backupTask?.cancel()
        backupTask = null
        synchronized(backupLock) {
            shuttingDown = true
            if (Config.block_cache_backup__enable) backupInternal()
        }
    }

    override fun onClosed() {
        Files.deleteIfExists(runningMarker)
    }

    fun mightContain(str: String) = !filterReady || blockFilter.mightContain(string2FilterKey(str))

    fun addBlock(block: Block, owner: UUID, setting: String?, extraData: List<String>?) {
        val blockToString = blockToString(block)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner.toString()
        addBlock(blockToString, value, extraData)
    }

    fun addBlock(block: Block, owner: String, setting: String?, extraData: List<String>?) {
        val blockToString = blockToString(block)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner
        addBlock(blockToString, value, extraData)
    }

    fun addBlock(state: BlockState, owner: UUID, setting: String?, extraData: List<String>?) {
        val blockToString = CacheManager.locationToString(state.location)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner.toString()
        addBlock(blockToString, value, extraData)
    }

    private fun addBlock(key: String, value: String, extraData: List<String>?) {
        if (!blockCache.containsKey(key)) {
            if (!blockFilter.put(string2FilterKey(key))) filterReady = false
        }
        if (extraData.isNullOrEmpty()) {
            blockCache.put(key, value)
        } else {
            val extraValue = extraData.joinToString(prefix = "$value\t", separator = "\t")
            blockCache.put(key, extraValue)
        }
    }

    fun removeBlock(block: Block) {
        removeBlock(blockToString(block))
    }

    fun removeBlock(block: BlockState) {
        removeBlock(blockToString(block))
    }

    fun removeBlock(str: String) {
        if (blockCache.containsKey(str)) {
            blockCache.remove(str)
            blockFilter.delete(string2FilterKey(str))
        }
        tempBlockCache2.invalidate(str)
    }

    inline fun blockToString(block: Block): String {
        return CacheManager.locationToString(block.location)
    }

    inline fun blockToString(block: BlockState): String {
        return CacheManager.locationToString(block.location)
    }

    inline fun dropItemToString(entity: Item): String {
        return CacheManager.locationToString(entity.location)
    }

    /**
     * 获取方块绑定的信息
     */
    fun getBlockInfo(block: Block): BlockInfo? {
        return getBlockInfo(CacheManager.locationToString(block.location))
    }

//    fun getBlockInfo(block: BlockState): BlockInfo? {
//        return getBlockInfo(CacheManager.locationToString(block.location))
//    }

    /**
     * 获取方块绑定的玩家
     */
    fun getBlockInfo(key: String): BlockInfo? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()

        if (!mightContain(key)) return null
//        println("mightContain cost ${System.nanoTime() - nanoTime}")
        val info = tempBlockCache2.get(key) {
            val value = blockCache.get(key) ?: return@get emptyInfo
            try {
                BlockInfo.deserialize(value)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyInfo
            }
        }
        return if (info === emptyInfo) null else info

//        val value = blockCache.get(key) ?: return null
//
//        val strings = value.split('\t')
//        val split = strings.first().split(',')
//        val info = if (strings.size > 1) {
//            BlockInfo(split[0], ItemSettings.getSetting(split.getOrNull(1)), strings.drop(1))
//        } else {
//            BlockInfo(split[0], ItemSettings.getSetting(split.getOrNull(1)))
//        }
//        return info
//        return cache.get(str)
    }

    fun addBreakingCache(loc: String, blockInfo: BlockInfo) {
        breakingCache[loc] = blockInfo
    }

    fun getBreakingCache(loc: String): BlockInfo? = breakingCache[loc]

    fun removeBreakingCache(loc: String) {
        breakingCache.remove(loc)
    }

    private fun removeMissingRestoredBlocks() {
        val restoredLocations = ArrayList<CacheLocation>()
        blockCache.iterator().forEachRemaining { entry ->
            val location = parseCacheLocation(entry.key) ?: return@forEachRemaining
            restoredLocations.add(location)
        }
        var removed = 0
        for (location in restoredLocations) {
            val world = Bukkit.getWorld(location.world) ?: continue
            if (world.getBlockAt(location.x, location.y, location.z).isEmpty) {
                removeBlock(location.cacheKey)
                removed++
            }
        }
        if (removed > 0) info("&e恢复后已清理 $removed 条方块不存在的绑定数据")
    }

    private fun parseCacheLocation(cacheKey: String): CacheLocation? {
        val zSeparator = cacheKey.lastIndexOf(',')
        if (zSeparator <= 0) return null
        val ySeparator = cacheKey.lastIndexOf(',', zSeparator - 1)
        if (ySeparator <= 0) return null
        val xSeparator = cacheKey.lastIndexOf(',', ySeparator - 1)
        if (xSeparator <= 0) return null
        val world = cacheKey.substring(0, xSeparator)
        val x = cacheKey.substring(xSeparator + 1, ySeparator).toIntOrNull() ?: return null
        val y = cacheKey.substring(ySeparator + 1, zSeparator).toIntOrNull() ?: return null
        val z = cacheKey.substring(zSeparator + 1).toIntOrNull() ?: return null
        return CacheLocation(cacheKey, world, x, y, z)
    }

    private fun startBackupTask() {
        if (!Config.block_cache_backup__enable) return
        shuttingDown = false
        val minutes = Config.block_cache_backup__interval_minutes
        if (minutes <= 0L) {
            warn("block-cache-backup.interval-minutes 必须大于 0，定时备份未启动")
            return
        }
        val period = minutes.coerceAtMost(Long.MAX_VALUE / 1200L) * 1200L
        backupTask = submit(delay = 1L, period = period, async = true) { backupNow() }
    }

    private fun backupNow() {
        synchronized(backupLock) {
            if (!shuttingDown) backupInternal()
        }
    }

    private fun backupInternal() {
        try {
            val snapshot = backupService.backup(blockCache)
            info("&a方块绑定缓存已备份至 ${snapshot.slot}，共 ${snapshot.entryCount} 条数据")
        } catch (throwable: Exception) {
            warn("方块绑定缓存备份失败，上一份完整备份不受影响")
            throwable.printStackTrace()
        }
    }

}
