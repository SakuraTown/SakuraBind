package top.iseason.bukkittemplate.config

import top.iseason.bukkittemplate.DisableHook
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService

/**
 * 配置文件监听器，负责实现配置修改之后的自动重载
 */
class ConfigWatcher private constructor(private val folder: File) : Thread() {
    private var isEnable = true
    private var service: WatchService = FileSystems.getDefault().newWatchService()
        .apply { folder.toPath().register(this, StandardWatchEventKinds.ENTRY_MODIFY) }

    /**
     * 自动重载的实现方法
     */
    override fun run() {
        while (isEnable) {
            val key = try {
                service.take()
            } catch (e: Exception) {
                cancel()
                break
            }
            //监听文件修改
            event@ for (pollEvent in key.pollEvents()) {
                if (pollEvent.kind() != StandardWatchEventKinds.ENTRY_MODIFY) continue
                val path = pollEvent.context() as Path
                if (!path.toString().endsWith(".yml")) break@event
                val absolutePath = "${folder}${File.separatorChar}$path"
                val simpleYAMLConfig = SimpleYAMLConfig.configs[absolutePath] ?: continue
                if (simpleYAMLConfig.isAutoUpdate) {
                    sleep(200)
                    simpleYAMLConfig.load()
                    sleep(300)
                    service.close()
                    service = FileSystems.getDefault().newWatchService()
                        .apply { folder.toPath().register(this, StandardWatchEventKinds.ENTRY_MODIFY) }
                    break@event
                }
            }
            key.reset()
        }
    }

    /**
     * 取消本监听器
     */
    fun cancel() {
        if (!isEnable) return
        isEnable = false
        service.close()
        interrupt()
    }

    companion object {
        private val folders = mutableMapOf<String, ConfigWatcher>()

        init {
            DisableHook.addTask {
                onDisable()
            }
        }

        /**
         * 监听某个文件夹，如果存在则返回该监听器
         */
        fun fromFile(file: File): ConfigWatcher {
            val parentFile = file.absoluteFile.parentFile
            val folder = parentFile.toString()
            val existWatcher = folders[folder]
            if (existWatcher != null) return existWatcher
            val configWatcher = ConfigWatcher(parentFile)
            folders[folder] = configWatcher
            configWatcher.isDaemon = true
            configWatcher.name = "File Watch for $folder"
            configWatcher.start()
            return configWatcher
        }

        private fun onDisable() {
            for (v in folders.values) {
                try {
                    v.cancel()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            folders.clear()
        }
    }
}