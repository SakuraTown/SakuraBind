package top.iseason.bukkittemplate;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import top.iseason.bukkittemplate.hook.BungeeCordHook;
import top.iseason.bukkittemplate.hook.PlaceHolderHook;

import java.util.concurrent.CompletableFuture;

/**
 * 插件启动类，由自定义 ClassLoader加载
 */
public class PluginBootStrap {
    private KotlinPlugin kotlinPlugin;
    private JavaPlugin javaPlugin;

    private PluginBootStrap() {
    }

    private void onLoad(Float ignore) {
        try {
            kotlinPlugin.onLoad();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    private void onEnable(Boolean ignore) {
        try {
            PlaceHolderHook.INSTANCE.checkHooked();
            BungeeCordHook.onEnable();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            kotlinPlugin.onEnable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        CompletableFuture.runAsync(this::onAsyncEnabled);
    }

    private void onDisable(Double ignore) {
        try {
            kotlinPlugin.onDisable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bukkit.getScheduler().cancelTasks(javaPlugin);
        HandlerList.unregisterAll(javaPlugin);
        DisableHook.disableAll();
        BungeeCordHook.onDisable();
    }

    private void onAsyncEnabled() {
        try {
            kotlinPlugin.onAsyncEnable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
