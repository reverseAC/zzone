package com.zjh.zzone.iot.media.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ylg.core.domain.R;
import com.ylg.core.enums.StatusEnum;
import com.ylg.core.exception.CheckedException;
import com.ylg.core.utils.bean.BeanUtils;
import com.ylg.iot.RemoteIotService;
import com.ylg.iot.constant.DeviceStatusEnum;
import com.ylg.iot.constant.IotCacheConstants;
import com.ylg.iot.dto.DeviceQueryDTO;
import com.ylg.iot.media.bo.DeviceStatus;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.gb28181.task.deviceStatus.DeviceStatusTask;
import com.ylg.iot.media.scheduling.DynamicTask;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.media.bo.SyncStatus;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.gb28181.task.deviceStatus.DeviceStatusTaskRunner;
import com.ylg.iot.media.mapper.MediaDeviceMapper;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPCommander;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.cmd.CatalogResponseMessageHandler;
import com.ylg.iot.media.vo.MediaDeviceVO;
import com.ylg.iot.redis.DeviceProductCache;
import com.ylg.iot.vo.DeviceInternalVO;
import com.ylg.mybatis.base.BaseServiceImpl;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static com.ylg.iot.constant.MediaCacheConstants.DEVICE_PREFIX;

/**
 * 设备媒体信息 服务实现类
 *
 * @author zjh
 * @since 2025-03-31 10:17
 */
@Slf4j
@Service
public class MediaDeviceServiceImpl extends BaseServiceImpl<MediaDeviceMapper, MediaDevice> implements MediaDeviceService {

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private SIPCommander commander;

    @Autowired
    private CatalogResponseMessageHandler catalogResponseMessageHandler;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private MediaDeviceChannelService channelService;

    @Autowired
    private InviteStreamService inviteStreamService;

    @Autowired
    private RemoteIotService remoteIotService;

    @Autowired
    private DeviceStatusTaskRunner deviceStatusTaskRunner;


    @Override
    public List<MediaDeviceVO> getAllOnlineDevice() {
        return baseMapper.getByOnline(DeviceStatusEnum.ONLINE.getCode());
    }

    @Override
    public void updateDevice(MediaDevice device) {
        device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
        this.updateById(device);

        // 更新缓存
        MediaDeviceVO deviceCache = RedisUtils.getObject(DEVICE_PREFIX + device.getGbId(), MediaDeviceVO.class);
        BeanUtils.copyProperties(device, deviceCache);
        RedisUtils.setObject(DEVICE_PREFIX + device.getGbId(), deviceCache);
    }


