package top.iseason.bukkittemplate;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;

//注入器
public class ReflectionUtil {
    private static MethodHandle addUrlHandle;
    private static Object ucp;
    private static LinkedList<URL> urls = new LinkedList<>();
    private static LinkedList<URL> subUrls = new LinkedList<>();
    private static boolean isInit = false;
    private static Unsafe unsafe;

    /**
     * 初始化反射模块
     */
    public static void enable() {
        //通过反射获取ClassLoader addUrl 方法，因为涉及java17 无奈使用UnSafe方法
        isInit = true;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            //将子依赖填入插件classloader
            setUcpTarget(BukkitTemplate.class.getClassLoader());
            for (URL subUrl : subUrls) {
                addURL(subUrl);
            }
            setUcpTarget(BukkitTemplate.isolatedClassLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        clear();
    }

    public static void clear() {
        urls = null;
        subUrls = null;
    }

    private static void setUcpTarget(ClassLoader classLoader) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException {
        Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
        ucp = unsafe.getObject(classLoader, unsafe.objectFieldOffset(ucpField));
        Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(lookupField), unsafe.staticFieldOffset(lookupField));
        addUrlHandle = lookup.findVirtual(ucp.getClass(), "addURL", MethodType.methodType(void.class, URL.class));
    }

    /**
     * 将URl添加进插件的ClassLoader
     */
    public static synchronized void addURL(URL url) {
        try {
            if (!isInit)
                urls.add(url);
            else
                addUrlHandle.invoke(ucp, url);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将URl添加进插件的ClassLoader
     */
    public static synchronized void addSubURL(URL url) {
        try {
            if (!isInit)
                subUrls.add(url);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 强行替换 object 中的某一个变量
     *
     * @param target
     * @param field
     * @param value
     */
    public static void replaceObject(Object target, Field field, Object value) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(target, value);
        field.setAccessible(false);
//        unsafe.putObject(target, unsafe.objectFieldOffset(field), value);
    }

    public static URL[] getUrls() {
        return urls.toArray(new URL[0]);
    }

}

