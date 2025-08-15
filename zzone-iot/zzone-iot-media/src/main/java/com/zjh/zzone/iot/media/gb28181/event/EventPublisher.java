package com.zjh.zzone.iot.media.gb28181.event;

import com.ylg.iot.media.event.MediaServerOfflineEvent;
import com.ylg.iot.media.event.MediaServerOnlineEvent;
import com.ylg.iot.media.event.RecordEndEvent;
import com.ylg.iot.vo.RecordInfo;
import com.ylg.iot.entity.MediaServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**    
 * @description:Event事件通知推送器，支持推送在线事件、离线事件
 * @author: swwheihei
 * @date:   2020年5月6日 上午11:30:50     
 */
@Component
public class EventPublisher {

	@Autowired
    private ApplicationEventPublisher applicationEventPublisher;

	public void mediaServerOfflineEventPublish(MediaServer mediaServer){
		MediaServerOfflineEvent outEvent = new MediaServerOfflineEvent(this);
		outEvent.setMediaServer(mediaServer);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void mediaServerOnlineEventPublish(MediaServer mediaServer) {
		MediaServerOnlineEvent outEvent = new MediaServerOnlineEvent(this);
		outEvent.setMediaServer(mediaServer);
		applicationEventPublisher.publishEvent(outEvent);
	}

	public void recordEndEventPush(RecordInfo recordInfo) {
		RecordEndEvent outEvent = new RecordEndEvent(this);
		outEvent.setRecordInfo(recordInfo);
		applicationEventPublisher.publishEvent(outEvent);
	}
}
