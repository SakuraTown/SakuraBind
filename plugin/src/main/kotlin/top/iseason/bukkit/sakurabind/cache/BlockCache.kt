package top.iseason.bukkit.sakurabind.cache

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.cache.CacheBuilder
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Item
import org.bukkit.scheduler.BukkitTask
import org.ehcache.Cache
import org.ehcache.PersistentCacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import top.iseason.bukkit.sakurabind.config.DefaultItemSetting
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.utils.other.runSync
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BlockCache : BaseCache() {
    private val filterFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}Filter-Block")
    lateinit var blockCache: Cache<String, String>

    var blockFilter: CuckooFilter = loadFilter(filterFile)
        private set

    val tempBlockCache2 = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .expireAfterAccess(3000L, TimeUnit.MILLISECONDS)
        .build<String, BlockInfo>()

    private val breakingCache: MutableMap<String, Pair<BlockInfo, BukkitTask>> = ConcurrentHashMap()

    val containerCache: MutableMap<String, Material> = ConcurrentHashMap()

    private val emptyInfo = BlockInfo("empty", DefaultItemSetting)

    override fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager> {
        return builder.withCache(
            "Block-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(8192, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(500, MemoryUnit.MB, true)
            ).withDispatcherConcurrency(2)
                .withExpiry(ExpiryPolicyBuilder.noExpiration())
                .build()
        )
    }

    override fun init(cacheManager: org.ehcache.CacheManager) {
        blockCache = cacheManager.getCache("Block-owner", String::class.java, String::class.java)!!
        super.init(cacheManager)
    }

    override fun reloadFilter() {
        val iterator = blockCache.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            blockFilter.put(string2FilterKey(next.key))
        }
    }

    override fun onSave() {
        saveFilter(filterFile, blockFilter)
    }

    fun mightContain(str: String) = blockFilter.mightContain(string2FilterKey(str))

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
            blockFilter.put(string2FilterKey(key))
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

        if (!blockFilter.mightContain(string2FilterKey(key))) return null
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
        val task = runSync {
            breakingCache.remove(loc)
        }
        val cache = breakingCache.put(loc, blockInfo to task)
        if (cache != null) {
            val oldTask = cache.second
            if (!oldTask.isCancelled) oldTask.cancel()
        }
    }

    fun getBreakingCache(loc: String): BlockInfo? = breakingCache[loc]?.first

    fun removeBreakingCache(loc: String) {
        val cache = breakingCache.remove(loc)
        if (cache != null) {
            val oldTask = cache.second
            if (!oldTask.isCancelled) oldTask.cancel()
        }
    }

}