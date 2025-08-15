package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response.cmd;

import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.bo.SipRequestCacheData;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.media.bo.SyncStatus;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.gb28181.parser.CatalogChannelParser;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.gb28181.session.CatalogDataManager;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.response.ResponseMessageHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 通道查询的回复
 * @author zjh
 * @since 2025-04-09 16:41
 */
@Slf4j
@Component
public class CatalogResponseMessageHandler extends SIPRequestParentProcessor implements InitializingBean, MessageHandler {

    private final String cmdType = "Catalog";

    @Autowired
    private ResponseMessageHandler responseMessageHandler;

    private final ConcurrentLinkedQueue<SipRequestCacheData> taskQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private MediaDeviceChannelService deviceChannelService;

    @Autowired
    private CatalogDataManager catalogDataCache;

    @Autowired
    private SipConfig sipConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        responseMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element element) {
        log.info("接收到目录回复消息：{}", evt.getRequest());
        taskQueue.offer(new SipRequestCacheData(evt, device, element));
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 目录查询回复: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void executeTaskQueue() {
        log.debug("Scheduled: CatalogResponseMessageHandler...");
        if (taskQueue.isEmpty()) {
            return;
        }
        List<SipRequestCacheData> handlerCatchDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            SipRequestCacheData poll = taskQueue.poll();
            if (poll != null) {
                handlerCatchDataList.add(poll);
            }
        }
        log.info("开始处理目录查询回复，共{}条", handlerCatchDataList.size());
        if (handlerCatchDataList.isEmpty()) {
            return;
        }
        for (SipRequestCacheData take : handlerCatchDataList) {
            if (take == null) {
                continue;
            }
            RequestEvent evt = take.getEvt();
            int sn = 0;
            // 全局异常捕获，保证下一条可以得到处理
            try {
                Element rootElement = null;
                try {
                    rootElement = getRootElement(take.getEvt(), take.getDevice().getCharset());
                } catch (DocumentException e) {
                    log.error("[xml解析] 失败： ", e);
                    continue;
                }
                if (rootElement == null) {
                    log.warn("[ 收到通道 ] content cannot be null, {}", evt.getRequest());
                    continue;
                }
                Element deviceListElement = rootElement.element("DeviceList");
                Element sumNumElement = rootElement.element("SumNum");
                Element snElement = rootElement.element("SN");
                int sumNum = Integer.parseInt(sumNumElement.getText());

                if (sumNum == 0) {
                    log.info("[收到通道]设备:{}的: 0个", take.getDevice().getGbId());
                    // 数据已经完整接收
                    deviceChannelService.deleteDeviceChannels(take.getDevice().getId());
                    catalogDataCache.setChannelSyncEnd(take.getDevice().getGbId(), sn, null);
                } else {
                    Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
                    if (deviceListIterator != null) {
                        List<MediaDeviceChannel> channelList = new ArrayList<>();
                        // 遍历DeviceList
                        while (deviceListIterator.hasNext()) {
                            Element itemDevice = deviceListIterator.next();
                            Element channelDeviceElement = itemDevice.element("DeviceID");
                            if (channelDeviceElement == null) {
                                continue;
                            }
                            // 从xml解析内容到 DeviceChannel 对象
                            MediaDeviceChannel channel = CatalogChannelParser.decode(itemDevice);
                            if (channel.getGbId() == null) {
                                log.info("[收到目录订阅]：但是解析失败 {}", new String(evt.getRequest().getRawContent()));
                                continue;
                            }
                            channel.setParentId(take.getDevice().getId());
                            channel.setParentGbId(take.getDevice().getGbId());
                            channelList.add(channel);
                        }
                        sn = Integer.parseInt(snElement.getText());
                        catalogDataCache.put(take.getDevice().getGbId(), sn, sumNum, take.getDevice(),
                                channelList);
                        log.info("[收到通道]设备: {} -> {}个，{}/{}", take.getDevice().getGbId(), channelList.size(), catalogDataCache.size(take.getDevice().getGbId(), sn), sumNum);
                    }
                }
            } catch (Exception e) {
                log.warn("[收到通道] 发现未处理的异常, \r\n{}", evt.getRequest());
                log.error("[收到通道] 异常内容： ", e);
            } finally {
                if (catalogDataCache.size(take.getDevice().getGbId(), sn) == catalogDataCache.sumNum(take.getDevice().getGbId(), sn)) {
                    // 数据已经完整接收， 此时可能存在某个设备离线变上线的情况，但是考虑到性能，此处不做处理，
                    // 目前支持设备通道上线通知时和设备上线时向上级通知
                    boolean resetChannelsResult = saveData(take.getDevice(), sn);
                    if (!resetChannelsResult) {
                        String errorMsg = "接收成功，写入失败，共" + catalogDataCache.sumNum(take.getDevice().getGbId(), sn) + "条，已接收" + catalogDataCache.getDeviceChannelList(take.getDevice().getGbId(), sn).size() + "条";
                        catalogDataCache.setChannelSyncEnd(take.getDevice().getGbId(), sn, errorMsg);
                    } else {
                        catalogDataCache.setChannelSyncEnd(take.getDevice().getGbId(), sn, null);
                    }
                }
            }
        }
    }

    @Transactional
    public boolean saveData(MediaDevice device, int sn) {

        boolean result = true;
        List<MediaDeviceChannel> deviceChannelList = catalogDataCache.getDeviceChannelList(device.getGbId(), sn);
        if (deviceChannelList != null && !deviceChannelList.isEmpty()) {
            result &= deviceChannelService.resetChannels(device.getId(), deviceChannelList);
        }
        return result;
    }

    @Override
    public void handForPlatform(RequestEvent evt, Platform parentPlatform, Element rootElement) {

    }

    public SyncStatus getChannelSyncProgress(String gbId) {
        return catalogDataCache.getSyncStatus(gbId);
    }

    public boolean isSyncRunning(String gbId) {
        return catalogDataCache.isSyncRunning(gbId);
    }

    public void setChannelSyncReady(MediaDevice device, int sn) {
        catalogDataCache.addReady(device, sn);
    }

    public void setChannelSyncEnd(String deviceId, int sn, String errorMsg) {
        catalogDataCache.setChannelSyncEnd(deviceId, sn, errorMsg);
    }


}
