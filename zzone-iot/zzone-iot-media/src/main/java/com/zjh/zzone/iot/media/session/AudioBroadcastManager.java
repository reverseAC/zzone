package com.zjh.zzone.iot.media.session;

import com.ylg.iot.media.bo.AudioBroadcastCache;
import com.ylg.iot.media.config.SipConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音广播消息管理类
 * @author lin
 */
@Slf4j
@Component
public class AudioBroadcastManager {

    @Autowired
    private SipConfig config;

    public static Map<Long, AudioBroadcastCache> data = new ConcurrentHashMap<>();


    public void update(AudioBroadcastCache audioBroadcastCatch) {
        data.put(audioBroadcastCatch.getChannelId(), audioBroadcastCatch);
    }

    public void del(Long channelId) {
        data.remove(channelId);

    }

    public List<AudioBroadcastCache> getAll(){
        Collection<AudioBroadcastCache> values = data.values();
        return new ArrayList<>(values);
    }


    public boolean exit(Long channelId) {
        return data.containsKey(channelId);
    }

    public AudioBroadcastCache get(Long channelId) {
        return data.get(channelId);
    }

    public List<AudioBroadcastCache> getByDeviceId(String deviceGbId) {
        List<AudioBroadcastCache> audioBroadcastCatchList= new ArrayList<>();
        for (AudioBroadcastCache broadcastCatch : data.values()) {
            if (broadcastCatch.getDeviceId().equals(deviceGbId)) {
                audioBroadcastCatchList.add(broadcastCatch);
            }
        }

        return audioBroadcastCatchList;
    }
}
