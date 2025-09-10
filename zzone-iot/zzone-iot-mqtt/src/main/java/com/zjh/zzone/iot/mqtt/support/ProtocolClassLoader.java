package com.zjh.zzone.iot.mqtt.support;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 自定义协议类加载器
 *
 * @author zjh
 * @date 2025-01-03 9:17
 */
public class ProtocolClassLoader extends URLClassLoader {
    private final URL[] urls;

    public ProtocolClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.urls = urls;
    }

    /**
     * 关闭类加载器
     * @throws IOException 异常
     */
    @Override
    public void close() throws IOException {
        super.close();
    }

    /**
     * 加载指定名称的类
     * @param name 类名
     * @param resolve 是否解析类
     * @return 类
     * @throws ClassNotFoundException 异常
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            Class<?> clazz = this.loadSelfClass(name);
            if (null != clazz) {
                if (resolve) {
                    this.resolveClass(clazz);
                }

                return clazz;
            }
        } catch (Exception ignored) {
        }

        return super.loadClass(name, resolve);
    }

    public synchronized Class<?> loadSelfClass(String name) throws ClassNotFoundException {
        Class<?> clazz = super.findLoadedClass(name);
        if (clazz == null) {
            clazz = super.findClass(name);
            this.resolveClass(clazz);
        }

        return clazz;
    }

    public URL getResource(String name) {
        return !StringUtils.hasLength(name) ? this.urls[0] : super.findResource(name);
    }

    public URL[] getUrls() {
        return this.urls;
    }
}
