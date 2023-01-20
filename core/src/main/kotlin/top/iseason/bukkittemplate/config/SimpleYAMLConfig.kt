package top.iseason.bukkittemplate.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.utils.other.submit
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.util.*


/**
 * 一个简单的支持自动重载的配置类，不建议作为数据储存用
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
open class SimpleYAMLConfig(
    /**
     * 默认配置路径，以.yml结尾，覆盖@FilePath
     */
    val defaultPath: String? = null,
    /**
     * 是否自动重载
     */
    var isAutoUpdate: Boolean = true,
    /**
     * 重载是否提示,如果你要自定义提示请关闭
     */
    var updateNotify: Boolean = true
) {

    /**
     * 更新时间
     */
    var updateTime = 0L

    /**
     * 配置文件路径
     */
    val configPath = getPath().apply {
        if (!exists()) {
            parentFile.mkdirs()
            createNewFile()
        }
    }

    /**
     * 配置对象,修改并不会生效，只能直接修改成员
     */
    var config: ConfigurationSection = YamlConfiguration()
        private set

    /**
     * 直接保存ConfigurationSection到文件中,而不是从属性读取
     */
    fun saveYaml() {
        (config as YamlConfiguration).save(configPath)
    }

    private val keys = mutableListOf<ConfigKey>().also { list ->
        //判断是否全为键值
        if (this@SimpleYAMLConfig.javaClass.getAnnotation(Key::class.java) != null) {
            this.javaClass.declaredFields.forEach {
//                if ("INSTANCE" == it.name) return@forEach
                if (Modifier.isFinal(it.modifiers)) {
                    return@forEach
                }
                val comments = mutableListOf<String>()
                it.getAnnotationsByType(Comment::class.java).forEach { an ->
                    //注释内容遍历
                    an.value.forEach { value ->
                        comments.add(value)
                    }
                }
                list.add(ConfigKey(it.name.replace("__", ".").replace('_', '-'), it, comments))
            }
            return@also
        }
        getAllFields().forEach {
            if (Modifier.isFinal(it.modifiers)) {
                return@forEach
            }
            val keyAnnotation = it.getAnnotation(Key::class.java) ?: return@forEach
            val key = keyAnnotation.key.ifEmpty { it.name.replace("__", ".").replace('_', '-') }
            val comments = mutableListOf<String>()
            it.getAnnotationsByType(Comment::class.java).forEach { an ->
                //注释内容遍历
                an.value.forEach { value ->
                    comments.add(value)
                }
            }
            it.isAccessible = true
//            it.isAccessible = true
//            println(it.get(it))
            list.add(ConfigKey(key, it, if (comments.isEmpty()) null else comments))
        }
    }

    init {
        if (isAutoUpdate) {
            try {
                ConfigWatcher.fromFile(configPath.absoluteFile)
            } catch (e: Exception) {
                warn("file watch Service error. Automatic updates will been closed!")
                isAutoUpdate = false
            }
        }
        configs[configPath.absolutePath] = this
    }

    /**
     * 设置重载或者保存时是否提醒
     */
    fun setUpdate(enable: Boolean) {
        isAutoUpdate = enable
    }

    /**
     * 将文件路径转化为文件系统标准
     */
    private fun normalizeFileStr(file: String) = file.replace('\\', File.separatorChar).replace('/', File.separatorChar)

    private fun getPath(): File {
        val dataFolder = BukkitTemplate.getPlugin().dataFolder
        if (defaultPath != null) return File(dataFolder, normalizeFileStr(defaultPath)).absoluteFile
        val annotation = this::class.java.getAnnotation(FilePath::class.java)
        require(annotation != null) { "path must not null" }
        return File(dataFolder, normalizeFileStr(annotation.path)).absoluteFile
    }

    /**
     * 异步保存配置
     */
    fun saveAsync(notify: Boolean = updateNotify) {
        submit(async = true) {
            save(notify)
        }
    }

    /**
     * 保存配置
     */
    fun save(notify: Boolean = updateNotify) {
        update(false)
        try {
            onSaved(config)
        } catch (_: Exception) {
        }
        if (notify)
            info("Config $configPath was saved!")
    }

    /**
     * 从文件异步加载配置
     */
    fun loadAsync(notify: Boolean = updateNotify) {
        submit(async = true) {
            load(notify)
        }
    }

    /**
     * 从文件加载配置
     */
    fun load(notify: Boolean = updateNotify) {
        if (!update(true)) {
            return
        }
        try {
            onLoaded(config)
        } catch (_: Exception) {
        }
        if (notify)
            info(notifyMessage.format(configPath.name))
    }

    /**
     * 更新配置
     * @param isReadOnly 是否只读
     * @return 更新成功返回true
     */
    private fun update(isReadOnly: Boolean): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - updateTime < 2000L) return false
        updateTime = currentTimeMillis
