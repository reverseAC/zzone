package com.zjh.zzone.iot.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ylg.iot.vo.RecordInfo;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.enums.media.ChannelDataType;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.bo.InviteInfo;
import com.ylg.iot.media.event.media.RecordInfoEndEvent;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPCommander;
import com.ylg.iot.media.mapper.MediaDeviceChannelMapper;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.mybatis.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * 设备媒体通道信息 服务实现类
 *
 * @author zjh
 * @since 2025-03-31 10:17
 */
@Slf4j
@Service
public class MediaDeviceChannelServiceImpl extends BaseServiceImpl<MediaDeviceChannelMapper, MediaDeviceChannel> implements MediaDeviceChannelService {

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private InviteStreamService inviteStreamService;

    @Autowired
    private SIPCommander commander;

    @Autowired
    private UserSetting userSetting;

    // 记录录像查询的结果等待
    private final Map<String, SynchronousQueue<RecordInfo>> topicSubscribers = new ConcurrentHashMap<>();

    /**
     * 监听录像查询结束事件
     */
    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(RecordInfoEndEvent event) {
        SynchronousQueue<RecordInfo> queue = topicSubscribers.get("record" + event.getRecordInfo().getSn());
        if (queue != null) {
            queue.offer(event.getRecordInfo());
        }
    }

    @Override
    public MediaDeviceChannel getByGbId(String deviceGbId, String channelGbId) {
        return baseMapper.selectOne(new QueryWrapper<MediaDeviceChannel>().eq("parent_gb_id", deviceGbId).eq("gb_id", channelGbId));
    }

    @Override
    public MediaDeviceChannel getByGbId(Long parentId, String channelGbId) {
        return baseMapper.selectOne(new QueryWrapper<MediaDeviceChannel>().eq("parent_id", parentId).eq("gb_id", channelGbId));
    }

