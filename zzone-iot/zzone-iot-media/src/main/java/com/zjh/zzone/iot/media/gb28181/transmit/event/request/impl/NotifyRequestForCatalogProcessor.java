package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.core.enums.StatusEnum;
import com.ylg.iot.enums.DeviceStatusEnum;
import com.ylg.iot.media.bo.CatalogChannelEvent;
import com.ylg.iot.media.bo.NotifyCatalogChannel;
import com.ylg.iot.media.event.CatalogEvent;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.bo.SipRequestCacheData;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.media.gb28181.event.EventPublisher;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.iot.media.vo.MediaDeviceVO;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.redis.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sip.RequestEvent;
import javax.sip.header.FromHeader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.ylg.iot.constant.MediaCacheConstants.DEVICE_PREFIX;

/**
 * SIP命令类型： NOTIFY请求中的目录请求处理
 *
 * @author zjh
 * @since 2025-06-20 13:47
 */
@Slf4j
@Component
public class NotifyRequestForCatalogProcessor extends SIPRequestParentProcessor {

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private MediaDeviceChannelService deviceChannelService;

    private final ConcurrentLinkedQueue<NotifyCatalogChannel> channelList = new ConcurrentLinkedQueue<>();

	private final ConcurrentLinkedQueue<SipRequestCacheData> notifyCatalogQueue = new ConcurrentLinkedQueue<>();


	public void process(RequestEvent evt) {
		if (notifyCatalogQueue.size() >= userSetting.getMaxNotifyCountQueue()) {
			log.error("[notify-目录订阅] 待处理消息队列已满 {}，返回486 BUSY_HERE，消息不做处理", userSetting.getMaxNotifyCountQueue());
			return;
		}
		notifyCatalogQueue.offer(new SipRequestCacheData(evt, null, null));
	}

