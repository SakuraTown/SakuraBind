package top.iseason.bukkit.sakurabind.cache

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
import top.iseason.bukkit.sakurabind.config.ItemSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkit.sakurabind.cuckoofilter.CuckooFilter
import top.iseason.bukkittemplate.BukkitTemplate
import java.io.File
import java.nio.charset.StandardCharsets

object EntityCache : BaseCache {
    private val filterFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}Filter-Entity")
    private lateinit var entityCache: Cache<String, String>
    private val entityFilter: CuckooFilter<CharSequence> = if (!filterFile.exists()) CuckooFilter.create(
        Funnels.stringFunnel(StandardCharsets.UTF_8), 20480, 0.03
    )
    else filterFile.inputStream().use { CuckooFilter.readFrom(it, Funnels.stringFunnel(StandardCharsets.UTF_8)) }

    override fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager> {
        return builder.withCache(
            "Entity-owner",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, String::class.java,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(100, EntryUnit.ENTRIES)
                    .offheap(10, MemoryUnit.MB)
                    .disk(200, MemoryUnit.MB, true)
            ).withExpiry(ExpiryPolicyBuilder.noExpiration())
//                .withService(OffHeapDiskStoreConfiguration(2))
                .build()
        )
    }

    override fun init(cacheManager: CacheManager) {
        entityCache = cacheManager.getCache("Entity-owner", String::class.java, String::class.java)!!
    }

    override fun onSave() {
        if (!filterFile.exists()) {
            filterFile.createNewFile()
        }
        filterFile.outputStream().use {
            entityFilter.writeTo(it)
        }
    }

    fun getEntityInfo(entity: Entity): Pair<String, ItemSetting>? {
        //使用布谷鸟过滤防止缓存穿透
        val uuid = entity.uniqueId.toString()
        if (!entityFilter.contains(uuid)) return null
        val get = entityCache.get(uuid) ?: return null
        val split = get.split(',')
        return split[0] to ItemSettings.getSetting(split.getOrNull(1))
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
}