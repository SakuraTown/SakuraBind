package top.iseason.bukkit.sakurabind.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakurabind.SakuraBind
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.CommandNodeExecutor
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import java.io.File

object ReloadCommand : CommandNode(
    name = "reload",
    description = "重载配置",
    default = PermissionDefault.OP,
    async = true,
    params = listOf(
        Param(
            "[file]",
            suggest = SimpleYAMLConfig.configs.keys.map { it.substring(BukkitTemplate.getPlugin().dataFolder.absolutePath.length + 1) })
    ),
) {

    override var onExecute: CommandNodeExecutor? = CommandNodeExecutor { params, _ ->
        var configName = params.nextOrNull<String>()
        if (configName == null) {
            DatabaseConfig.closeDB()
            SakuraBind.initConfig()
            return@CommandNodeExecutor
        }
        val key = BukkitTemplate.getPlugin().dataFolder.absolutePath + File.separatorChar + configName
        val config = SimpleYAMLConfig.configs[key] ?: throw ParmaException("配置 $configName 不存在!")
        config.load(!params.hasParma("-silent"))
    }

}
