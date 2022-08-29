package com.example.bukkit.templateplugin.ui

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.ui.container.UIContainer

@FilePath("mymultiui.yml")
object MyMultiUIConfig : SimpleYAMLConfig() {
    val multiUI = MultiUI()
    var myUI: UIContainer? = null
    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        myUI = multiUI.deserialize(config)
    }
}