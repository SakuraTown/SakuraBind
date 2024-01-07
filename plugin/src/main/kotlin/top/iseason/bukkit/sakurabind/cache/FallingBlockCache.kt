package top.iseason.bukkit.sakurabind.cache

import com.google.common.cache.CacheBuilder
import com.google.common.hash.Funnels
import org.bukkit.entity.Entity
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.PersistentCacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import java.io.File
import java.nio.charset.StandardCharsets

object FallingBlockCache : BaseCache {
    private val filterFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}Filter-FallingBlock")
    private lateinit var fallingBlockCache: Cache<String, String>
    private val fallingBlockFilter: CuckooFilter<CharSequence> = if (!filterFile.exists()) CuckooFilter.create(
        Funnels.stringFunnel(StandardCharsets.UTF_8), 10240, 0.03
    )
    else filterFile.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }
    private val tempCache = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .maximumSize(30)
        .softValues()
        .build<String, BlockInfo>()

    override fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager> {
        return builder.withCache(
            "FallingBlock-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(10, EntryUnit.ENTRIES)
                    .offheap(1, MemoryUnit.MB)
                    .disk(20, MemoryUnit.MB, true)
            ).withExpiry(ExpiryPolicyBuilder.noExpiration())
//                .withService(OffHeapDiskStoreConfiguration(2))
                .build()
        )
    }

    override fun init(cacheManager: CacheManager) {
        fallingBlockCache = cacheManager.getCache("FallingBlock-owner", String::class.java, String::class.java)!!
    }

    override fun onSave() {
        if (!filterFile.exists()) {
            filterFile.createNewFile()
        }
        filterFile.outputStream().use {
            fallingBlockFilter.writeTo(it)
        }
    }

    fun getFallingInfo(entity: Entity): BlockInfo? {
        //使用布谷鸟过滤防止缓存穿透
//        val nanoTime = System.nanoTime()
        val uuid = entity.uniqueId.toString()
        if (!fallingBlockFilter.contains(uuid)) return null
        return tempCache.get(uuid) {
            val value = fallingBlockCache.get(uuid) ?: return@get null
            BlockInfo.deserialize(value)
        }
//        return cache.get(str)
    }

    fun addFalling(entity: Entity, blockInfo: BlockInfo) {
        val uuid = entity.uniqueId.toString()
        fallingBlockCache.put(uuid, blockInfo.serialize())
        fallingBlockFilter.add(uuid)
    }

    fun removeEntity(entity: Entity) {
        val uuid = entity.uniqueId.toString()
        tempCache.invalidate(uuid)
        fallingBlockCache.remove(uuid)
        fallingBlockFilter.remove(uuid)
    }

}