//        sleep(300L)
        val loadConfiguration = YamlConfiguration.loadConfiguration(configPath)
        val temp = YamlConfiguration()
        val commentMap = hashMapOf<String, String>()
        //缺了键补上
        var incomplete = false
        keys.forEach { key ->
            //获取并设置注释
            val keyName = key.key
            val anotherName = if (keyName.endsWith('@')) keyName.substring(0, keyName.length - 1) else "$keyName@"
            val finalKey = if (loadConfiguration.contains(anotherName)) anotherName else keyName
            if (isReadOnly) {
                var value = loadConfiguration.get(finalKey)
                if (Map::class.java.isAssignableFrom(key.field.type) && value != null) {
                    value = (value as MemorySection).getValues(false)
                } else if (Set::class.java.isAssignableFrom(key.field.type) && value != null) {
                    value = loadConfiguration.getList(finalKey)?.toSet()
                }
                if (value != null) {
                    //获取修改的键值
                    try {
                        key.setValue(this, value)
                    } catch (e: Exception) {
                        debug("Loading config $configPath error! key:${finalKey} value: $value")
                    }
                } else {
                    incomplete = true
                }
            }
            if (!(!incomplete && isReadOnly)) {
                val comments = key.comments
                if (comments != null) {
                    for (str in comments) {
                        val noPathKey = finalKey.substring(finalKey.lastIndexOf('.') + 1)
                        //注释识别标识
                        val random = "comment-${UUID.randomUUID()}"
                        //传入注释内容，待转换
                        commentMap["$noPathKey-$random"] = "# $str"
                        //将注释当作键值写入配置文件
                        val s = "${finalKey}-$random"
                        temp.set(s, "")
                    }
                }
            }
            //将数据写入临时配置
            try {
                var value = key.getValue(this)
                if (value is Set<*>) {
                    value = value.toList()
                }
                temp.set(finalKey, value)
            } catch (e: Exception) {
                debug("setting config $configPath error! key:${finalKey}")
            }
        }
        if (!(!incomplete && isReadOnly) || !configPath.exists()) {
            //保存临时配置，此时注释尚未转换
            temp.save(configPath)
            //转换注释
            commentFile(configPath, commentMap)
        }
        config = YamlConfiguration.loadConfiguration(configPath)
        return true
    }

    /**
     * 配置读取完毕之后的回调
     */
    open fun onLoaded(section: ConfigurationSection) {

    }

    /**
     * 配置保存完毕之后的回调
     */
    open fun onSaved(section: ConfigurationSection) {

    }

    /**
     * 转换配置文件的注释
     */
    private fun commentFile(file: File, commentMap: Map<String, String>) {
        // 创建临时文件
        val commentedFile = File(file.path + ".tmp")
        val readLines = file.readLines()
        val newFile: MutableList<String> = ArrayList(readLines.size)
        //逐行扫描,匹配注释并替换
        readLines.forEach {
            var nextLine: String = it
            val trim = nextLine.trim()
            val key = trim.substring(0, trim.length - 4)
            val comment = commentMap[key]
            // 是注释
            if (comment != null) {
                nextLine = if (comment.trim() == "#") ""
                else nextLine.substring(0, nextLine.indexOf(key)) + comment
                newFile.add(nextLine)
                return@forEach
            }
            // 是旧注释
            if (key.length > 36) {
                val uuid = key.substring(key.length - 36, key.length)
                try {
                    UUID.fromString(uuid)
                    return@forEach
                } catch (_: Exception) {
                }
            }
            newFile.add(nextLine)
        }
        //写入数据到临时文件
        Files.write(commentedFile.toPath(), newFile)
//        commentedFile.readLines().forEach { println(it) }
        //复制替换
        copyFileUsingStream(commentedFile, file)
        //删除临时文件
        Files.delete(commentedFile.toPath())

    }

    /**
     * 复制文件内容
     */
    @Throws(IOException::class)
    private fun copyFileUsingStream(source: File, dest: File) {
        FileInputStream(source).use { fis ->
            FileOutputStream(dest).use { fos ->
                val buffer = ByteArray(1024)
                var length: Int
                while (fis.read(buffer).also { length = it } > 0) {
                    fos.write(buffer, 0, length)
                }
            }
        }
    }

    private fun getAllFields(): List<Field> {
        val fields = mutableListOf<Field>()
        var superClass: Class<*> = this::class.java
        while (superClass != SimpleYAMLConfig::class.java) {
            fields.addAll(0, listOf(*superClass.declaredFields))
            superClass = superClass.superclass
        }
        return fields
    }

    companion object {
        //监听器列表
        val configs = mutableMapOf<String, SimpleYAMLConfig>()

        //重载提示信息
        var notifyMessage: String = "Config %s was reloaded!"
    }
}
