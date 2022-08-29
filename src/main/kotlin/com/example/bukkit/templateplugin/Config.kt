package com.example.bukkit.templateplugin

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {
    @Comment("")
    @Comment("测试")
    @Comment("测试2", "6666")
    @Key("test.test1.test2")
    var test = 0

    @Key
    @Comment("", "list 测试")
    var test2 = mutableListOf("12312", "asdfas46", "tew4q5t456wefg6s")

    @Key
    @Comment("", "set 测试")
    var set = mutableSetOf("asdasd", "asdasdas", "asdas")

    @Comment("", "map 测试")
    @Key
    var map = mutableMapOf("test" to mutableMapOf("1" to mutableMapOf('1' to 2), "2" to "2"), "test2" to 2)

    override val onLoaded: ConfigurationSection.() -> Unit = {
//        println("loaded")
    }
    override val onSaved: (ConfigurationSection.() -> Unit) = {
//        println("saved")
    }

}