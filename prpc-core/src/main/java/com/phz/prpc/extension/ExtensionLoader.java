package com.phz.prpc.extension;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <p>
 * 自己实现一个扩展类加载器辅助类
 * ，区别于{@code JDK}的{@code SPI}机制，我们预定好在 {@code jar} 包的 {@code META-INF/extensions} 目录下方存放扩展类文件，文件内容就为第三方实现的全路径
 * </p>
 *
 * @author PengHuanZhi
 * @date 2022年01月16日 17:56
 */
@Slf4j
@Data
public final class ExtensionLoader<T> {
    /**
     * 约定第三方实现配置文件目录
     **/
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";

    /**
     * 接口的类型，用于获取此接口下的第三方实现
     **/
    private final Class<?> type;

    /**
     * 通过接口的{@link Class}对象获取其第三方实现类的加载器
     *
     * @param type 接口的类型
     * @return ExtensionLoader<T> 返回一个指定接口类型的类加载器辅助类
     **/
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Spi需要知道你想要找到哪个功能的第三方实现！");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("只支持寻找接口类型的第三方实现！");
        }
        if (type.getAnnotation(Spi.class) == null) {
            throw new IllegalArgumentException("目标接口必须被@Spi注解标注！");
        }
        return new ExtensionLoader<>(type);
    }

    /**
     * 获取这个接口指定名称的第三方实现对象
     *
     * @return T 返回目标实现
     **/
    public T getExtension() {
        // 加载到一个第三方实现
        Class<T> clazz = loadExtensionFile();
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("实例化失败 : " + clazz);
        }
    }

    /**
     * 加载约定好的目录下方的名称为接口全路径的扩展文件
     *
     * @return Class<T> 返回目标第三方实现的{@link Class}对象
     **/
    private Class<T> loadExtensionFile() {
        //想要获取谁的实现类
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            urls = classLoader.getResources(fileName);
            if (urls != null) {
                URL resourceUrl = urls.nextElement();
                return loadResource(classLoader, resourceUrl);
            }
            return null;
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * 读取扩展文件的内容，找到第三方实现的全路径，并获得其{@link Class}对象
     *
     * @param classLoader 扩展类加载器辅助类的类加载器
     * @param resourceUrl 文件在资源{@code URL}
     * @return Class<T> 返回目标{@link Class}对象
     **/
    @SuppressWarnings("unchecked")
    private Class<T> loadResource(ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 可能是注释
                final int ci = line.indexOf('#');
                //如果是第一个位置，则这一行都可以不用解析了
                if (ci == 0) {
                    continue;
                } else if (ci > 0) {
                    //如果非第一个位置，需要将注释前面的内容取出来，也就是将注释后面的内容截取
                    line = line.substring(0, ci);
                }
                return (Class<T>) classLoader.loadClass(line.trim());
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error(e.getMessage());
            return null;
        }
        return null;
    }
}