package com.zjh.zzone.iot.mqtt.support;

import com.ylg.iot.message.ProtocolSupportDefinition;
import com.ylg.iot.protocol.DeviceMessageCodec;
import com.ylg.iot.protocol.loader.ProtocolClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 使用自定义ClassLoader加载jar包，并获取编解码器实例
 *
 * @author zjh
 * @since 2025-01-02 19:47
 */
public class JarProtocolSupportLoader implements ProtocolSupportLoader {
    private static final Logger log = LoggerFactory.getLogger(JarProtocolSupportLoader.class);
    /**
     * 类加载器实例
     */
    private final Map<Long, ProtocolClassLoader> protocolLoaders = new ConcurrentHashMap<>();

    public JarProtocolSupportLoader() {
    }

    /**
     * 创建类加载器实例
     * @param location jar包路径
     * @return 类加载器实例
     */
    protected ProtocolClassLoader createClassLoader(URL location) {
        return new ProtocolClassLoader(new URL[]{location}, this.getClass().getClassLoader());
    }

    /**
     * 关闭所有类加载器实例
     */
    protected void closeAll() {
        this.protocolLoaders.keySet().forEach(this::closeLoader);
        this.protocolLoaders.clear();
    }

    /**
     * 关闭类加载器实例并卸载其加载的所有类
     */
    @Override
    public void closeLoader(Long id) {
        try {
            protocolLoaders.get(id).close();
        } catch (Throwable throwable) {
            log.error("close loader error", throwable);
        }
    }

    @Override
    public ProtocolSupport load(ProtocolSupportDefinition definition) {
        try {
            Long id = definition.getId();
            String location = (String) Optional.ofNullable(definition.getLocalLocation()).map(String::valueOf).orElseThrow(() -> new IllegalArgumentException("location"));
            URL url;
            if (!location.contains("://")) {
                url = (new File(location)).toURI().toURL();
            } else {
                url = new URL("jar:" + location + "!/");
            }

            URL fLocation = url;
            ProtocolClassLoader loader = this.protocolLoaders.compute(id, (key, old) -> {
                if (null != old) {
                    try {
                        this.closeLoader(key);
                    } catch (Exception ignored) {
                    }
                }
                return this.createClassLoader(fLocation);
            });
            log.debug("加载协议完成: {}", location);
            DefaultProtocolSupport support = new DefaultProtocolSupport(definition);
            List<DeviceMessageCodec> stringDeviceMessageCodecMap = extractClassesFromJar(loader, new File(location), DeviceMessageCodec.class);
            support.addMessageCodecSupports(stringDeviceMessageCodecMap);
            return support;

        } catch (Exception exception) {
            log.error("加载协议失败: {}", definition.getId(), exception);
        }
        return getDefaultProtocolSupport();
    }

    /**
     * 获取默认编解码器
     * @return 默认编解码器
     */
    public ProtocolSupport getDefaultProtocolSupport() {
        // TODO 新建一系列默认编解码器，在找不到编解码器时使用
        return null;
    }

    /**
     * 从 JAR 包中提取指定类型的类
     * @param classLoader 类加载器
     * @param jarFile JAR 文件
     * @param type 类型
     * @param <T> 类型
     * @return 类列表
     * @throws Exception 异常
     */
    public static <T> List<T> extractClassesFromJar(ProtocolClassLoader classLoader, File jarFile, Class<T> type) throws Exception {

        List<T> resultList = new ArrayList<>();

        type = (Class<T>) classLoader.loadClass(type.getName());

        // 加载 JAR 包
        try (JarFile jar = new JarFile(jarFile)) {
            for (JarEntry entry : jar.stream().collect(Collectors.toList())) {
                String entryName = entry.getName();
                // 过滤 .class 文件
                if (entryName.endsWith(".class")) {
                    String className = entryName
                            .replace("/", ".") // 转换路径分隔符
                            .replace(".class", ""); // 去掉后缀

                    try {
                        // 加载类
                        Class<?> clazz = classLoader.loadClass(className);

                        // 判断是否实现了目标接口
                        if (type.isAssignableFrom(clazz)
                                && !clazz.isInterface()
                                && !Modifier.isAbstract(clazz.getModifiers())) {
                            // 反射获取实例
                            @SuppressWarnings("unchecked")
                            T instance = (T) clazz.getDeclaredConstructor().newInstance();

                            // 放入 集合
                            resultList.add(instance);
                        }
                    } catch (Exception e) {
                        // 忽略非目标类的加载异常
                        System.err.println("Failed to load class: " + className + ", " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return resultList;
    }

}
