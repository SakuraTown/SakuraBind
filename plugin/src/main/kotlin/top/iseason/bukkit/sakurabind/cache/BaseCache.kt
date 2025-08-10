package top.iseason.bukkit.sakurabind.cache

import com.github.mgunlogson.cuckoofilter4j.CuckooFilter
import com.google.common.hash.Hashing
import org.ehcache.CacheManager
import org.ehcache.PersistentCacheManager
import org.ehcache.config.builders.CacheManagerBuilder
import top.iseason.bukkittemplate.debug.warn
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

abstract class BaseCache {

    private var rebuildFilter = false

    /**
     * 设置缓存
     */
    abstract fun setCache(builder: CacheManagerBuilder<PersistentCacheManager>): CacheManagerBuilder<PersistentCacheManager>

    /**
     * 初始化
     */
    open fun init(cacheManager: CacheManager) {
        if (rebuildFilter) {
            rebuildFilter = false
            reloadFilter()
        }
    }

    open fun reloadFilter() {}

    /**
     * 保存
     */
    abstract fun onSave()

    fun string2FilterKey(str: String): Long = Hashing.murmur3_128().hashUnencodedChars(str).asLong()

    fun loadFilter(file: File): CuckooFilter {
        return if (!file.exists())
            newFilter()
        else try {
            ObjectInputStream(file.inputStream()).use { it.readObject() as CuckooFilter }
        } catch (e: Exception) {
            rebuildFilter = true
            warn("error while loading filter $file , create new filter")
            e.printStackTrace()
            newFilter()
        }
    }


    fun saveFilter(file: File, filter: CuckooFilter) {
        if (!file.exists())
            file.createNewFile()
        ObjectOutputStream(file.outputStream()).use {
            it.writeObject(filter)
        }

    }

    open fun newFilter(): CuckooFilter = CuckooFilter
        .Builder(65536)
        .withFalsePositiveRate(0.0001)
        .withExpectedConcurrency(2)
        .build()
}