package com.zjh.zzone.iot.media.callback;

import com.ylg.iot.media.gb28181.event.sip.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

/**
 * 管理等待设备回复的事件
 *
 * @author zjh
 * @date 2025-07-11 10:47
 */
@Slf4j
@Component
public class MessageSubscribe {

    private final Map<String, MessageEvent<?>> subscribes = new ConcurrentHashMap<>();

    private final DelayQueue<MessageEvent<?>> delayQueue = new DelayQueue<>();

    @Scheduled(fixedDelay = 200)   //每200毫秒执行
    public void execute(){
        log.debug("Scheduled: MessageSubscribe...");
        if (delayQueue.isEmpty()) {
            return;
        }
        try {
            MessageEvent<?> take = delayQueue.poll();
            if (take == null) {
                return;
            }
            // 出现超时异常
            if(take.getCallback() != null) {
                take.getCallback().run(200, "消息超时未回复", null);
            }
            subscribes.remove(take.getKey());
        } catch (Exception e) {
            log.error("[设备状态任务] ", e);
        }
    }

    public void addSubscribe(MessageEvent<?> event) {
        MessageEvent<?> messageEvent = subscribes.get(event.getKey());
        if (messageEvent != null) {
            subscribes.remove(event.getKey());
            delayQueue.remove(messageEvent);
        }
        subscribes.put(event.getKey(), event);
        delayQueue.offer(event);
    }

    public MessageEvent<?> getSubscribe(String key) {
        return subscribes.get(key);
    }

    public void removeSubscribe(String key) {
        if(key == null){
            return;
        }
        MessageEvent<?> messageEvent = subscribes.get(key);
        if (messageEvent != null) {
            subscribes.remove(key);
            delayQueue.remove(messageEvent);
        }
    }

    public boolean isEmpty(){
        return subscribes.isEmpty();
    }

    public Integer size() {
        return subscribes.size();
    }
}
