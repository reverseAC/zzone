package com.zjh.zzone.iot.media.gb28181.task.deviceStatus;

import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * 设备状态管理定时任务
 */
@Slf4j
@Component
public class DeviceStatusTaskRunner {

    private final Map<String, DeviceStatusTask> subscribes = new ConcurrentHashMap<>();

    private final DelayQueue<DeviceStatusTask> delayQueue = new DelayQueue<>();

    @Autowired
    private ServerInstanceConfig serverInstance;

    private final String prefix = "vmp:device:status:";

    // 状态过期检查
    @Scheduled(fixedDelay = 500)
    public void expirationCheck(){
        log.debug("Scheduled: DeviceStatusTaskRunner...");
        DeviceStatusTask take = null;
        try {
            take = delayQueue.poll();
            if (take == null) {
                return;
            }
            try {
                removeTask(take.getDeviceId());
                take.expired();
            } catch (Exception e) {
                log.error("[设备状态到期] 到期处理时出现异常， 设备编号: {} ", take.getDeviceId());
            }
        } catch (Exception e) {
            log.error("[设备状态任务] ", e);
        }
    }

    public void addTask(DeviceStatusTask task) {
        long duration = (task.getDelayTime() - System.currentTimeMillis())/1000;
        if (duration < 0) {
            return;
        }
        subscribes.put(task.getDeviceId(), task);
        String key = String.format("%s%s:%s", prefix, serverInstance.getInstanceId(), task.getDeviceId());
        RedisUtils.setObject(key, task.getInfo(), duration, TimeUnit.SECONDS);
        delayQueue.offer(task);
    }

    public boolean removeTask(String key) {
        DeviceStatusTask task = subscribes.get(key);
        if (task == null) {
            return false;
        }
        String redisKey = String.format("%s%s:%s", prefix, serverInstance.getInstanceId(), task.getDeviceId());
        RedisUtils.deleteObject(redisKey);
        subscribes.remove(key);
        if (delayQueue.contains(task)) {
            boolean remove = delayQueue.remove(task);
            if (!remove) {
                log.info("[移除状态任务] 从延时队列内移除失败： {}", key);
            }
        }
        return true;
    }

    public SipTransactionInfo getTransactionInfo(String key) {
        DeviceStatusTask task = subscribes.get(key);
        if (task == null) {
            return null;
        }
        return task.getTransactionInfo();
    }

    public boolean updateDelay(String key, long expirationTime) {
        DeviceStatusTask task = subscribes.get(key);
        if (task == null) {
            return false;
        }
        log.debug("[更新状态任务时间] 编号： {}", key);
        task.setDelayTime(expirationTime);
        String redisKey = String.format("%s%s:%s", prefix, serverInstance.getInstanceId(), task.getDeviceId());
        RedisUtils.setExpire(redisKey, (expirationTime - System.currentTimeMillis())/1000, TimeUnit.SECONDS);
        return true;
    }

    public boolean containsKey(String key) {
        return subscribes.containsKey(key);
    }
}
