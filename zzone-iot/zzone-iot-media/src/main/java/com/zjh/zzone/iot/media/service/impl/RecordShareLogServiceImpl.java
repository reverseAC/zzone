package com.zjh.zzone.iot.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ylg.core.exception.CheckedException;
import com.ylg.core.utils.StringUtils;
import com.ylg.core.utils.bean.BeanUtils;
import com.ylg.iot.entity.*;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.config.MediaConfig;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.contant.InviteResultCode;
import com.ylg.iot.media.contant.RecordShareDurationUnit;
import com.ylg.iot.media.dto.RecordShareLogDTO;
import com.ylg.iot.media.mapper.RecordShareLogMapper;
import com.ylg.iot.media.service.*;
import com.ylg.iot.media.utils.CloudRecordUtils;
import com.ylg.iot.media.vo.RecordShareLogVO;
import com.ylg.iot.vo.DownloadFileInfo;
import com.ylg.iot.vo.StreamContent;
import com.ylg.mybatis.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RecordShareLogServiceImpl extends BaseServiceImpl<RecordShareLogMapper, RecordShareLog> implements RecordShareLogService {

    @Autowired
    private PlayService playService;

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private MediaDeviceChannelService deviceChannelService;

    @Autowired
    private CloudRecordService cloudRecordService;

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private MediaConfig mediaConfig;


    @Override
    public IPage<RecordShareLogVO> shareLogPage(IPage<RecordShareLogVO> page, RecordShareLogDTO params) {
        List<RecordShareLogVO> shareLogVOS = baseMapper.selectShareLogPage(page, params);
        if(CollectionUtils.isEmpty(shareLogVOS)) {
            return page.setRecords(Collections.emptyList());
        }

        Map<String, RecordShareLogVO> shareLogVOMap = shareLogVOS.stream().filter(logItem -> !StringUtils.isEmpty(logItem.getStream())).collect(Collectors.toMap(RecordShareLogVO::getStream, Function.identity()));
        if (!shareLogVOMap.isEmpty()) {
            cloudRecordService.list(new QueryWrapper<CloudRecord>().in("stream", shareLogVOMap.keySet())).forEach(cloudRecord -> {
                MediaServer serverByServer = mediaServerService.getServerByServerId(cloudRecord.getMediaServerId());

                DownloadFileInfo downloadFilePath = CloudRecordUtils.getDownloadFilePath(serverByServer, cloudRecord.getFilePath());
                String path = downloadFilePath.getHttpPath();

                shareLogVOMap.get(cloudRecord.getStream()).setPath(path);
            });
        }

        return page.setRecords(shareLogVOS);
    }

    @Override
    public RecordShareLogVO getShareLogById(Long id) {
        RecordShareLog recordShareLog = baseMapper.selectById(id);
        if (recordShareLog == null) {
            throw new CheckedException("未找到分享记录 id: " + id);
        }
        RecordShareLogVO recordShareLogVO = BeanUtils.copyProperties(recordShareLog, RecordShareLogVO::new);
        if (StringUtils.isEmpty(recordShareLogVO.getStream())) {
            return recordShareLogVO;
        }

        List<CloudRecord> cloudRecords = cloudRecordService.list(new QueryWrapper<CloudRecord>().eq("stream", recordShareLog.getStream()).select("id", " app", "stream"));
        if (!CollectionUtils.isEmpty(cloudRecords)) {
            CloudRecord cloudRecord = cloudRecords.get(0);
            MediaServer serverByServer = mediaServerService.getServerByServerId(cloudRecord.getMediaServerId());

            DownloadFileInfo downloadFilePath = CloudRecordUtils.getDownloadFilePath(serverByServer, cloudRecord.getFilePath());
            String path = downloadFilePath.getHttpPath();
            recordShareLogVO.setPath(path);
        }

        return recordShareLogVO;
    }

    @Override
    public Long createShare(RecordShareLogDTO recordShareLog) {
        String deviceGbId = recordShareLog.getDeviceGbId();
        String channelGbId = recordShareLog.getChannelGbId();

        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "未找到设备 deviceGbId: " + deviceGbId + ",channelGbId:{}" + channelGbId);

        MediaDeviceChannel channel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
        Assert.notNull(channel, "未找到通道 deviceGbId: " + deviceGbId + ",channelGbId:{}" + channelGbId);

        // 1. 插入分享记录
        // 计算过期时间
        Long timeMills = RecordShareDurationUnit.getByCode(recordShareLog.getValidDurationUnit()).getTimeMills(recordShareLog.getValidDuration());
        Instant instant = Instant.ofEpochMilli(timeMills + System.currentTimeMillis());
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        recordShareLog.setExpireTime(localDateTime);
        // 插入数据
        this.baseMapper.insert(recordShareLog);
        Long logId = recordShareLog.getId();

        // 下载录像
        // 回调：将录像与分享记录绑定
        ErrorCallback<StreamDetail> callback  = (code, msg, streamInfo) -> {
            if (code == InviteResultCode.SUCCESS.getCode()) {
                if (streamInfo != null) {
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix()) && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    StreamContent streamContent = streamInfo.getStreamContent();
                    recordShareLog.setStream(streamContent.getStream());
                    this.baseMapper.updateById(recordShareLog);
                }
            } else {
                log.error("录像下载失败, code: {}, msg: {}, streamInfo: {}", code, msg, streamInfo);
            }
        };
        // 格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTime = recordShareLog.getStartTime().format(formatter);
        String endTime = recordShareLog.getEndTime().format(formatter);
        playService.download(device, channel, startTime, endTime, 4, callback);

        return logId;
    }

    @Override
    public void updateShare(RecordShareLogDTO params) {
        // 更新数据
        this.baseMapper.updateById(params);

        // 踢出超数用户
    }

    @Override
    public void expireShare(Long logId) {
        // 修改过期时间为现在
        RecordShareLog log = this.getById(logId);
        log.setExpireTime(LocalDateTime.now());
        this.updateById(log);

        // 删除录像记录
        List<CloudRecord> cloudRecords = this.cloudRecordService.list(new QueryWrapper<CloudRecord>().eq("stream", log.getStream()));
        if (CollectionUtils.isEmpty(cloudRecords)) {
            return;
        }
        Set<Long> recordIdList = cloudRecords.stream().map(CloudRecord::getId).collect(Collectors.toSet());

        // 删除录像文件
        this.cloudRecordService.deleteFileByIds(recordIdList);
    }

    @Scheduled(fixedDelay = 2000)   // 每2秒执行
    public void execute(){
        Set<Long> expireCloudRecordIds = baseMapper.getExpireShare();
        if (CollectionUtils.isEmpty(expireCloudRecordIds)) {
            return;
        }

        // 删除录像文件
        this.cloudRecordService.deleteFileByIds(expireCloudRecordIds);
    }

}
