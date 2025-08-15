package com.zjh.zzone.iot.media.init;

import com.ylg.iot.media.config.MediaConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.event.media.MediaServerChangeEvent;
import com.ylg.iot.media.service.MediaServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时从配置文件加载节点信息，以及发送个节点状态管理去控制节点状态
 */
@Slf4j
@Component
@Order(value=12)
public class MediaServerRunner implements CommandLineRunner {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private UserSetting userSetting;


    @Override
    public void run(String... strings) throws Exception {
        // 清理所有在线节点的缓存信息
        mediaServerService.clearMediaServerForOnline();

        // 获取配置的流媒体服务信息
        MediaServer defaultMediaServer = mediaConfig.getMediaSerItem();
        // 判断默认流媒体服务是否存在，且是否变更
        MediaServer mediaServer = mediaServerService.getServerByServerId(defaultMediaServer.getServerId());
        if (mediaServer != null) {
            defaultMediaServer.setId(mediaServer.getId());
            mediaServerService.updateServer(defaultMediaServer);
        } else {
            mediaServerService.addServer(defaultMediaServer);
        }

        // 获取所有的zlm， 并开启主动连接
        List<MediaServer> all = mediaServerService.list();
        log.info("[媒体节点] 加载节点列表， 共{}个节点", all.size());
        MediaServerChangeEvent event = new MediaServerChangeEvent(this);
        event.setMediaServerItemList(all);
        applicationEventPublisher.publishEvent(event);
    }
}