    @Transactional
    @Override
    public void updateDeviceList(List<MediaDevice> deviceList) {
        if (deviceList.isEmpty()){
            log.info("[批量更新设备] 列表为空，更细失败");
            return;
        }
        if (deviceList.size() == 1) {
            updateDevice(deviceList.get(0));
        } else {
            for (MediaDevice device : deviceList) {
                device.setCharset(device.getCharset() == null ? "" : device.getCharset().toUpperCase());
            }
            int limitCount = 300;
            if (!deviceList.isEmpty()) {
                if (deviceList.size() > limitCount) {
                    for (int i = 0; i < deviceList.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > deviceList.size()) {
                            toIndex = deviceList.size();
                        }
                        this.updateBatchById(deviceList.subList(i, toIndex));
                    }
                } else {
                    this.updateBatchById(deviceList);
                }
                for (MediaDevice device : deviceList) {
                    RedisUtils.setObject(DEVICE_PREFIX + device.getGbId(), device); //TODO 转为VO
                }
            }
        }
    }

    @Override
    @Transactional
    public void delete(String deviceGbId) {
        MediaDevice device = getByGbId(deviceGbId);
        Assert.notNull(device, "未找到设备");

        // 删除通道
        channelService.deleteDeviceChannels(device.getId());
        // 删除设备
        baseMapper.deleteById(device);
        // 删除缓存
        RedisUtils.deleteObject(DEVICE_PREFIX + device.getGbId());
        // 删除点播信息
        inviteStreamService.clearInviteInfo(deviceGbId);
    }

    /**
     * 根据设备国标id查询设备信息
     *
     * @param gbId 设备id
     * @return DeviceMediaInfo
     */
    @Override
    public MediaDeviceVO getByGbId(String gbId) {
        MediaDeviceVO device = RedisUtils.getObject(DEVICE_PREFIX + gbId, MediaDeviceVO.class);
        if (device == null) {
            device = baseMapper.selectByGbId(gbId);
            if (device != null) {
                RedisUtils.setObject(DEVICE_PREFIX + gbId, device);
            }
        }
        return device;
    }

    /**
     * 设备注册/设备上线
     *
     * @param device 设备信息
     * @param sipTransactionInfo sip事务信息
     */
    @Override
    public void online(MediaDeviceVO device, SipTransactionInfo sipTransactionInfo) {
        log.info("[设备上线] gbId：{}->{}:{}", device.getGbId(), device.getIp(), device.getPort());

        remoteIotService.changeDeviceStatusBySn(device.getGbId(), DeviceStatusEnum.ONLINE.getCode());

        MediaDeviceVO deviceInRedis = RedisUtils.getObject(DEVICE_PREFIX + device.getGbId(), MediaDeviceVO.class);
        MediaDeviceVO deviceInDb = getByGbId(device.getGbId());

        if (deviceInRedis != null && deviceInDb == null) {
            // redis 存在脏数据
            inviteStreamService.clearInviteInfo(device.getGbId());
        }

        device.setKeepAliveTime(LocalDateTime.now());
        if (device.getHeartBeatCount() == null) {
            // 读取设备配置， 获取心跳间隔和心跳超时次数， 在次之前暂时设置为默认值
            device.setHeartBeatCount(3);
            device.setHeartBeatInterval(60);
            device.setPositionCapability(0);

        }
        if (sipTransactionInfo != null) {
            device.setSipTransactionInfo(sipTransactionInfo);
        } else {
            if (deviceInRedis != null) {
                device.setSipTransactionInfo(deviceInRedis.getSipTransactionInfo());
            }
        }

        // 第一次上线 或者 设备之前是离线状态--进行通道同步和设备信息查询
        if (deviceInDb == null) {
            // 调用iot-service的上线接口，接口功能为： TODO
            //      如果存在，则设备上线
            //      如果不存在，则添加设备

            device.setOnline(DeviceStatusEnum.ONLINE.getCode());
            log.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", device.getGbId());
            baseMapper.insert(device);
            RedisUtils.setObject(DEVICE_PREFIX + device.getGbId(), device);
            try {
                commander.deviceInfoQuery(device);
                commander.deviceConfigQuery(device, null, "BasicParam", null);
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 查询设备信息和配置: {}", e.getMessage());
            }
            // 进行通道等信息的同步
            sync(device);

        } else {
            if(DeviceStatusEnum.OFFLINE.getCode().equals(device.getOnline())){
                device.setOnline(DeviceStatusEnum.ONLINE.getCode());
                baseMapper.updateById(device);
                RedisUtils.setObject(DEVICE_PREFIX + device.getGbId(), device);
                if (userSetting.getSyncChannelOnDeviceOnline()) {
                    log.info("[设备上线,离线状态下重新注册]: {}，查询设备信息以及通道信息", device.getGbId());
                    try {
                        commander.deviceInfoQuery(device);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
                    }
                    sync(device);     // TODO 如果设备下的通道级联到了其他平台，那么需要发送事件或者notify给上级平台
                }

            } else {
                baseMapper.updateById(device);
                RedisUtils.setObject(DEVICE_PREFIX + device.getGbId(), device);
            }
            if (channelService.getDeviceChannels(device.getGbId()).isEmpty()) {
                log.info("[设备上线]: {}，通道数为0,查询通道信息", device.getGbId());
                sync(device);
            }
        }
        long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
        if (deviceStatusTaskRunner.containsKey(device.getGbId())) {
            if (sipTransactionInfo == null) {
                deviceStatusTaskRunner.updateDelay(device.getGbId(), expiresTime + System.currentTimeMillis());
            } else {
                deviceStatusTaskRunner.removeTask(device.getGbId());
                DeviceStatusTask task = DeviceStatusTask.getInstance(device.getGbId(), sipTransactionInfo, expiresTime + System.currentTimeMillis(), this::deviceStatusExpire);
                deviceStatusTaskRunner.addTask(task);
            }
        } else {
            DeviceStatusTask task = DeviceStatusTask.getInstance(device.getGbId(), sipTransactionInfo, expiresTime + System.currentTimeMillis(), this::deviceStatusExpire);
            deviceStatusTaskRunner.addTask(task);
        }

    }

    private void deviceStatusExpire(String deviceId, SipTransactionInfo transactionInfo) {
        log.info("[设备状态] 到期， 编号： {}", deviceId);
        offline(deviceId, "保活到期");
    }

    @Override
    public MediaDevice getDeviceByChannelGbId(String channelGbId) {
        return baseMapper.selectByChannelGbId(channelGbId);
    }

    @Override
    public void offline(String gbId, String reason) {
        MediaDevice device = getByGbId(gbId);
        if (device == null) {
            log.warn("[设备不存在] device：{}", gbId);
            return;
        }

        remoteIotService.changeDeviceStatusBySn(device.getGbId(), DeviceStatusEnum.OFFLINE.getCode());

        // 主动查询设备状态, 没有HostAddress无法发送请求，可能是手动添加的设备
        if (device.getHostAddress() != null) {
            Boolean deviceStatus = getDeviceStatus(device);
            if (deviceStatus != null && deviceStatus) {
                log.info("[设备离线] 主动探测发现设备在线，暂不处理  device：{}", gbId);
                MediaDeviceVO mediaDeviceVO = BeanUtils.copyProperties(device, MediaDeviceVO::new);
                online(mediaDeviceVO, null);
                return;
            }
        }
        log.info("[设备离线] {}, device：{}， 心跳间隔： {}，心跳超时次数： {}， 上次心跳时间：{}， 上次注册时间： {}", reason, gbId,
                device.getHeartBeatInterval(), device.getHeartBeatCount(), device.getKeepAliveTime(), device.getRegisterTime());
        device.setOnline(DeviceStatusEnum.OFFLINE.getCode());
        //
        cleanOfflineDevice(device);
        //
        RedisUtils.setObject(DEVICE_PREFIX + device.getGbId(), device);
        updateById(device);

        // 通道下线
        channelOfflineByDevice(device);
    }

    private void channelOfflineByDevice(MediaDevice device) {
        // 级联设备离线
    }

    private void cleanOfflineDevice(MediaDevice device) {

        // 离线释放所有ssrc

        // 移除订阅

    }

    @Override
    public Boolean getDeviceStatus(@NotNull MediaDevice device) {
        SynchronousQueue<DeviceStatus> queue = new SynchronousQueue<>();
        try {
            commander.deviceStatusQuery(device, null, ((code, msg, data) -> {
                queue.offer(data);
            }));
            DeviceStatus data = queue.poll(10, TimeUnit.SECONDS); // 最多等待10秒
            if (data != null && "ONLINE".equalsIgnoreCase(data.getOnline())) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }

        } catch (InvalidArgumentException | SipException | ParseException | InterruptedException e) {
            log.error("[命令发送失败] 设备状态查询: {}", e.getMessage());
        }
        return null;
    }


    @Override
    public void sync(MediaDevice device) {
        if (catalogResponseMessageHandler.isSyncRunning(device.getGbId())) {
            SyncStatus syncStatus = catalogResponseMessageHandler.getChannelSyncProgress(device.getGbId());
            log.info("[同步通道] 同步已存在, 设备: {}, 同步信息: {}", device.getGbId(), JSON.toJSON(syncStatus));
            return;
        }
        int seq = (int)((Math.random()*9+1)*100000);
        catalogResponseMessageHandler.setChannelSyncReady(device, seq);
        try {
            commander.catalogQuery(device, seq, event -> {
                String errorMsg = String.format("同步通道失败，错误码： %s, %s", event.statusCode, event.msg);
                log.info("[同步通道]失败,编号: {}, 错误码： {}, {}", device.getGbId(), event.statusCode, event.msg);
                catalogResponseMessageHandler.setChannelSyncEnd(device.getGbId(), seq, errorMsg);
            });
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[同步通道], 信令发送失败：{}", e.getMessage() );
            String errorMsg = String.format("同步通道失败，信令发送失败： %s", e.getMessage());
            catalogResponseMessageHandler.setChannelSyncEnd(device.getGbId(), seq, errorMsg);
        }
    }


    @Override
    public void record(MediaDevice device, String channelId, String recordCmdStr, ErrorCallback<String> callback) {
        try {
            commander.recordCmd(device, channelId, recordCmdStr, callback);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 开始/停止录像: {}", e.getMessage());
            callback.run(500, "命令发送: " + e.getMessage(), null);
            throw new RuntimeException("命令发送: " + e.getMessage());
        }
    }

    @Override
    public void updateDeviceHeartInfo(MediaDevice device) {
        MediaDevice deviceInDb = baseMapper.selectById(device.getId());
        Assert.notNull(deviceInDb, "设备不存在:" + device.getId());

        if (!Objects.equals(deviceInDb.getHeartBeatCount(), device.getHeartBeatCount())
                || !Objects.equals(deviceInDb.getHeartBeatInterval(), device.getHeartBeatInterval())) {

            deviceInDb.setHeartBeatCount(device.getHeartBeatCount());
            deviceInDb.setHeartBeatInterval(device.getHeartBeatInterval());
            deviceInDb.setPositionCapability(device.getPositionCapability());
            updateDevice(deviceInDb);

            long expiresTime = Math.min(device.getExpires(), device.getHeartBeatInterval() * device.getHeartBeatCount()) * 1000L;
            if (deviceStatusTaskRunner.containsKey(device.getGbId())) {
                deviceStatusTaskRunner.updateDelay(device.getGbId(), expiresTime + System.currentTimeMillis());
            }
        }
    }

    @Override
    public void checkDeviceEnable(String deviceGbId) {
        DeviceProductCache deviceCache = RedisUtils.getObject(IotCacheConstants.DEVICE_CACHE_SN + deviceGbId, DeviceProductCache.class);
        if (deviceCache == null) {
            DeviceQueryDTO deviceQueryDTO = new DeviceQueryDTO();
            deviceQueryDTO.setSn(deviceGbId);
            R<List<DeviceInternalVO>> iotDeviceR = remoteIotService.internalDeviceList(deviceQueryDTO);
            if (R.SUCCESS != iotDeviceR.getCode()) {
                throw new CheckedException(iotDeviceR.getMsg());
            }

            List<DeviceInternalVO> iotDeviceList = iotDeviceR.getData();
            if (CollectionUtils.isEmpty(iotDeviceList)) {
                throw new CheckedException("设备不存在:" + deviceGbId);
            }
            DeviceInternalVO deviceInternal = iotDeviceList.get(0);
            if (StatusEnum.DISABLE.getCode().equals(deviceInternal.getStatus())) {
                throw new CheckedException("设备已禁用,无法操作");
            }
        } else {
            if (StatusEnum.DISABLE.getCode().equals(deviceCache.getDeviceEnable())) {
                throw new CheckedException("设备已禁用,无法操作");
            }
        }
    }
}
