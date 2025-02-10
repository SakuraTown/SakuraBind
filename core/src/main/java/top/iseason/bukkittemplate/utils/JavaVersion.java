package top.iseason.bukkittemplate.utils;

public class JavaVersion {
    private static int version;

    static {
        String versionString = System.getProperty("java.version");
        int indexOf = versionString.indexOf('.');
        try {
            if (indexOf > 0) {
                String substring = versionString.substring(0, indexOf);
                if (substring.equals("1")) {
                    int indexOf1 = versionString.indexOf('.', indexOf + 1);
                    version = Integer.parseInt(versionString.substring(indexOf + 1, indexOf1));
                } else {
                    version = Integer.parseInt(substring);
                }
            } else {
                version = Integer.parseInt(versionString);
            }
        } catch (Exception e) {
            version = 8;
        }
    }

    public static boolean isGreaterOrEqual(int version) {
        return JavaVersion.version >= version;
    }
}
