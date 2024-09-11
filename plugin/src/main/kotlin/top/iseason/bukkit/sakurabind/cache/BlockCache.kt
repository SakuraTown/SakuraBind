package top.iseason.bukkit.sakurabind.cache

import com.google.common.cache.CacheBuilder
import com.google.common.hash.Funnels
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Item
import org.ehcache.Cache
import org.ehcache.PersistentCacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import top.iseason.bukkit.sakurabind.config.DefaultItemSetting
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

object BlockCache : BaseCache {
    private val filterFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}Filter-Block")
    private lateinit var blockCache: Cache<String, String>

    private val blockFilter: CuckooFilter<CharSequence> = if (!filterFile.exists()) CuckooFilter.create(
        Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
    )
    else filterFile.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }

//    val tempBlockCache: UserManagedCache<String, String> = UserManagedCacheBuilder
//        .newUserManagedCacheBuilder(String::class.java, String::class.java)
//        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(1)))
//        .withDispatcherConcurrency(1)
//        .withKeyCopier(IdentityCopier())
//        .withValueCopier(IdentityCopier())
//        .build(true)

    private val tempBlockCache2 = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .expireAfterAccess(500L, TimeUnit.MILLISECONDS)
        .softValues()
        .build<String, BlockInfo>()

    private val tempBlockCache3 = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .expireAfterWrite(500L, TimeUnit.MILLISECONDS)
        .build<String, BlockInfo>()

    val containerCache = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .expireAfterWrite(200L, TimeUnit.MILLISECONDS)
        .build<String, Material>()

    private val emptyInfo = BlockInfo("empty", DefaultItemSetting)

    override fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager> {
        return builder.withCache(
            "Block-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(500, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(500, MemoryUnit.MB, true)
            ).withDispatcherConcurrency(2)
                .withExpiry(ExpiryPolicyBuilder.noExpiration())
                .build()
        )
    }

    override fun init(cacheManager: org.ehcache.CacheManager) {
        blockCache = cacheManager.getCache("Block-owner", String::class.java, String::class.java)!!
    }

    override fun onSave() {
        if (!filterFile.exists()) {
            filterFile.createNewFile()
        }
        filterFile.outputStream().use {
            blockFilter.writeTo(it)
        }
    }

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
        if (extraData.isNullOrEmpty()) {
            blockCache.put(key, value)
        } else {
            val extraValue = extraData.joinToString(prefix = "$value\t", separator = "\t")
            blockCache.put(key, extraValue)
        }
        blockFilter.add(key)
    }

    fun removeBlock(block: Block) {
        removeCache(blockToString(block))
    }

    fun removeCache(str: String) {
        tempBlockCache2.invalidate(str)
        blockFilter.remove(str)
        blockCache.remove(str)
    }

    fun blockToString(block: Block): String {
        return CacheManager.locationToString(block.location)
    }

    fun dropItemToString(entity: Item): String {
        return CacheManager.locationToString(entity.location)
    }

    /**
     * 获取方块绑定的信息
     */
    fun getBlockInfo(block: Block): BlockInfo? {
//        if (SakuraBindAPI.isTileEntity(block)) {
//            return SakuraBindAPI.getTileOwner(block)
//        }
        return getBlockInfo(blockToString(block))
    }

    fun getBlockInfo(block: BlockState): BlockInfo? {
        return getBlockInfo(CacheManager.locationToString(block.location))
    }

    /**
     * 获取方块绑定的玩家
     */
    fun getBlockInfo(key: String): BlockInfo? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()
        if (!blockFilter.contains(key)) return null
//        println("mightContain cost ${System.nanoTime() - nanoTime}")
        val info = tempBlockCache2.get(key) {
            val value = blockCache.get(key) ?: return@get emptyInfo
            try {
                BlockInfo.deserialize(value)
            } catch (_: Exception) {
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

    fun addBlockTemp(loc: String, blockInfo: BlockInfo) = tempBlockCache3.put(loc, blockInfo)
    fun getBlockTemp(loc: String): BlockInfo? = tempBlockCache3.getIfPresent(loc)
    fun removeBlockTemp(loc: String) = tempBlockCache3.invalidate(loc)

}