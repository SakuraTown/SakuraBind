package top.iseason.bukkit.sakurabind.cache

import org.ehcache.PersistentCacheManager
import org.ehcache.config.builders.CacheManagerBuilder

interface BaseCache {
    /**
     * 设置缓存
     */
    fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager>

    /**
     * 初始化
     */
    fun init(cacheManager: org.ehcache.CacheManager)

    /**
     * 保存
     */
    fun onSave()
}