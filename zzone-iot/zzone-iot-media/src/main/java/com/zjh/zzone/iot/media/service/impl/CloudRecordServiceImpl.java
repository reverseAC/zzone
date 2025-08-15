package com.zjh.zzone.iot.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ylg.core.exception.CheckedException;
import com.ylg.core.utils.StringUtils;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.entity.RecordShareLog;
import com.ylg.iot.enums.media.HookType;
import com.ylg.iot.media.bo.StreamAuthorityInfo;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.media.callback.Hook;
import com.ylg.iot.media.contant.ResultCode;
import com.ylg.iot.media.dto.CloudRecordDTO;
import com.ylg.iot.media.event.media.MediaRecordMp4Event;
import com.ylg.iot.media.callback.HookSubscribe;
import com.ylg.iot.entity.CloudRecord;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.mapper.CloudRecordMapper;
import com.ylg.iot.media.service.CloudRecordService;
import com.ylg.iot.media.service.MediaServerService;
import com.ylg.iot.media.service.RecordShareLogService;
import com.ylg.iot.media.utils.CloudRecordUtils;
import com.ylg.iot.media.utils.DateUtil;
import com.ylg.iot.vo.DownloadFileInfo;
import com.ylg.iot.vo.MediaInfo;
import com.ylg.mybatis.base.BaseServiceImpl;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class CloudRecordServiceImpl extends BaseServiceImpl<CloudRecordMapper, CloudRecord> implements CloudRecordService {

    @Autowired
    private MediaServerService mediaServerService;

    @Autowired
    private ServerInstanceConfig serverInstance;

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private RecordShareLogService recordShareLogService;


    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaRecordMp4Event event) {
        CloudRecord cloudRecord = event.getCloudRecord();
        if (ObjectUtils.isEmpty(cloudRecord.getCallId())) {
            StreamAuthorityInfo streamAuthorityInfo = RedisUtils.getHashKey(
                    MediaCacheConstants.MEDIA_STREAM_AUTHORITY + serverInstance.getInstanceId(),
                    event.getApp() + "_" + event.getStream(),
                    StreamAuthorityInfo.class);
            if (streamAuthorityInfo != null) {
                cloudRecord.setCallId(streamAuthorityInfo.getCallId());
            }
        }
        log.info("[添加录像记录] {}/{}, callId: {}, 内容：{}", event.getApp(), event.getStream(), cloudRecord.getCallId(), event.getRecordInfo());
        baseMapper.insert(cloudRecord);
    }

    @Override
    public DownloadFileInfo getPlayUrlPath(Long recordId) {
        CloudRecord recordItem = baseMapper.selectById(recordId);
        if (recordItem == null) {
            throw new CheckedException("资源不存在");
        }

        String filePath = recordItem.getFilePath();
        MediaServer mediaServerItem = mediaServerService.getServerByServerId(recordItem.getMediaServerId());
        return CloudRecordUtils.getDownloadFilePath(mediaServerItem, filePath);
    }

    @Override
    public List<CloudRecord> getAllList(CloudRecordDTO cloudRecord) {
        // 开始时间和结束时间在数据库中都是以秒为单位的
        if (cloudRecord.getStartTimeStr() != null ) {
            if (!DateUtil.verification(cloudRecord.getStartTimeStr(), DateUtil.formatter)) {
                throw new CheckedException("开始时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            long startTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(cloudRecord.getStartTimeStr());
            cloudRecord.setStartTime(startTimeStamp);
        }
        if (cloudRecord.getEndTimeStr() != null ) {
            if (!DateUtil.verification(cloudRecord.getEndTimeStr(), DateUtil.formatter)) {
                throw new CheckedException("结束时间格式错误，正确格式为： " + DateUtil.formatter);
            }
            long endTimeStamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(cloudRecord.getEndTimeStr());
            cloudRecord.setEndTime(endTimeStamp);
        }
        return baseMapper.getList(cloudRecord);
    }

    @Override
    public void loadRecord(String app, String stream, String date, ErrorCallback<StreamDetail> callback) {

        // 判断是否分享
        List<RecordShareLog> logs = recordShareLogService.list(new QueryWrapper<RecordShareLog>().eq("stream", stream));
        if (!CollectionUtils.isEmpty(logs)) {
            RecordShareLog log = logs.get(0);
            if (log.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new CheckedException("该视频分享已失效");
            }
        }

        CloudRecordDTO cloudRecordDTO = new CloudRecordDTO();
        cloudRecordDTO.setApp(app);
        cloudRecordDTO.setStream(stream);

        if (!StringUtils.isEmpty(date)) {
            long startTimestamp = DateUtil.yyyy_MM_dd_HH_mm_ssToTimestampMs(date + " 00:00:00");
            long endTimestamp = startTimestamp + 24 * 60 * 60 * 1000;
            cloudRecordDTO.setStartTime(startTimestamp);
            cloudRecordDTO.setEndTime(endTimestamp);
        }

        List<CloudRecord> recordItemList = baseMapper.getList(cloudRecordDTO);
        if (recordItemList.isEmpty()) {
            throw new CheckedException("此时间无录像");
        }
        String mediaServerId = recordItemList.get(0).getMediaServerId();
        MediaServer mediaServer = mediaServerService.getServerByServerId(mediaServerId);
        if (mediaServer == null) {
            throw new CheckedException("媒体节点不存在： " + mediaServerId);
        }
        String buildApp = "mp4_record";
        String buildStream = app + "_" + stream + "_" + date;
        MediaInfo mediaInfo = mediaServerService.getMediaInfo(mediaServer, buildApp, buildStream);
         if (mediaInfo != null) {
             if (callback != null) {
                 StreamDetail streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServer, buildApp, buildStream, mediaInfo, null);
                 callback.run(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), streamInfo);
             }
             return;
         }

        Hook hook = Hook.getInstance(HookType.on_media_arrival, buildApp, buildStream);
        subscribe.addSubscribe(hook, (hookData) -> {
            StreamDetail streamInfo = mediaServerService.getStreamInfoByAppAndStream(mediaServer, buildApp, buildStream, hookData.getMediaInfo(), null);
            if (callback != null) {
                callback.run(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), streamInfo);
            }
        });
        String dateDir = recordItemList.get(0).getFilePath().substring(0, recordItemList.get(0).getFilePath().lastIndexOf("/"));
        mediaServerService.loadMP4File(mediaServer, buildApp, buildStream, dateDir);
    }

    @Override
    public void deleteFileByIds(Set<Long> ids) {
        log.info("[删除录像文件] ids: {}", ids.toArray());
        List<CloudRecord> cloudRecordItemList = baseMapper.selectBatchIds(ids);
        if (cloudRecordItemList.isEmpty()) {
            return;
        }
        List<CloudRecord> cloudRecordItemIdListForDelete = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        for (CloudRecord cloudRecordItem : cloudRecordItemList) {
            String date = new File(cloudRecordItem.getFilePath()).getParentFile().getName();
            MediaServer mediaServer = mediaServerService.getServerByServerId(cloudRecordItem.getMediaServerId());
            try {
                boolean deleteResult = mediaServerService.deleteRecordDirectory(mediaServer, cloudRecordItem.getApp(),
                        cloudRecordItem.getStream(), date, cloudRecordItem.getFileName());
                if (deleteResult) {
                    log.warn("[录像文件] 删除磁盘文件成功： {}", cloudRecordItem.getFilePath());
                    cloudRecordItemIdListForDelete.add(cloudRecordItem);
                }
            } catch (Exception e) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(cloudRecordItem.getFileName());
            }

        }
        if (!cloudRecordItemIdListForDelete.isEmpty()) {
            baseMapper.deleteBatchIds(cloudRecordItemIdListForDelete);
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.append(" 删除失败");
            log.warn(stringBuilder.toString());
        }
    }
}
