package top.iseason.bukkit.bukkittemplate;

import java.util.ArrayList;

/**
 * 在插件 onDisable 时自动调用 onDisable() 方法
 */
public abstract class AutoDisable {
    private static final ArrayList<AutoDisable> closeables = new ArrayList<>();

    public AutoDisable() {
        closeables.add(this);
    }

    public static void disableAll() {
        for (AutoDisable closeable : closeables) {
            closeable.onDisable();
        }
    }

    public abstract void onDisable();
}
