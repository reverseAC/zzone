package com.zjh.zzone.iot.mqtt.support;

import com.ylg.core.exception.CheckedException;
import com.ylg.core.utils.file.FileUtils;
import com.ylg.iot.message.ProtocolSupportDefinition;
import jakarta.annotation.PreDestroy;
import lombok.Generated;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * 下载网络Jar包到本地
 * <pre>
 *     1. 下载的协议包放在./data/protocols目录下，可通过启动参数-Dprotocol.temp.path进行配置
 *     2. 文件名规则: 协议ID+"_"+md5(文件地址)
 * </pre>
 *
 * @author zjh
 * @since 2025-01-02 19:52
 */
public class AutoDownloadJarProtocolSupportLoader extends JarProtocolSupportLoader {

    /**
     * 存放本地协议的文件夹
     */
    final File tempPath;

    public AutoDownloadJarProtocolSupportLoader() {
        tempPath = new File(System.getProperty("protocol.temp.path", "./data/protocols"));
        tempPath.mkdirs();
    }

    @Override
    @PreDestroy
    @Generated
    protected void closeAll() {
        super.closeAll();
    }

    @Override
    public void closeLoader(Long loader) {
        super.closeLoader(loader);
    }

    // 加载jar包
    @Override
    public ProtocolSupport load(ProtocolSupportDefinition definition) {

        // jar包地址
        String location = definition.getLocation();

        if (StringUtils.hasText(location)){
            String urlMd5 = DigestUtils.md5Hex(location);
            // 地址没变则直接加载本地文件
            File file = new File(tempPath, (definition.getId() + "_" + urlMd5) + ".jar");
            if (file.exists()) {
                // 设置文件地址文本地文件
                definition.setLocalLocation(file.getAbsolutePath());
                // 如果已经存在则直接加载
                try {
                    return super.load(definition);
                } catch (Exception e) {
                    // 加载失败则删除文件,避免影响重试逻辑
                    if (file.exists()) {
                        file.delete();
                    }
                    throw e;
                }
            }

            // 不存在则下载后再进行加载
            if (location.startsWith("http")) {
                try {
                    String localLocation = FileUtils.downLoadFromUrl(location, file.getAbsolutePath());
                    definition.setLocalLocation(localLocation);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                FileUtils.downLoadFile(location, file.getAbsolutePath());
            }
            return super.load(definition);
        } else {
            throw new CheckedException("协议地址不能为空");
        }
    }
}
