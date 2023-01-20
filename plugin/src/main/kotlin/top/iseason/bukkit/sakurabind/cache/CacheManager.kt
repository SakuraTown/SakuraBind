@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import org.bukkit.Bukkit
import org.bukkit.Location
import org.ehcache.CacheManager
import org.ehcache.PersistentCacheManager
import org.ehcache.Status
import org.ehcache.config.builders.*
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.utils.other.submit
import java.io.File
import java.lang.Thread.sleep
import java.util.*


object CacheManager {

    private var builder: CacheManagerBuilder<PersistentCacheManager>? = null
    private var cacheManager: CacheManager? = null

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
        if (cacheManager?.status != Status.UNINITIALIZED) {
            cacheManager?.close()
            println("[SakuraBind] shutdown hook for encache has finished!")
        }
    }
    private var lastTime = System.currentTimeMillis()
    private val timeout = Bukkit.spigot().config.getInt("settings.timeout-time", 60) * 1000
    private var pluginDisabled = false
    private val watchDog = Thread {
        while (!pluginDisabled) {
            if (System.currentTimeMillis() - lastTime > timeout) {
                println("[SakuraBind] detect server has not response over 60000")
                if (cacheManager?.status != Status.UNINITIALIZED) {
                    cacheManager?.close()
                    println("[SakuraBind] saved cache data!")
                }
                break
            }
            sleep(3000)
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
            baseCacheManager.init(cacheManager!!)
        }
        builder = null
        //容灾
        Runtime.getRuntime().addShutdownHook(hook)
        submit(period = 20) {
            lastTime = System.currentTimeMillis()
        }
        watchDog.isDaemon = true
        watchDog.start()
    }

    fun locationToString(location: Location): String =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

    @Throws(Exception::class)
    fun save() {
        for (baseCache in cacheManagerList) {
            baseCache.onSave()
        }
        cacheManager?.close()
        pluginDisabled = true
        Runtime.getRuntime().removeShutdownHook(hook)
    }

}