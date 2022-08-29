package com.example.bukkit.templateplugin.ui


import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkit.bukkittemplate.ui.container.UIContainer
import top.iseason.bukkit.bukkittemplate.ui.slot.Button
import top.iseason.bukkit.bukkittemplate.ui.slot.getContainer
import top.iseason.bukkit.bukkittemplate.ui.slot.onClicked
import top.iseason.bukkit.bukkittemplate.ui.slot.serializeId
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessage
import top.iseason.bukkit.bukkittemplate.utils.toColor


class MultiUI : UIContainer(arrayOf(Page1(), Page2(), Page3())) {
    override var onPageChanged: ((from: Int, to: Int) -> Unit)? = { from, to ->
//        info("from $from to $to")
    }
}

class Page1 : ChestUI("page1" + System.currentTimeMillis()) {
    val button = Button(ItemStack(Material.GREEN_WOOL), 4).onClicked {
        it.whoClicked.sendColorMessage("&a当前是第一页")
    }.setup().serializeId("current")
    val last = Button(ItemStack(Material.PUMPKIN).applyMeta { setDisplayName("&c上一页".toColor()) }, 0).onClicked {
        getContainer()?.lastPage(it.whoClicked)
    }.setup().serializeId("last")
    val next = Button(ItemStack(Material.MELON).applyMeta { setDisplayName("&a下一页".toColor()) }, 8).onClicked {
        getContainer()?.nextPage(it.whoClicked)
    }.setup().serializeId("next")
    override var serializeId: String = "page1"
}

class Page2 : ChestUI("page2" + System.currentTimeMillis()) {
    val button = Button(ItemStack(Material.GREEN_WOOL), 4).onClicked {
        it.whoClicked.sendColorMessage("&a当前是第二页")
    }.setup().serializeId("current")
    val last = Button(ItemStack(Material.PUMPKIN).applyMeta { setDisplayName("&c上一页".toColor()) }, 0).onClicked {
        getContainer()?.lastPage(it.whoClicked)
    }.setup().serializeId("last")
    val next = Button(ItemStack(Material.MELON).applyMeta { setDisplayName("&a下一页".toColor()) }, 8).onClicked {
        getContainer()?.nextPage(it.whoClicked)
    }.setup().serializeId("next")
    override var serializeId: String = "page2"
}

class Page3 : ChestUI("page3" + System.currentTimeMillis()) {
    val button = Button(ItemStack(Material.GREEN_WOOL), 4).onClicked {
        it.whoClicked.sendColorMessage("&a当前是第三页")
    }.setup().serializeId("current")
    val last = Button(ItemStack(Material.PUMPKIN).applyMeta { setDisplayName("&c上一页".toColor()) }, 0).onClicked {
        getContainer()?.lastPage(it.whoClicked)
    }.setup().serializeId("last")
    val next = Button(ItemStack(Material.MELON).applyMeta { setDisplayName("&a下一页".toColor()) }, 8).onClicked {
        getContainer()?.nextPage(it.whoClicked)
    }.setup().serializeId("next")
    override var serializeId: String = "page3"
}
