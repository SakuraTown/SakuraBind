package com.example.bukkit.templateplugin

import top.iseason.bukkit.bukkittemplate.KotlinPlugin
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.debug.info

object TemplatePlugin : KotlinPlugin() {

    override fun onAsyncLoad() {
    }

    override fun onEnable() {

//        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        info("&a插件已启用!")
    }

    override fun onAsyncEnable() {
        //命令
//        command1()
//        openUICommand()
//        command1()

        //如果使用命令模块，取消注释
//        CommandBuilder.updateCommands()
        //如果使用UI模块,取消注释
//        registerListeners(UIListener)

        //使用数据库请取消注释以下2行
        DatabaseConfig.load(false)
        DatabaseConfig.initTables()

//        SimpleYAMLConfig.notifyMessage = "&7配置文件 &6%s &7已重载!"
//        Config.load(false)
//        LagCatcher.performanceCheck("test", 0) {
//            DependencyDownloader().addRepositories("https://maven.aliyun.com/repository/public")
//                .downloadDependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
//        }
    }

    override fun onDisable() {
        info("&6插件已卸载! ")
    }

}