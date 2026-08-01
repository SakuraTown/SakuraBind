package top.iseason.bukkit.sakurabind.utils;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Keeps the bStats constructor invocation descriptor aligned with the Java dependency.
 */
public final class MetricsFactory {
    private MetricsFactory() {
    }

    public static void create(JavaPlugin plugin, int serviceId) {
        new Metrics(plugin, serviceId);
    }
}
