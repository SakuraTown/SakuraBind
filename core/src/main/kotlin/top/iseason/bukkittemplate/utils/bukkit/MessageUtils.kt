@file:Suppress("UNUSED", "MemberVisibilityCanBePrivate")

package top.iseason.bukkittemplate.utils.bukkit

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.title.Title
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import top.iseason.bukkittemplate.BukkitTemplate
import top.iseason.bukkittemplate.DisableHook
import top.iseason.bukkittemplate.debug.warn
import top.iseason.bukkittemplate.dependency.DependencyDownloader
import top.iseason.bukkittemplate.hook.BungeeCordHook
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.other.submit
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * bukkit的消息相关工具
 */
object MessageUtils {
    private var miniMessageSupport = false
    private var miniMessageLoaded = false
    lateinit var audiences: BukkitAudiences
    private val HEX_PATTERN = Pattern.compile("#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})")
    private val hexColorSupport = try {
        net.md_5.bungee.api.ChatColor.of("#66ccff")
        true
    } catch (e: Throwable) {
        false
    }

    fun enableMiniMessage() {
        if (miniMessageSupport) return
        miniMessageSupport = true
        if (miniMessageLoaded) return
        val dd = DependencyDownloader()
            .addRepository("https://maven.aliyun.com/repository/public")
            .addRepository("https://repo.maven.apache.org/maven2/")
        dd.dependencies = mutableMapOf(
            "net.kyori:adventure-platform-bukkit:4.2.0" to 4,
            "net.kyori:adventure-text-minimessage:4.12.0" to 1
        )
        dd.start(true)
        audiences = BukkitAudiences.create(BukkitTemplate.getPlugin())
        miniMessageLoaded = true
        DisableHook.addTask {
            audiences.close()
        }
    }

    fun disableMiniMessage() {
        miniMessageSupport = false
    }


    /**
     * 默认消息前缀
     */
    var defaultPrefix = "&a[&6${BukkitTemplate.getPlugin().description.name}&a] &f"

    /**
     * 颜色代码正则
     */
    val colorPattern: Pattern = Pattern.compile("#[A-F|\\d]{6}", Pattern.CASE_INSENSITIVE)

    /**
     * 将String转为bukkit支持的颜色消息,从 bentobox 抄来的
     * 例子: &a你好、#66ccff 你好、#6cf 你好
     */
    fun String.toColor(): String {
        if (miniMessageSupport) return this
        if (!hexColorSupport) return ChatColor.translateAlternateColorCodes('&', this)
        val matcher: Matcher = HEX_PATTERN.matcher(this)
        // Increase buffer size by 32 like it is in bungee cord api. Use buffer because it is sync.
        val buffer = StringBuffer(length + 32)
        while (matcher.find()) {
            val group: String = matcher.group(1)
            if (group.length == 6) {
                // Parses #ffffff to a color text.
                matcher.appendReplacement(
                    buffer, ChatColor.COLOR_CHAR + "x"
                            + ChatColor.COLOR_CHAR + group[0] + ChatColor.COLOR_CHAR + group[1]
                            + ChatColor.COLOR_CHAR + group[2] + ChatColor.COLOR_CHAR + group[3]
                            + ChatColor.COLOR_CHAR + group[4] + ChatColor.COLOR_CHAR + group[5]
                )
            } else {
                // Parses #fff to a color text.
                matcher.appendReplacement(
                    buffer, ChatColor.COLOR_CHAR + "x"
                            + ChatColor.COLOR_CHAR + group[0] + ChatColor.COLOR_CHAR + group[0]
                            + ChatColor.COLOR_CHAR + group[1] + ChatColor.COLOR_CHAR + group[1]
                            + ChatColor.COLOR_CHAR + group[2] + ChatColor.COLOR_CHAR + group[2]
                )
            }
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString())
    }

    /**
     * String集合格式化颜色
     */
    fun Collection<String>.toColor() = map { it.toColor() }

    /**
     * 发送带颜色转换的消息,不输出null与空string
     */
    fun CommandSender.sendColorMessage(message: Any?, prefix: String = defaultPrefix) {
        val msg = message?.toString()
        if (msg.isNullOrEmpty()) return
        msg.split("\\n", "\n")
            .forEach { m ->
                //普通消息
                if (!m.startsWith('[')) {
                    sendMsg(PlaceHolderHook.setPlaceHolder("$prefix$m", this as? OfflinePlayer))
                    return@forEach
                }
                //特殊消息
                if (m.startsWith("[broadcast]", true)) {
                    broadcast(m.drop(11), prefix)
                    return@forEach
                }
                if (this is Player && m.startsWith("[actionbar]", true)) {
                    sendActionBar(m.drop(11), prefix)
                    return@forEach
                }
                if (this is Player && m.startsWith("[main-title]", true)) {
                    sendMainTitle(m.drop(12), prefix)
                    return@forEach
                }
                if (this is Player && m.startsWith("[sub-title]", true)) {
                    sendSubTitle(m.drop(11), prefix)
                    return@forEach
                }
                if (m.startsWith("[command]", true) ||
                    m.startsWith("[console]", true) ||
                    m.startsWith("[op-command]", true)
                ) {
                    val opCommand = m.startsWith("[op-command]", true)
                    val size = if (opCommand) 12 else 9
                    val command = PlaceHolderHook.setPlaceHolder(m.drop(size).trim(), this as? OfflinePlayer)
                    val sender = if (m.startsWith("[console]", true)) Bukkit.getConsoleSender() else this
                    val tempOP = opCommand && !sender.isOp
                    if (Bukkit.isPrimaryThread()) {
                        try {
                            if (tempOP) sender.isOp = true
                            Bukkit.dispatchCommand(sender, command)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        } finally {
                            if (tempOP) sender.isOp = false
                        }
                    } else {
                        submit {
                            try {
                                if (tempOP) sender.isOp = true
                                Bukkit.dispatchCommand(sender, command)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            } finally {
                                if (tempOP) sender.isOp = false
                            }
                        }
                    }
                    return@forEach
                }

            }

    }

