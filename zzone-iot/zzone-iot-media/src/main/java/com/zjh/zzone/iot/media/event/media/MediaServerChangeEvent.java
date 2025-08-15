package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.entity.MediaServer;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 媒体服务器变更事件
 *
 * @author zjh
 * @date 2025-06-25 10:22
 */
@Getter
public class MediaServerChangeEvent extends ApplicationEvent {

    private List<MediaServer> mediaServerItemList;

    public MediaServerChangeEvent(Object source) {
        super(source);
    }

    public void setMediaServerItemList(List<MediaServer> mediaServerItemList) {
        this.mediaServerItemList = mediaServerItemList;
    }

    public void setMediaServerItemList(MediaServer... mediaServerItemArray) {
        this.mediaServerItemList = new ArrayList<>();
        this.mediaServerItemList.addAll(Arrays.asList(mediaServerItemArray));
    }
}
