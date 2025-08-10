package top.iseason.bukkit.sakurabind.cache

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
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
import top.iseason.bukkit.sakurabind.config.BaseSetting
import top.iseason.bukkit.sakurabind.config.ItemSettings
import top.iseason.bukkittemplate.BukkitTemplate
import java.io.File


object EntityCache : BaseCache() {
    private val filterFile = File(BukkitTemplate.getPlugin().dataFolder, "data${File.separator}Filter-Entity")
    private lateinit var entityCache: Cache<String, String>

    private val entityFilter = loadFilter(filterFile)

    override fun newFilter(): CuckooFilter = CuckooFilter
        .Builder(32768)
        .withFalsePositiveRate(0.0001)
        .withExpectedConcurrency(2)
        .build()

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
        super.init(cacheManager)
    }

    override fun reloadFilter() {
        val iterator = entityCache.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            entityFilter.put(string2FilterKey(next.key))
        }
    }

    override fun onSave() {
        saveFilter(filterFile, entityFilter)
    }

    fun getEntityInfo(entity: Entity): Pair<String, BaseSetting>? {
        //使用布谷鸟过滤防止缓存穿透
        val uuid = entity.uniqueId.toString()
        if (!entityFilter.mightContain(string2FilterKey(uuid))) return null
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
        if (!entityCache.containsKey(value)) {
            entityFilter.put(string2FilterKey(uuid))
        }
        entityCache.put(uuid, value)
    }

    fun removeEntity(entity: Entity) {
        val uuid = entity.uniqueId.toString()
        if (entityCache.containsKey(uuid)) {
            entityCache.remove(uuid)
            entityFilter.delete(string2FilterKey(uuid))
        }
    }
}