package com.zjh.zzone.iot.media.gb28181.session;

import com.ylg.iot.media.bo.CatalogData;
import com.ylg.iot.media.bo.SyncStatus;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CatalogDataManager implements CommandLineRunner {

    @Autowired
    private MediaDeviceChannelService deviceChannelService;

    private final Map<String, CatalogData> dataMap = new ConcurrentHashMap<>();

    private final String key = "vmp:sip:catalog:data:";

    public String buildMapKey(String gbId, int sn ) {
        return gbId + "_" + sn;
    }

    public void addReady(MediaDevice device, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(device.getGbId(), sn));
        if (catalogData != null) {
            Set<String> redisKeysForChannel = catalogData.getRedisKeysForChannel();
            if (redisKeysForChannel != null && !redisKeysForChannel.isEmpty()) {
                for (String deleteKey : redisKeysForChannel) {
                    RedisUtils.deleteObject(key + deleteKey);
                }
            }
            dataMap.remove(buildMapKey(device.getGbId(),sn));
        }
        catalogData = new CatalogData();
        catalogData.setDevice(device);
        catalogData.setSn(sn);
        catalogData.setStatus(CatalogData.CatalogDataStatus.ready);
        catalogData.setTime(Instant.now());
        dataMap.put(buildMapKey(device.getGbId(),sn), catalogData);
    }

    public void put(String deviceId, int sn, int total, MediaDevice device, List<MediaDeviceChannel> deviceChannelList) {
        CatalogData catalogData = dataMap.get(buildMapKey(device.getGbId(),sn));
        if (catalogData == null ) {
            log.warn("[缓存-Catalog] 未找到缓存对象，可能已经结束");
            return;
        }
        catalogData.setStatus(CatalogData.CatalogDataStatus.running);
        catalogData.setTotal(total);
        catalogData.setTime(Instant.now());

        if (deviceChannelList != null && !deviceChannelList.isEmpty()) {
            for (MediaDeviceChannel deviceChannel : deviceChannelList) {
                String keyForChannel = "CHANNEL:" + deviceId + ":" + deviceChannel.getGbId() + ":" + sn;
                RedisUtils.setObject(key + keyForChannel, deviceChannel);
                catalogData.getRedisKeysForChannel().add(keyForChannel);
            }
        }
    }

    public List<MediaDeviceChannel> getDeviceChannelList(String deviceId, int sn) {
        List<MediaDeviceChannel> result = new ArrayList<>();
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null ) {
            log.warn("[Redis-Catalog] 未找到缓存对象，可能已经结束");
            return result;
        }
        for (String objectKey : catalogData.getRedisKeysForChannel()) {
            MediaDeviceChannel deviceChannel = RedisUtils.getObject(key + objectKey, MediaDeviceChannel.class);
            if (deviceChannel != null) {
                result.add(deviceChannel);
            }
        }
        return result;
    }

    public SyncStatus getSyncStatus(String gbId) {
        if (dataMap.isEmpty()) {
            return null;
        }
        Set<String> keySet = dataMap.keySet();
        for (String key : keySet) {
            CatalogData catalogData = dataMap.get(key);
            if (catalogData != null && gbId.equals(catalogData.getDevice().getGbId())) {

                SyncStatus syncStatus = new SyncStatus(catalogData.getTotal(), catalogData.getRedisKeysForChannel().size(),
                        catalogData.getErrorMsg(), null, catalogData.getTime());

                syncStatus.setSyncIng(!catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready) && !catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end));
                if (catalogData.getErrorMsg() != null) {
                    // 失败的同步信息,返回一次后直接移除
                    dataMap.remove(key);
                }
                return syncStatus;
            }
        }
        return null;
    }

    public boolean isSyncRunning(String gbId) {
        if (dataMap.isEmpty()) {
            return false;
        }
        Set<String> keySet = dataMap.keySet();
        for (String key : keySet) {
            CatalogData catalogData = dataMap.get(key);
            if (catalogData != null && gbId.equals(catalogData.getDevice().getGbId())) {
                return !catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end);
            }
        }
        return false;
    }

    @Override
    public void run(String... args) throws Exception {
        // 启动时清理旧的数据
        RedisUtils.batchDelete(key);
    }

    @Scheduled(fixedDelay = 50 * 1000)   //每5秒执行一次, 发现数据5秒未更新则移除数据并认为数据接收超时
    private void timerTask(){
        log.debug("Scheduled: CatalogDataManager...");
        if (dataMap.isEmpty()) {
            return;
        }
        Set<String> keys = dataMap.keySet();

        Instant instantBefore5S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(5));
        Instant instantBefore30S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(30));
        for (String dataKey : keys) {
            CatalogData catalogData = dataMap.get(dataKey);
            if ( catalogData.getTime().isBefore(instantBefore5S)) {
                if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.running)) {
                    String deviceId = catalogData.getDevice().getGbId();
                    int sn = catalogData.getSn();
                    List<MediaDeviceChannel> deviceChannelList = getDeviceChannelList(deviceId, sn);
                    if (catalogData.getTotal() == deviceChannelList.size()) {
                        deviceChannelService.resetChannels(catalogData.getDevice().getId(), deviceChannelList);
                    } else {
                        deviceChannelService.updateChannels(catalogData.getDevice(), deviceChannelList);
                    }
                    String errorMsg = "更新成功，共" + catalogData.getTotal() + "条，已更新" + deviceChannelList.size() + "条";
                    catalogData.setErrorMsg(errorMsg);
                } else if (catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready)) {
                    String errorMsg = "同步失败，等待回复超时";
                    catalogData.setErrorMsg(errorMsg);
                }
            }
            if ((catalogData.getStatus().equals(CatalogData.CatalogDataStatus.end) || catalogData.getStatus().equals(CatalogData.CatalogDataStatus.ready))
                    && catalogData.getTime().isBefore(instantBefore30S)) { // 超过三十秒，如果标记为end则删除
                dataMap.remove(dataKey);
                Set<String> redisKeysForChannel = catalogData.getRedisKeysForChannel();
                if (redisKeysForChannel != null && !redisKeysForChannel.isEmpty()) {
                    for (String deleteKey : redisKeysForChannel) {
                        RedisUtils.deleteObject(key + deleteKey);
                    }
                }
            }
        }
    }

    public void setChannelSyncEnd(String deviceId, int sn, String errorMsg) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return;
        }
        catalogData.setStatus(CatalogData.CatalogDataStatus.end);
        catalogData.setErrorMsg(errorMsg);
        catalogData.setTime(Instant.now());
    }

    public int size(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getRedisKeysForChannel().size() + catalogData.getErrorChannel().size();
    }

    public int sumNum(String deviceId, int sn) {
        CatalogData catalogData = dataMap.get(buildMapKey(deviceId,sn));
        if (catalogData == null) {
            return 0;
        }
        return catalogData.getTotal();
    }
}
