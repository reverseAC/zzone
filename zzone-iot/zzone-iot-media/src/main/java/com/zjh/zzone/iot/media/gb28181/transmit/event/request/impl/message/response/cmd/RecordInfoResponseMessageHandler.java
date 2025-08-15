package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response.cmd;

import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.vo.RecordInfo;
import com.ylg.iot.vo.RecordItem;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.event.media.RecordInfoEndEvent;
import com.ylg.iot.media.event.media.RecordInfoEvent;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.ylg.iot.media.utils.DateUtil;
import com.ylg.redis.util.RedisUtils;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ylg.iot.media.utils.XmlUtil.getText;

/**
 * 录像查询的回复
 *
 * @author zjh
 * @since 2025-07-05 16:41
 */
@Slf4j
@Component
public class RecordInfoResponseMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {

    private final String cmdType = "RecordInfo";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private Long recordInfoTtl = 1800L;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element rootElement) {
        try {
            // 回复200 OK
             responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 国标录像: {}", e.getMessage());
        }
        try {
            String sn = getText(rootElement, "SN");
            String channelId = getText(rootElement, "DeviceID");
            RecordInfo recordInfo = new RecordInfo();
            recordInfo.setChannelId(channelId);
            recordInfo.setDeviceId(device.getGbId());
            recordInfo.setSn(sn);
            recordInfo.setName(getText(rootElement, "Name"));
            String sumNumStr = getText(rootElement, "SumNum");
            int sumNum = 0;
            if (!ObjectUtils.isEmpty(sumNumStr)) {
                sumNum = Integer.parseInt(sumNumStr);
            }
            recordInfo.setSumNum(sumNum);
            Element recordListElement = rootElement.element("RecordList");
            if (recordListElement == null || sumNum == 0) {
                log.info("无录像数据");
                recordInfo.setCount(sumNum);
                recordInfoEventPush(recordInfo);
                recordInfoEndEventPush(recordInfo);
            } else {
                Iterator<Element> recordListIterator = recordListElement.elementIterator();
                if (recordListIterator != null) {
                    List<RecordItem> recordList = new ArrayList<>();
                    // 遍历DeviceList
                    while (recordListIterator.hasNext()) {
                        Element itemRecord = recordListIterator.next();
                        Element recordElement = itemRecord.element("DeviceID");
                        if (recordElement == null) {
                            log.info("记录为空，下一个...");
                            continue;
                        }
                        RecordItem record = new RecordItem();
                        record.setDeviceId(getText(itemRecord, "DeviceID"));
                        record.setName(getText(itemRecord, "Name"));
                        record.setFilePath(getText(itemRecord, "FilePath"));
                        record.setFileSize(getText(itemRecord, "FileSize"));
                        record.setAddress(getText(itemRecord, "Address"));

                        String startTimeStr = getText(itemRecord, "StartTime");
                        record.setStartTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(startTimeStr));

                        String endTimeStr = getText(itemRecord, "EndTime");
                        record.setEndTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(endTimeStr));

                        record.setSecrecy(itemRecord.element("Secrecy") == null ? 0
                                : Integer.parseInt(getText(itemRecord, "Secrecy")));
                        record.setType(getText(itemRecord, "Type"));
                        record.setRecorderId(getText(itemRecord, "RecorderID"));
                        recordList.add(record);
                    }
                    Map<String, RecordItem> map = recordList.stream()
                            .filter(record -> record.getDeviceId() != null)
                            .collect(Collectors.toMap(record -> record.getStartTime()+ record.getEndTime(), Function.identity()));
                    // 获取任务结果数据
                    // 录像信息可能分多次上报，汇总后进行发送
                    String resKey = MediaCacheConstants.REDIS_RECORD_INFO_RES_PREFIX + channelId + sn;
                    RedisUtils.setHashMap(resKey, map);
                    RedisUtils.setExpire(resKey, recordInfoTtl, TimeUnit.SECONDS);
                    String resCountKey = MediaCacheConstants.REDIS_RECORD_INFO_RES_COUNT_PREFIX + channelId + sn;
                    long incr = RedisUtils.getNextSequence(resCountKey, map.size());
                    RedisUtils.setExpire(resCountKey, recordInfoTtl, TimeUnit.SECONDS);
                    recordInfo.setRecordList(recordList);
                    recordInfo.setCount(Math.toIntExact(incr));
                    recordInfoEventPush(recordInfo);
                    log.info("录像累加数目：" + incr);
                    if (incr < sumNum) {
                        return;
                    }
                    // 已接收完成
                    List<RecordItem> resList = new ArrayList<>(RedisUtils.getHashMap(resKey, RecordItem.class).values());
                    if (resList.size() < sumNum) {
                        return;
                    }
                    recordInfo.setRecordList(resList);
                    recordInfoEndEventPush(recordInfo);
                }
            }
        } catch (Exception e) {
            log.error("[国标录像] 发现未处理的异常, \r\n{}", evt.getRequest());
            log.error("[国标录像] 异常内容： ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element element) {

    }

    private void recordInfoEventPush(RecordInfo recordInfo) {
        if (recordInfo == null) {
            return;
        }
        if(recordInfo.getRecordList() != null) {
            Collections.sort(recordInfo.getRecordList());
        } else {
            recordInfo.setRecordList(new ArrayList<>());
        }
        RecordInfoEvent outEvent = new RecordInfoEvent(this);
        outEvent.setRecordInfo(recordInfo);
        applicationEventPublisher.publishEvent(outEvent);
    }

    private void recordInfoEndEventPush(RecordInfo recordInfo) {
        if (recordInfo == null) {
            return;
        }
        if(recordInfo.getRecordList() != null) {
            Collections.sort(recordInfo.getRecordList());
        } else {
            recordInfo.setRecordList(new ArrayList<>());
        }
        RecordInfoEndEvent outEvent = new RecordInfoEndEvent(this);
        outEvent.setRecordInfo(recordInfo);
        applicationEventPublisher.publishEvent(outEvent);
    }
}
