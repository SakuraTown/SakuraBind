package com.example.bukkit.templateplugin

import com.example.bukkit.templateplugin.ui.*
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import top.iseason.bukkit.bukkittemplate.command.*
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.ui.openPageableUI
import top.iseason.bukkit.bukkittemplate.ui.openUI
import top.iseason.bukkit.bukkittemplate.utils.sendMessage

fun command1() {
    commandRoot("playerutil", alias = arrayOf("test1", "test2"), description = "测试命令1") {
        node(
            "potion",
            description = "玩家药水控制",
            default = PermissionDefault.OP,
            isPlayerOnly = true,
            params = arrayOf(
                Param("<操作>", listOf("add", "set", "remove")),
                Param("<药水类型>", ParamSuggestCache.potionTypes),
                Param("[玩家]", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[等级]", listOf("0", "1", "2", "3", "4")),
                Param("[秒]", listOf("1", "5", "10"))
            )
        ) {
            onExecute {
                val operation = getParam<String>(0)
                if (operation !in setOf("add", "set", "remove"))
                    throw ParmaException("&7参数 &c${operation}&7 不是一个有效的操作,支持的有: add、set、remove")
                val type = getParam<PotionEffectType>(1)
                var player = getOptionalParam<Player>(2)
                var reduce = 0
                if (player == null) {
                    player = it as Player
                    reduce++
                }
                var level = getOptionalParam<Int>(3 - reduce)
                if (level == null) {
                    level = 0
                    reduce++
                }
                var time = ((getOptionalParam<Double>(4 - reduce) ?: 10.0) * 20.0).toInt()

                when (operation) {
                    "add" -> {
                        val potionEffect = player.getPotionEffect(type)
                        if (potionEffect != null) time += potionEffect.duration
                        player.addPotionEffect(PotionEffect(type, time, level))
                    }

                    "set" -> {
                        player.removePotionEffect(type)
                        player.addPotionEffect(PotionEffect(type, time, level))
                    }

                    else -> {
                        player.removePotionEffect(type)
                    }
                }
                true
            }
            onSuccess("${SimpleLogger.prefix}&a命令已执行!")
        }

        node("other", default = PermissionDefault.OP, description = "测试节点2") {
            node("test3", alias = arrayOf("test1", "test2"), description = "测试命令") {
                onExecute {
                    true
                }
            }
        }
    }
}

fun command2() {
    commandRoot(
        "2node",
        alias = arrayOf("node2", "node3"),
        default = PermissionDefault.OP,
        async = true,
        description = "测试命令2",
        params = arrayOf(
            Param("<玩家>", suggestRuntime = ParamSuggestCache.playerParam),
            Param("[数字]", listOf("1", "5", "10", "-5", "-1"))
        )
    ).onExecute {
        val param1 = getParam<Int>(0)
        val param2 = getOptionalParam<Double>(1)
        it.sendMessage(param1)
        it.sendMessage(param2)
        true
    }
}

fun command3() {
    TestNode.registerAsRoot()
}

fun command4() {
    commandRoot("testcommand") {
        node("node1") {
            node("node2") {
                testNode()
            }
        }
        node("node4")
        node("node5")
    }
}

fun openUICommand() {
    commandRoot("openUI", isPlayerOnly = true, async = true) {
        node("UI", isPlayerOnly = true).onExecute {
            (it as Player).openUI<MyUI> {
                title = it.displayName
            }
            true
        }
        node("UISer", isPlayerOnly = true).onExecute {
            (it as Player).openInventory(MyUIConfig.myUI?.clone()?.build() ?: return@onExecute true)
            true
        }
        node("multiUISer", isPlayerOnly = true).onExecute {
            MyMultiUIConfig.myUI?.clone()?.openFor((it as Player))
            true
        }
        node("saveUISer", isPlayerOnly = true, async = true).onExecute {
            MyUISer.serialize(MyUIConfig.config)
            (MyUIConfig.config as YamlConfiguration).save(MyUIConfig.configPath)
            true
        }
        node("saveMultiUISer", isPlayerOnly = true, async = true).onExecute {
            MyMultiUIConfig.multiUI.serialize(MyMultiUIConfig.config)
            (MyMultiUIConfig.config as YamlConfiguration).save(MyMultiUIConfig.configPath)
            true
        }
        node("MultiUI", isPlayerOnly = true).onExecute {
            (it as Player).openPageableUI<MultiUI>()
            true
        }
    }

}

/**
 * 结构命令
 */
fun CommandBuilder.testNode() = node("node3") {
    onExecute {
        it.sendMessage("hello")
        true
    }
}

object TestNode : CommandNode(
    "testnode",
    alias = arrayOf("testnode2", "testnode3"),
    default = PermissionDefault.OP,
    async = true,
    description = "测试命令-单独类",
    params = arrayOf(
        Param("<玩家>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("[金钱]", listOf("1", "5", "10", "-5", "-1"))
    )
) {
    init {
        onExecute = {
            val player = getParam<Player>(0)
            val money = getOptionalParam<Double>(1)
            player.sendMessage(money.toString())
            true
        }
    }
}

