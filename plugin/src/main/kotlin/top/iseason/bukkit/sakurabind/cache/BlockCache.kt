package top.iseason.bukkit.sakurabind.cache

import com.google.common.hash.Funnels
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Item
import org.ehcache.Cache
import org.ehcache.PersistentCacheManager
import org.ehcache.UserManagedCache
import org.ehcache.config.builders.*
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.copy.IdentityCopier
import top.iseason.bukkit.sakurabind.config.ItemSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*

object BlockCache : BaseCache {
    private val filterFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}Filter-Block")
    private lateinit var blockCache: Cache<String, String>

    private val blockFilter: CuckooFilter<CharSequence> = if (!filterFile.exists()) CuckooFilter.create(
        Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
    )
    else filterFile.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }

    private val tempBlockCache: UserManagedCache<String, String> = UserManagedCacheBuilder
        .newUserManagedCacheBuilder(String::class.java, String::class.java)
        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(3)))
        .withKeyCopier(IdentityCopier())
        .withValueCopier(IdentityCopier())
        .build(true)

    override fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager> {
        return builder.withCache(
            "Block-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(50, EntryUnit.ENTRIES)
                    .offheap(50, MemoryUnit.MB)
                    .disk(500, MemoryUnit.MB, true)
            ).withExpiry(ExpiryPolicyBuilder.noExpiration())
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

    fun addBlock(block: Block, owner: UUID, setting: String?) {
        val blockToString = blockToString(block)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner.toString()
        addBlock(blockToString, value)
    }

    fun addBlock(block: Block, owner: String, setting: String?) {
        val blockToString = blockToString(block)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner
        addBlock(blockToString, value)
    }

    fun addBlock(state: BlockState, owner: UUID, setting: String?) {
        val blockToString = CacheManager.locationToString(state.location)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner.toString()
        addBlock(blockToString, value)
    }

    fun addBlock(key: String, value: String) {
        blockCache.put(key, value)
        blockFilter.add(key)
    }

    fun removeBlock(block: Block) {
        removeCache(blockToString(block))
    }

    fun removeCache(str: String) {
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
     * 获取方块绑定的玩家
     */
    fun getBlockOwner(block: Block): Pair<String, ItemSetting>? {
//        if (SakuraBindAPI.isTileEntity(block)) {
//            return SakuraBindAPI.getTileOwner(block)
//        }
        return getBlockOwner(blockToString(block))
    }

    fun getBlockOwner(block: BlockState): Pair<String, ItemSetting>? {
        return getBlockOwner(CacheManager.locationToString(block.location))
    }

    /**
     * 获取方块绑定的玩家
     */
    fun getBlockOwner(str: String): Pair<String, ItemSetting>? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()
        if (!blockFilter.contains(str)) return null
//        println("mightContain cost ${System.nanoTime() - nanoTime}")
        val get = blockCache.get(str) ?: return null
        val split = get.split(',')
        return split[0] to ItemSettings.getSetting(split.getOrNull(1))
//        return cache.get(str)
    }

    fun addBlockTemp(loc: String, owner: String) = tempBlockCache.put(loc, owner)
    fun getBlockTemp(loc: String): String? = tempBlockCache.get(loc)
    fun removeBlockTemp(loc: String) = tempBlockCache.remove(loc)
}