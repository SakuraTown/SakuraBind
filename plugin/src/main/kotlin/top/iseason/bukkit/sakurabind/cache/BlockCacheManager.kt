@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import com.google.common.hash.Funnels
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration


object BlockCacheManager {
    private val cacheManager: CacheManager
    private val filter: CuckooFilter<CharSequence>
    private val cache: Cache<String, String>
    private val tempCache: Cache<String, String>

    init {
        val blockDataFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}block-owner")
        if (!blockDataFile.exists()) {
            blockDataFile.mkdirs()
        }
        var builder = CacheManagerBuilder
            .persistence(blockDataFile)
            .builder(CacheManagerBuilder.newCacheManagerBuilder())
        builder = builder.withCache(
            "block-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(10, EntryUnit.ENTRIES)
                    .offheap(50, MemoryUnit.MB)
                    .disk(500, MemoryUnit.MB, true)
            ).withExpiry(ExpiryPolicyBuilder.noExpiration())
                .build()
        )
        builder = builder.withCache(
            "block-owner-temp",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(5, EntryUnit.ENTRIES)
                    .offheap(1, MemoryUnit.MB)
                    .disk(2, MemoryUnit.MB, false)
            ).withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMillis(300)))
                .build()
        )
        cacheManager = builder.build(true)
        cache = cacheManager.getCache("block-owner", String::class.java, String::class.java)!!
        tempCache = cacheManager.getCache("block-owner-temp", String::class.java, String::class.java)!!

        val file = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter")
        filter = if (!file.exists()) CuckooFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
        )
        else file.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }

        DisableHook.addTask {
            filter.writeTo(File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter").outputStream())
        }

    }

    fun addBlock(block: Block, owner: Player) {
        val blockToString = blockToString(block)
        cache.put(blockToString, owner.uniqueId.toString())
        filter.add(blockToString)
    }

    fun removeBlock(block: Block) {
        removeCache(blockToString(block))
    }

    fun removeCache(str: String) {
        filter.remove(str)
        cache.remove(str)
    }

    fun blockToString(block: Block): String {
        return locationToString(block.location, block.type.toString())
    }

    private fun locationToString(location: Location, type: String): String =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}=${type}"

    fun entityToString(entity: Item): String {
        return locationToString(entity.location, entity.itemStack.type.toString())
    }

    fun addTemp(loc: String, owner: String) = tempCache.put(loc, owner)
    fun getTemp(loc: String) = tempCache.get(loc)
    fun removeTemp(loc: String) = tempCache.remove(loc)

    /**
     * 获取方块绑定的玩家
     */
    fun getOwner(block: Block): String? {
//        if (SakuraBindAPI.isTileEntity(block)) {
//            return SakuraBindAPI.getTileOwner(block)
//        }
        return getOwner(blockToString(block))
    }


    /**
     * 获取方块绑定的玩家
     */
    fun getOwner(str: String): String? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()
        if (!filter.contains(str)) return null
//        println("mightContain cost ${System.nanoTime() - nanoTime}")
        return cache.get(str)
    }

    /**
     * 判断方块是否能破坏
     */
    fun canBreak(block: Block, player: Player?): Boolean {
        val owner = getOwner(block) ?: return true
        return player != null && player.uniqueId.toString() == owner
    }

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