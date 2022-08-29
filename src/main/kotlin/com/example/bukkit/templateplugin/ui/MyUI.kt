package com.example.bukkit.templateplugin.ui

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkit.bukkittemplate.ui.container.Pageable
import top.iseason.bukkit.bukkittemplate.ui.container.UIContainer
import top.iseason.bukkit.bukkittemplate.ui.slot.*
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessage
import top.iseason.bukkit.bukkittemplate.utils.toColor

class MyUI : ChestUI("${ChatColor.YELLOW}测试UI", row = 6, clickDelay = 500L), Pageable {

    init {
        setBackGround(Icon(ItemStack(Material.STONE), 0))
    }

    val messageButton = Button(
        ItemStack(Material.ANVIL).applyMeta {
            setDisplayName("${ChatColor.GREEN}按钮示例")
        },
    ).onClicked {
        it.whoClicked.sendColorMessage("&a 你点击了按钮")
    }.setup()

    val inputSlot = IOSlot(4, placeholder = ItemStack(Material.HOPPER))
        .inputFilter {
            it.type == Material.APPLE
        }.onInput {
            getViewers().lastOrNull()?.sendColorMessage("&a 放入了苹果")
            info("输入了苹果")
            messageButton.displayName = "&a强化苹果".toColor()
            messageButton.onClicked = {
                it.whoClicked.sendColorMessage("&a 你强化了苹果")
            }
        }.outputFilter {
            getViewers().lastOrNull()?.sendColorMessage("无法输出")
            false
        }.setup()

    override var container: UIContainer? = null
    override fun getUI() = this
}
