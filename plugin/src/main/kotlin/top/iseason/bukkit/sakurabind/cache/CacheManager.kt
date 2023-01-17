@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import org.bukkit.Location
import org.ehcache.CacheManager
import org.ehcache.PersistentCacheManager
import org.ehcache.Status
import org.ehcache.config.builders.*
import top.iseason.bukkittemplate.BukkitTemplate
import java.io.File
import java.util.*


object CacheManager {

    private var builder: CacheManagerBuilder<PersistentCacheManager>? = null
    private lateinit var cacheManager: CacheManager

    private val cacheManagerList = mutableListOf<BaseCache>()

    init {
        val dataFile = File(BukkitTemplate.getPlugin().dataFolder, "data")
        if (!dataFile.exists()) {
            dataFile.mkdirs()
        }
        builder = CacheManagerBuilder
            .persistence(dataFile)
            .builder(CacheManagerBuilder.newCacheManagerBuilder())
    }

    private val hook = Thread {
        if (cacheManager.status != Status.UNINITIALIZED) {
            cacheManager.close()
            println("[SakuraBind] shutdown hook for encache has finished!")
        }
    }

    /**
     * 添加注册缓存管理器
     */
    fun register(cacheManager: BaseCache) {
        cacheManagerList.add(cacheManager)
    }

    /**
     * 构建初始化缓存管理器
     */
    fun build() {
        if (cacheManagerList.isEmpty()) return
        for (baseCacheManager in cacheManagerList) {
            builder = baseCacheManager.setCache(builder!!)
        }
        cacheManager = builder!!.build(true)
        for (baseCacheManager in cacheManagerList) {
            baseCacheManager.init(cacheManager)
        }
        builder = null
        //容灾
        Runtime.getRuntime().addShutdownHook(hook)

    }

    fun locationToString(location: Location): String =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"


    @Throws(Exception::class)
    fun save() {
        for (baseCache in cacheManagerList) {
            baseCache.onSave()
        }
        cacheManager.close()
        Runtime.getRuntime().removeShutdownHook(hook)
    }

}