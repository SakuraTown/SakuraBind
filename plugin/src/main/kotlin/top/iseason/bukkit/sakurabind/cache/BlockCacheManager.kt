@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import com.google.common.hash.Funnels
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.UserManagedCache
import org.ehcache.config.builders.*
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.copy.IdentityCopier
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.config.Setting
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*


object BlockCacheManager {
    private val cacheManager: CacheManager
    private val filter: CuckooFilter<CharSequence>
    private val cache: Cache<String, String>

    //    private val tempCache: Cache<String, String>
    val tempCache: UserManagedCache<String, String> = UserManagedCacheBuilder
        .newUserManagedCacheBuilder(String::class.java, String::class.java)
        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(3)))
        .withKeyCopier(IdentityCopier())
        .withValueCopier(IdentityCopier())
        .build(true)

    init {
        val blockDataFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}block-owner")
        if (!blockDataFile.exists()) {
            blockDataFile.mkdirs()
        }
//        println(1)
        var builder = CacheManagerBuilder
            .persistence(blockDataFile)
            .builder(CacheManagerBuilder.newCacheManagerBuilder())

        builder = builder.withCache(
            "block-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(50, EntryUnit.ENTRIES)
                    .offheap(50, MemoryUnit.MB)
                    .disk(500, MemoryUnit.MB, true)
            ).withExpiry(ExpiryPolicyBuilder.noExpiration())
                .build()
        )
        cacheManager = builder.build(true)
        cache = cacheManager.getCache("block-owner", String::class.java, String::class.java)!!
        val file = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter")
        filter = if (!file.exists()) CuckooFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
        )
        else file.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }

        DisableHook.addTask {
            save()
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
        val blockToString = locationToString(state.location)
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner.toString()
        addBlock(blockToString, value)
    }

    fun addBlock(key: String, value: String) {
        cache.put(key, value)
        filter.add(key)
    }

    fun removeBlock(block: Block) {
        removeCache(blockToString(block))
    }

    fun removeCache(str: String) {
        filter.remove(str)
        cache.remove(str)
    }

    fun blockToString(block: Block): String {
        return locationToString(block.location)
    }

    fun locationToString(location: Location): String =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

    fun entityToString(entity: Item): String {
        return locationToString(entity.location)
    }

    fun addTemp(loc: String, owner: String) = tempCache.put(loc, owner)
    fun getTemp(loc: String) = tempCache.get(loc)
    fun removeTemp(loc: String) = tempCache.remove(loc)

    /**
     * 获取方块绑定的玩家
     */
    fun getOwner(block: Block): Pair<String, Setting>? {
//        if (SakuraBindAPI.isTileEntity(block)) {
//            return SakuraBindAPI.getTileOwner(block)
//        }
        return getOwner(blockToString(block))
    }

    fun getOwner(block: BlockState): Pair<String, Setting>? {
        return getOwner(locationToString(block.location))
    }


    /**
     * 获取方块绑定的玩家
     */
    fun getOwner(str: String): Pair<String, Setting>? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()
        if (!filter.contains(str)) return null
//        println("mightContain cost ${System.nanoTime() - nanoTime}")
        val get = cache.get(str) ?: return null
        val split = get.split(',')
        return split[0] to ItemSettings.getSetting(split.getOrNull(1))
//        return cache.get(str)
    }

    /**
     * 判断方块是否能破坏
     */
    fun canBreak(block: Block, player: Player?): Boolean {
        val (owner, setting) = getOwner(block) ?: return true
        return !setting.getBoolean("block-deny.break", owner, player)
    }

    @Throws(Exception::class)
    fun save() {
        val file = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.outputStream().use {
            filter.writeTo(it)
        }
        cacheManager.close()
    }


}