	@Scheduled(fixedDelay = 400)   //每400毫秒执行一次
	public void executeTaskQueue(){
		log.debug("Scheduled: NotifyRequestForCatalogProcessor...");
		if (notifyCatalogQueue.isEmpty()) {
			return;
		}
		List<SipRequestCacheData> handlerCatchDataList = new ArrayList<>();
		int size = notifyCatalogQueue.size();
		for (int i = 0; i < size; i++) {
			SipRequestCacheData poll = notifyCatalogQueue.poll();
			if (poll != null) {
				handlerCatchDataList.add(poll);
			}
		}
		if (handlerCatchDataList.isEmpty()) {
			return;
		}
		for (SipRequestCacheData take : handlerCatchDataList) {
			if (take == null) {
				continue;
			}
			RequestEvent evt = take.getEvt();
			try {
				FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
				String gbId = SipUtils.getUserIdFromFromHeader(fromHeader);

				MediaDevice device = RedisUtils.getObject(DEVICE_PREFIX + gbId, MediaDeviceVO.class);;
				if (device == null || DeviceStatusEnum.OFFLINE.getCode().equals(device.getOnline())) {
					log.warn("[收到目录订阅]：{}, 但是设备已经离线", (device != null ? device.getGbId() : ""));
					continue;
				}
				Element rootElement = getRootElement(evt, device.getCharset());
				if (rootElement == null) {
					log.warn("[ 收到目录订阅 ] content cannot be null, {}", evt.getRequest());
					continue;
				}
				Element deviceListElement = rootElement.element("DeviceList");
				if (deviceListElement == null) {
					log.warn("[ 收到目录订阅 ] content cannot be null, {}", evt.getRequest());
					continue;
				}
				Iterator<Element> deviceListIterator = deviceListElement.elementIterator();
				if (deviceListIterator != null) {

					// 遍历DeviceList
					while (deviceListIterator.hasNext()) {
						Element itemDevice = deviceListIterator.next();
						CatalogChannelEvent catalogChannelEvent = null;
                        try {
                            catalogChannelEvent = CatalogChannelEvent.decode(itemDevice);
							if (catalogChannelEvent.getChannel() == null) {
								log.info("[解析CatalogChannelEvent]成功：但是解析通道信息失败， 原文如下： \n{}", new String(evt.getRequest().getRawContent()));
								continue;
							}
							catalogChannelEvent.getChannel().setParentId(device.getId());
                        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                                 IllegalAccessException e) {
                            log.error("[解析CatalogChannelEvent]失败，", e);
                            log.error("[解析CatalogChannelEvent]失败原文: \n{}", new String(evt.getRequest().getRawContent(), Charset.forName(device.getCharset())));
							continue;
                        }
						if (log.isDebugEnabled()){
							log.debug("[收到目录订阅]：{}/{}-{}", device.getGbId(),
									catalogChannelEvent.getChannel().getGbId(), catalogChannelEvent.getEvent());
						}
						MediaDeviceChannel channel = catalogChannelEvent.getChannel();
						switch (catalogChannelEvent.getEvent()) {
							case CatalogEvent.ON:
								// 上线
								log.info("[收到通道上线通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								channel.setStatus(StatusEnum.ENABLE.getCode());
								channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
								break;
							case CatalogEvent.OFF:
								// 离线
								log.info("[收到通道离线通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[收到通道离线通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								} else {
									channel.setStatus(StatusEnum.DISABLE.getCode());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
								}
								break;
							case CatalogEvent.ADD:
								// 增加
								log.info("[收到增加通道通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								// 判断此通道是否存在
								MediaDeviceChannel deviceChannel = deviceChannelService.getChannelByGbId(device.getId(), catalogChannelEvent.getChannel().getGbId());
								if (deviceChannel != null) {
									log.info("[增加通道] 已存在，不发送通知只更新，设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
									channel.setId(deviceChannel.getId());
									channel.setHasAudio(deviceChannel.isHasAudio());
									channel.setUpdateTime(LocalDateTime.now());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel));
								} else {
									catalogChannelEvent.getChannel().setUpdateTime(LocalDateTime.now());
									catalogChannelEvent.getChannel().setCreateTime(LocalDateTime.now());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel));
								}

								break;
							case CatalogEvent.UPDATE:
								// 更新
								log.info("[收到更新通道通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								// 判断此通道是否存在
								MediaDeviceChannel deviceChannelForUpdate = deviceChannelService.getChannelByGbId(device.getId(), catalogChannelEvent.getChannel().getGbId());
								if (deviceChannelForUpdate != null) {
									channel.setId(deviceChannelForUpdate.getId());
									channel.setHasAudio(deviceChannelForUpdate.isHasAudio());
									channel.setUpdateTime(LocalDateTime.now());
									channel.setUpdateTime(LocalDateTime.now());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.UPDATE, channel));
								} else {
									catalogChannelEvent.getChannel().setCreateTime(LocalDateTime.now());
									catalogChannelEvent.getChannel().setUpdateTime(LocalDateTime.now());
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.ADD, channel));
								}
								break;
							case CatalogEvent.DEL:
								// 删除
								log.info("[收到删除通道通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.DELETE, channel));
								break;
							case CatalogEvent.VLOST:
								// 视频丢失
								log.info("[收到通道视频丢失通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[收到通道视频丢失通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
								}
								break;
							case CatalogEvent.DEFECT:
								// 故障
								log.info("[收到通道视频故障通知] 来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								if (userSetting.getRefuseChannelStatusChannelFormNotify()) {
									log.info("[收到通道视频故障通知] 但是平台已配置拒绝此消息，来自设备: {}, 通道 {}", device.getGbId(), catalogChannelEvent.getChannel().getGbId());
								} else {
									channel.setStatus("OFF");
									channelList.add(NotifyCatalogChannel.getInstance(NotifyCatalogChannel.Type.STATUS_CHANGED, channel));
								}
								break;
							default:
								log.warn("[ NotifyCatalog ] event not found ： {}", catalogChannelEvent.getEvent());

						}
						// NOTE：如有级联逻辑，可在此处发送消息
					}
				}

			} catch (DocumentException e) {
				log.error("未处理的异常 ", e);
			}
		}
		if (!channelList.isEmpty()) {
			executeSave();
		}
	}

	@Transactional
	public void executeSave() {
		int size = channelList.size();
		List<NotifyCatalogChannel> channelListForSave = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			channelListForSave.add(channelList.poll());
		}

		for (NotifyCatalogChannel notifyCatalogChannel : channelListForSave) {
			try {
				switch (notifyCatalogChannel.getType()) {
					case STATUS_CHANGED:
						deviceChannelService.updateChannelStatus(notifyCatalogChannel.getChannel());
						break;
					case ADD:
						deviceChannelService.addChannel(notifyCatalogChannel.getChannel());
						break;
					case UPDATE:
						deviceChannelService.updateChannelForNotify(notifyCatalogChannel.getChannel());
						break;
					case DELETE:
						deviceChannelService.removeById(notifyCatalogChannel.getChannel());
						break;
				}
			}catch (Exception e) {
				log.error("[存储收到的通道]类型：{}，编号：{}", notifyCatalogChannel.getType(),
						notifyCatalogChannel.getChannel().getGbId(), e);
			}
		}
	}
}
