package top.iseason.bukkittemplate.dependency;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import top.iseason.bukkittemplate.BukkitTemplate;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginDependency {

    /**
     * 解析下载plugin.yml中的依赖
     */
    public static boolean parsePluginYml() {
        YamlConfiguration yml = null;
        // 为什么不用 classloader 的 getResource呢，因为某些sb系统或者服务端会乱改
        // 导致 getResource 的内容错误, 已测试 Debian + CatServer
        String location = PluginDependency.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try (JarFile jarFile = new JarFile(URLDecoder.decode(location, "UTF-8"), false)) {
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            InputStream resource = jarFile.getInputStream(entry);
            yml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (yml == null) return true;
        ConfigurationSection libConfigs = yml.getConfigurationSection("runtime-libraries");
        if (libConfigs == null) return true;
        DependencyDownloader dd = new DependencyDownloader();
        String folder = libConfigs.getString("libraries-folder");
        if (folder != null) {
            File parent;
            if (folder.toLowerCase().startsWith("@plugin:")) {
                parent = new File(BukkitTemplate.getPlugin().getDataFolder(), folder.substring(8));
            } else {
                parent = new File(folder);
            }
            DependencyDownloader.parent = parent;
        }
        List<String> repositories = libConfigs.getStringList("repositories");
        if (!repositories.isEmpty()) {
            dd.repositories.clear();
            for (String repository : repositories) {
                dd.addRepository(repository);
            }
        }
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        for (String library : libConfigs.getStringList("libraries")) {
            String[] split = library.split(",");
            if (split.length == 1) {
                map.put(library, 2);
            } else if (split.length == 2) {
                map.put(split[0], Integer.parseInt(split[1]));
            }
            String substring = library.substring(0, library.lastIndexOf(":"));
            DependencyDownloader.parallel.add(substring);
        }
        dd.dependencies = map;
        DependencyDownloader.assembly.addAll(libConfigs.getStringList("assembly"));
        DependencyDownloader.exists.addAll(libConfigs.getStringList("excludes"));
        return dd.start(libConfigs.getBoolean("parallel", false));
    }
}
