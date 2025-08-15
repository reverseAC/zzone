package com.zjh.zzone.iot.media.controller;

import com.ylg.core.domain.R;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.config.MediaConfig;
import com.ylg.iot.media.service.MediaServerService;
import com.ylg.iot.vo.MediaInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * 流媒体服务 控制器
 *
 * @author zjh
 * @since 2025-08-01 09:06
 */
@Slf4j
@RestController
@RequestMapping("/server")
public class MediaServerController {

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private MediaConfig mediaConfig;

    /**
     * 获取流信息
     * @param app 应用名
     * @param stream 流标识
     * @return 流信息
     */
    @GetMapping(value = "/media_info")
    @ResponseBody
    public R<MediaInfo> getMediaInfo(String app, String stream) {
        MediaServer mediaServer = mediaServerService.getServerByServerId(mediaConfig.getId());
        Assert.notNull(mediaServer, "流媒体不存在");
        return R.ok(mediaServerService.getMediaInfo(mediaServer, app, stream));
    }

}
