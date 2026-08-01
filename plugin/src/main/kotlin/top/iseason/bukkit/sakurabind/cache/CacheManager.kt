@file:Suppress("UnstableApiUsage")

package top.iseason.bukkit.sakurabind.cache

import org.bukkit.Bukkit
import org.bukkit.Location
import org.ehcache.CacheManager
import org.ehcache.PersistentCacheManager
import org.ehcache.Status
import org.ehcache.config.builders.CacheManagerBuilder
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

    @Volatile
    private var lastTime = 0L
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
        for (baseCacheManager in cacheManagerList) {
            baseCacheManager.beforeManagerBuild()
        }
        cacheManager = builder!!.build(true)
        for (baseCacheManager in cacheManagerList) {
            baseCacheManager.init(cacheManager!!)
        }
        builder = null
        if (!Config.thread_dump_protection__enable) return
        hook = Thread {
            if (closeCacheManager()) {
                println("[SakuraBind] shutdown hook for encache has finished!")
            }
        }
        watchDog = Thread {
            while (!pluginDisabled) {
                if (lastTime != 0L && System.currentTimeMillis() - lastTime > timeout) {
                    println("[SakuraBind] detect server has not response over $timeout millis")
                    if (closeCacheManager()) {
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
                try {
                    sleep(1000)
                } catch (_: InterruptedException) {
                }
            }
        }
        //容灾
        Runtime.getRuntime().addShutdownHook(hook)
        submit(delay = Config.thread_dump_protection__delay * 20L, period = 20) {
            lastTime = System.currentTimeMillis()
        }
        watchDog!!.isDaemon = true
        watchDog!!.start()
    }

    inline fun locationToString(location: Location): String =
        "${location.world?.name},${location.blockX},${location.blockY},${location.blockZ}"

    @Throws(Exception::class)
    fun save() {
        pluginDisabled = true
        for (baseCache in cacheManagerList) {
            baseCache.onSave()
        }
        closeCacheManager()
        try {
            watchDog?.interrupt()
            if (hook != null)
                Runtime.getRuntime().removeShutdownHook(hook)
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    private fun closeCacheManager(): Boolean {
        val manager = cacheManager ?: return false
        if (manager.status == Status.UNINITIALIZED) return false
        manager.close()
        for (baseCache in cacheManagerList) {
            baseCache.onClosed()
        }
        return true
    }

}