    @Override
    public void updateChannels(MediaDevice device, List<MediaDeviceChannel> channels) {
        List<MediaDeviceChannel> addChannels = new ArrayList<>();
        List<MediaDeviceChannel> updateChannels = new ArrayList<>();
        HashMap<String, MediaDeviceChannel> channelsInStore = new HashMap<>();
        int result = 0;
        if (channels != null && !channels.isEmpty()) {
            List<MediaDeviceChannel> channelList = getDeviceChannels(device.getGbId());
            if (channelList.isEmpty()) {
                for (MediaDeviceChannel channel : channels) {
                    channel.setParentId(device.getId());
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                    if (inviteInfo != null && inviteInfo.getStreamDetail() != null) {
                        channel.setStreamId(inviteInfo.getStreamDetail().getStream());
                    }
                    addChannels.add(channel);
                }
            } else {
                for (MediaDeviceChannel deviceChannel : channelList) {
                    channelsInStore.put(deviceChannel.getParentId() + deviceChannel.getGbId(), deviceChannel);
                }
                for (MediaDeviceChannel channel : channels) {
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
                    if (inviteInfo != null && inviteInfo.getStreamDetail() != null) {
                        channel.setStreamId(inviteInfo.getStreamDetail().getStream());
                    }

                    MediaDeviceChannel deviceChannelInDb = channelsInStore.get(channel.getParentId() + channel.getGbId());
                    if ( deviceChannelInDb != null) {
                        channel.setId(deviceChannelInDb.getId());
                        updateChannels.add(channel);
                    } else {
                        addChannels.add(channel);
                    }
                }
            }
            Set<String> channelSet = new HashSet<>();
            // 滤重
            List<MediaDeviceChannel> addChannelList = new ArrayList<>();
            List<MediaDeviceChannel> updateChannelList = new ArrayList<>();
            addChannels.forEach(channel -> {
                if (channelSet.add(channel.getGbId())) {
                    addChannelList.add(channel);
                }
            });
            channelSet.clear();
            updateChannels.forEach(channel -> {
                if (channelSet.add(channel.getGbId())) {
                    updateChannelList.add(channel);
                }
            });

            int limitCount = 500;
            if (!addChannelList.isEmpty()) {
                if (addChannelList.size() > limitCount) {
                    for (int i = 0; i < addChannelList.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > addChannelList.size()) {
                            toIndex = addChannelList.size();
                        }
                        List<MediaDeviceChannel> mediaDeviceChannels = addChannelList.subList(i, toIndex);
                        saveBatch(mediaDeviceChannels);
                        result += mediaDeviceChannels.size();
                    }
                } else {
                    saveBatch(addChannelList);
                    result += addChannelList.size();
                }
            }
            if (!updateChannelList.isEmpty()) {
                if (updateChannelList.size() > limitCount) {
                    for (int i = 0; i < updateChannelList.size(); i += limitCount) {
                        int toIndex = i + limitCount;
                        if (i + limitCount > updateChannelList.size()) {
                            toIndex = updateChannelList.size();
                        }
                        List<MediaDeviceChannel> mediaDeviceChannels = updateChannelList.subList(i, toIndex);
                        updateBatchById(mediaDeviceChannels);
                        result += mediaDeviceChannels.size();
                    }
                } else {
                    updateBatchById(updateChannelList);
                    result += updateChannelList.size();
                }
            }
        }
        log.info("设备通道信息更新完成，更新通道数量：{}", result);
    }

    @Override
    public void stopPlay(Long channelId) {
        update().set("stream_id", null).eq("id", channelId).update();
    }

    @Override
    @Transactional
    public boolean resetChannels(Long deviceDbId, List<MediaDeviceChannel> deviceChannelList) {
        if (CollectionUtils.isEmpty(deviceChannelList)) {
            return false;
        }
        List<MediaDeviceChannel> allChannels = getDeviceChannels(deviceDbId);
        Map<String, MediaDeviceChannel> allChannelMap = new HashMap<>();
        if (!allChannels.isEmpty()) {
            for (MediaDeviceChannel deviceChannel : allChannels) {
                allChannelMap.put(deviceChannel.getParentId() + deviceChannel.getGbId(), deviceChannel);
            }
        }
        // 数据去重
        List<MediaDeviceChannel> channels = new ArrayList<>();

        List<MediaDeviceChannel> updateChannels = new ArrayList<>();
        List<MediaDeviceChannel> addChannels = new ArrayList<>();
        List<MediaDeviceChannel> deleteChannels = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Integer> subContMap = new HashMap<>();

        for (MediaDeviceChannel deviceChannel : deviceChannelList) {
            MediaDeviceChannel channelInDb = allChannelMap.get(deviceChannel.getParentId() + deviceChannel.getGbId());
            if (channelInDb != null) {
                deviceChannel.setStreamId(channelInDb.getStreamId());
                deviceChannel.setHasAudio(channelInDb.isHasAudio());
                deviceChannel.setId(channelInDb.getId());
                updateChannels.add(deviceChannel);
            } else {
                addChannels.add(deviceChannel);
            }
            allChannelMap.remove(deviceChannel.getParentId() + deviceChannel.getGbId());
            channels.add(deviceChannel);
            if (!ObjectUtils.isEmpty(deviceChannel.getParentId())) {
                if (subContMap.get(deviceChannel.getParentGbId()) == null) {
                    subContMap.put(deviceChannel.getParentGbId(), 1);
                } else {
                    Integer count = subContMap.get(deviceChannel.getParentGbId());
                    subContMap.put(deviceChannel.getParentGbId(), count++);
                }
            }
        }
        deleteChannels.addAll(allChannelMap.values());

        if (stringBuilder.length() > 0) {
            log.info("[目录查询]收到的数据存在重复： {}" , stringBuilder);
        }
        if(CollectionUtils.isEmpty(channels)){
            log.info("通道重设，数据为空={}" , deviceChannelList);
            return false;
        }
        int limitCount = 500;
        if (!addChannels.isEmpty()) {
            if (addChannels.size() > limitCount) {
                for (int i = 0; i < addChannels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > addChannels.size()) {
                        toIndex = addChannels.size();
                    }
                    saveBatch(addChannels.subList(i, toIndex));
                }
            } else {
                saveBatch(addChannels);
            }
        }
        if (!updateChannels.isEmpty()) {
            if (updateChannels.size() > limitCount) {
                for (int i = 0; i < updateChannels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > updateChannels.size()) {
                        toIndex = updateChannels.size();
                    }
                    updateBatchById(updateChannels.subList(i, toIndex));
                }
            } else {
                updateBatchById(updateChannels);
            }
            // 不对收到的通道做比较，已确定是否真的发生变化，所以不发送更新通知

        }
        if (!deleteChannels.isEmpty()) {
            try {
                // 这些通道可能关联了，上级平台需要删除同时发送消息
                List<Long> ids = new ArrayList<>();
                deleteChannels.stream().forEach(deviceChannel -> {
                    ids.add(deviceChannel.getId());
                });
            } catch (Exception e) {
                log.error("[移除通道国标级联共享失败]", e);
            }
            if (deleteChannels.size() > limitCount) {
                for (int i = 0; i < deleteChannels.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > deleteChannels.size()) {
                        toIndex = deleteChannels.size();
                    }
                    removeBatchByIds(deleteChannels.subList(i, toIndex));
                }
            } else {
                removeBatchByIds(deleteChannels);
            }
        }
        return true;

    }

    @Override
    public void updateChannelStreamIdentification(MediaDeviceChannel channel) {
        Assert.hasLength(channel.getStreamIdentification(), "码流标识必须存在");
            log.info("[更新通道码流类型] 设备: {}, 通道：{}， 码流： {}", channel.getParentGbId(), channel.getGbId(), channel.getStreamIdentification());

        UpdateWrapper<MediaDeviceChannel> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", channel.getId()).set("stream_identification", channel.getStreamIdentification());
        baseMapper.update(updateWrapper);
    }

    @Override
    public void startPlay(Long channelId, String stream) {
        UpdateWrapper<MediaDeviceChannel> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", channelId).set("stream_id", stream);
        baseMapper.update(updateWrapper);
    }

    @Override
    public void changeAudio(Long channelId, Boolean audio) {
        UpdateWrapper<MediaDeviceChannel> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", channelId).set("has_audio", audio);
        baseMapper.update(updateWrapper);
    }

    @Override
    public void updateChannelStatus(MediaDeviceChannel channel) {
        UpdateWrapper<MediaDeviceChannel> wrapper = new UpdateWrapper<>();
        wrapper.eq("parent_id", channel.getParentId());
        wrapper.eq("gb_id", channel.getGbId());

        MediaDeviceChannel updateUser = new MediaDeviceChannel();
        updateUser.setOnline(channel.getOnline());

        update(updateUser, wrapper);
    }

    @Override
    public void addChannel(MediaDeviceChannel channel) {
        channel.setDataType(ChannelDataType.GB28181.value);
        save(channel);
    }

    @Override
    public void updateChannelForNotify(MediaDeviceChannel channel) {
        updateById(channel);
    }

    @Override
    public List<MediaDeviceChannel> getDeviceChannels(String deviceGbId) {
        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "未找到设备：" + deviceGbId);

        return list(new QueryWrapper<MediaDeviceChannel>().eq("parent_id", device.getId()).orderByAsc("name"));
    }

    @Override
    public List<MediaDeviceChannel> getDeviceChannels(Long deviceId) {
        MediaDevice device = deviceService.getById(deviceId);
        Assert.notNull(device, "未找到设备：" + deviceId);

        return list(new QueryWrapper<MediaDeviceChannel>().eq("parent_id", device.getId()));
    }

    @Override
    public void getRecordStatus(String deviceGbId, String channelGbId, ErrorCallback<String> rollback) {
        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在");

        MediaDeviceChannel channel = this.getChannelByGbId(device.getId(), channelGbId);
        Assert.notNull(channel, "设备通道不存在");

        try {
            commander.deviceStatusQuery(device, channel, (code, msg, data) -> {
                rollback.run(code, msg, data.getRecord());
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 获取设备状态: {}", e.getMessage());
            throw new RuntimeException("命令发送失败: " + e.getMessage());
        }
    }

    @Override
    public MediaDeviceChannel getBroadcastChannel(Long deviceDbId) {
        List<MediaDeviceChannel> channels = getDeviceChannels(deviceDbId);
        if (channels.size() == 1) {
            return channels.get(0);
        }
        for (MediaDeviceChannel channel : channels) {
            // 获取137类型的
            if (SipUtils.isFrontEnd(channel.getGbId())) {
                return channel;
            }
        }
        return null;
    }

    @Override
    public MediaDeviceChannel getChannelByGbId(Long parentId, String gbId) {
        return list(new QueryWrapper<MediaDeviceChannel>().eq("parent_id", parentId).eq("gb_id", gbId)).get(0);
    }

    @Override
    public void queryRecordInfo(MediaDevice device, MediaDeviceChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback) {
        try {
            int sipSn  =  (int)((Math.random() * 9 + 1) * 100000);
            commander.recordInfoQuery(device, channel.getGbId(), startTime, endTime, sipSn, null, null, eventResult -> {
                try {
                    // 消息发送成功, 监听等待数据到来
                    SynchronousQueue<RecordInfo> queue = new SynchronousQueue<>();
                    topicSubscribers.put("record" + sipSn, queue);
                    RecordInfo recordInfo = queue.poll(userSetting.getRecordInfoTimeout(), TimeUnit.MILLISECONDS);
                    if (recordInfo != null) {
                        callback.run(200, "成功", recordInfo);
                    } else {
                        callback.run(500, "失败", null);
                    }
                } catch (InterruptedException e) {
                    callback.run(500, e.getMessage(), null);
                } finally {
                    this.topicSubscribers.remove("record" + sipSn);
                }

            }, (eventResult -> {
                callback.run(500, "查询录像失败, status: " +  eventResult.statusCode + ", message: " + eventResult.msg, null);
            }));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 查询录像: {}", e.getMessage());
            throw new RuntimeException("命令发送失败: " +  e.getMessage());
        }
    }

    @Override
    public void deleteDeviceChannels(Long deviceId) {
        QueryWrapper<MediaDeviceChannel> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", deviceId);
        baseMapper.delete(wrapper);
    }
}
