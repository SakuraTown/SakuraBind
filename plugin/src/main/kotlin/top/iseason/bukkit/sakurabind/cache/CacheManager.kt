@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import com.google.common.hash.Funnels
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.UserManagedCache
import org.ehcache.config.builders.*
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import org.ehcache.impl.copy.IdentityCopier
import top.iseason.bukkit.sakurabind.config.ItemSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*


object CacheManager {
    private val cacheManager: CacheManager
    private val blockFilter: CuckooFilter<CharSequence>
    private val entityFilter: CuckooFilter<CharSequence>
    private val blockCache: Cache<String, String>
    private val entityCache: Cache<String, String>

    //    private val tempCache: Cache<String, String>
    private val tempBlockCache: UserManagedCache<String, String> = UserManagedCacheBuilder
        .newUserManagedCacheBuilder(String::class.java, String::class.java)
        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(3)))
        .withKeyCopier(IdentityCopier())
        .withValueCopier(IdentityCopier())
        .build(true)

    init {
        val blockDataFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}owner-cache")
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
        builder = builder.withCache(
            "entity-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(30, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(200, MemoryUnit.MB, true)
            ).withExpiry(ExpiryPolicyBuilder.noExpiration())
                .build()
        )
        cacheManager = builder.build(true)
        blockCache = cacheManager.getCache("block-owner", String::class.java, String::class.java)!!
        entityCache = cacheManager.getCache("entity-owner", String::class.java, String::class.java)!!

        val blockFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter-block")
        blockFilter = if (!blockFile.exists()) CuckooFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
        )
        else blockFile.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }
        val entityFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter-entity")
        entityFilter = if (!blockFile.exists()) CuckooFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
        )
        else entityFile.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }
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
        return locationToString(block.location)
    }

    private fun locationToString(location: Location): String =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

    fun dropItemToString(entity: Item): String {
        return locationToString(entity.location)
    }

    fun addBlockTemp(loc: String, owner: String) = tempBlockCache.put(loc, owner)
    fun getBlockTemp(loc: String): String? = tempBlockCache.get(loc)
    fun removeBlockTemp(loc: String) = tempBlockCache.remove(loc)

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
        return getBlockOwner(locationToString(block.location))
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

    fun getEntityOwner(entity: Entity): Pair<String, ItemSetting>? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()
        val uuid = entity.uniqueId.toString()
        if (!entityFilter.contains(uuid)) return null
//        println("mightContain cost ${System.nanoTime() - nanoTime}")
        val get = entityCache.get(uuid) ?: return null
        val split = get.split(',')
        return split[0] to ItemSettings.getSetting(split.getOrNull(1))
//        return cache.get(str)
    }

    fun addEntity(entity: Entity, owner: String, setting: String?) {
        val value = if (setting != null && setting != "global-setting")
            "$owner,$setting"
        else owner
        addEntity(entity, value)
    }

    fun addEntity(entity: Entity, value: String) {
        val uuid = entity.uniqueId.toString()
        entityCache.put(uuid, value)
        entityFilter.add(uuid)
    }

    fun removeEntity(entity: Entity) {
        val uuid = entity.uniqueId.toString()
        entityCache.remove(uuid)
        entityFilter.remove(uuid)
    }

    @Throws(Exception::class)
    fun save() {
        val blockFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter-block")
        if (!blockFile.exists()) {
            blockFile.createNewFile()
        }
        blockFile.outputStream().use {
            blockFilter.writeTo(it)
        }
        val entityFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}filter-entity")
        if (!entityFile.exists()) {
            entityFile.createNewFile()
        }
        entityFile.outputStream().use {
            entityFilter.writeTo(it)
        }
        cacheManager.close()
    }


}