    private fun CommandSender.sendMsg(msg: String) {
        if (miniMessageSupport)
            audiences.sender(this).sendMessage(MiniMessage.miniMessage().deserialize(msg))
        else sendMessage(msg)
    }

    /**
     * 发送带颜色转换的消息
     */
    fun CommandSender.sendColorMessages(vararg messages: Any?, prefix: String = defaultPrefix) =
        messages.forEach { sendColorMessage(it, prefix) }

    /**
     * 发送带颜色转换的消息
     */
    fun CommandSender.sendColorMessages(messages: Collection<Any?>?, prefix: String = defaultPrefix) =
        messages?.forEach { sendColorMessage(it, prefix) }

    /**
     * 发送带有前缀的消息,没有格式化颜色代码
     */
    fun CommandSender.sendMessage(message: Any?, prefix: String = defaultPrefix) {
        if (message == null || message.toString().isEmpty()) return
        sendMessage(prefix + message.toString())
    }

    /**
     * 进行颜色转换并发送给所有人
     */
    fun broadcast(message: Any?, prefix: String = defaultPrefix) {
        if (message == null || message.toString().isEmpty()) return
        val finalMessage = PlaceHolderHook.setPlaceHolder("$prefix$message", null)
        if (BungeeCordHook.bungeeCordEnabled) {
            if (miniMessageSupport) {
                val component = MiniMessage.miniMessage().deserialize(finalMessage)
                val serialize = GsonComponentSerializer.gson().serialize(component)
                BungeeCordHook.broadcastRaw(serialize)
            } else {
                BungeeCordHook.broadcast(finalMessage)
            }
        } else {
            if (miniMessageSupport) {
                val component = MiniMessage.miniMessage().deserialize(finalMessage)
                audiences.all().sendMessage(component)
            } else {
                Bukkit.broadcastMessage(finalMessage)
            }
        }
    }

    /**
     * 进行颜色转换并发送给控制台
     */
    fun sendConsole(message: Any?, prefix: String = defaultPrefix) =
        Bukkit.getConsoleSender().sendColorMessage(message, prefix)

    /**
     * 进行颜色转换并发送给控制台
     */
    fun sendConsole(messages: Collection<Any?>?, prefix: String = defaultPrefix) =
        messages?.forEach { sendConsole(it, prefix) }

    /**
     * 进行颜色转换并发送给控制台
     */
    fun sendConsole(messages: Array<Any?>?, prefix: String = defaultPrefix) =
        messages?.forEach { sendConsole(it, prefix) }

    /**
     * 去除字符串里的bukkit颜色代码
     */
    fun String.noColor(): String = ChatColor.stripColor(this)!!

    /**
     * 快速格式化字符串
     *
     * 例子: 你好啊 {0},欢迎来到 {1}
     *
     * 传入:  "Iseason", "我的世界"
     *
     * 结果: 你好啊 Iseason,欢迎来到 我的世界
     */
    fun String.formatBy(vararg values: Any?): String {
        var temp = this
        values.forEachIndexed { index, any ->
            if (any == null) return@forEachIndexed
            temp = temp.replace("{$index}", any.toString())
        }
        return temp
    }

    /**
     * 发送 actionbar 消息
     */
    fun Player.sendActionBar(message: String?, prefix: String = defaultPrefix) {
        if (message == null || message.toString().isEmpty()) return
        val finalMessage = PlaceHolderHook.setPlaceHolder("$prefix$message", this)
        try {
            if (miniMessageSupport) {
                val component = MiniMessage.miniMessage().deserialize(finalMessage)
                audiences.player(this).sendActionBar(component)
            } else {
                this.spigot()
                    .sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(finalMessage))
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            warn("该服务端版本不支持 ActionBar 消息!")
        }
    }

    /**
     * 发送 title 消息
     */
    fun Player.sendMainTitle(message: String?, prefix: String = defaultPrefix) {
        if (message == null || message.toString().isEmpty()) return
        val finalMessage = PlaceHolderHook.setPlaceHolder("$prefix$message", this)
        if (miniMessageSupport) {
            val component = MiniMessage.miniMessage().deserialize(finalMessage)
            audiences.player(this).showTitle(Title.title(component, Component.empty()))
        } else {
            this.sendTitle(finalMessage, "")
        }
    }

    /**
     * 发送 subtitle 消息
     */
    fun Player.sendSubTitle(message: String?, prefix: String = defaultPrefix) {
        if (message == null || message.toString().isEmpty()) return
        val finalMessage = PlaceHolderHook.setPlaceHolder("$prefix$message", this)
        if (miniMessageSupport) {
            val component = MiniMessage.miniMessage().deserialize(finalMessage)
            audiences.player(this).showTitle(Title.title(Component.empty(), component))
        } else {
            this.sendTitle("", finalMessage)
        }
    }
}

