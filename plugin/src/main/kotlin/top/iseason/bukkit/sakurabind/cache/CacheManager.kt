@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import org.bukkit.Bukkit
import org.bukkit.Location
import org.ehcache.CacheManager
import org.ehcache.PersistentCacheManager
import org.ehcache.Status
import org.ehcache.config.builders.*
import top.iseason.bukkit.sakurabind.config.Config
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.utils.other.submit
import java.io.File
import java.lang.Thread.sleep


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
            .withClassLoader(this.javaClass.classLoader)
    }

    private var lastTime = System.currentTimeMillis()
    private val timeout = Config.thread_dump_protection__timeout * 1000
    private var pluginDisabled = false

    private var hook: Thread? = null

    private var watchDog: Thread? = null

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
        if (!Config.thread_dump_protection__enable) return
        hook = Thread {
            if (cacheManager?.status != Status.UNINITIALIZED) {
                cacheManager?.close()
                println("[SakuraBind] shutdown hook for encache has finished!")
            }
        }
        watchDog = Thread {
            while (!pluginDisabled) {
                if (System.currentTimeMillis() - lastTime > timeout) {
                    println("[SakuraBind] detect server has not response over $timeout seconds")
                    if (cacheManager?.status != Status.UNINITIALIZED) {
                        cacheManager?.close()
                        println("[SakuraBind] saved cache data!")
                    }
                    if (Config.thread_dump_protection__disable_plugin) {
                        Bukkit.getPluginManager().disablePlugin(BukkitTemplate.getPlugin())
                    }
                    if (Config.thread_dump_protection__stop_server) {
                        Bukkit.getServer().shutdown()
                    }
                    break
                }
                sleep(3000)
            }
        }
        //容灾
        Runtime.getRuntime().addShutdownHook(hook)
        submit(period = 20) {
            lastTime = System.currentTimeMillis()
        }
        watchDog!!.isDaemon = true
        watchDog!!.start()
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
        try {
            watchDog?.interrupt()
            if (hook != null)
                Runtime.getRuntime().removeShutdownHook(hook)
        } catch (_: Throwable) {
        }

    }

}