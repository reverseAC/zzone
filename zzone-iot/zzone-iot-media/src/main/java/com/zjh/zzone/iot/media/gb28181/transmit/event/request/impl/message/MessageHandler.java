package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message;

import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.entity.MediaDevice;
import org.dom4j.Element;

import javax.sip.RequestEvent;

/**
 * MESSAGE类型消息处理 接口
 * <p>
 * MESSAGE消息包含多种CmdType：
 * <li>Catalog 获取目录></li>
 * <li>DeviceStatus 获取设备状态></li>
 * <li>DeviceInfo 获取设备信息></li>
 * <li>RecordInfo 获取录像目录信息></li>
 * <li>MobilePosition 获取移动位置></li>
 * <li>DeviceControl 设备控制></li>
 * <li>等等，详见GBT+28181</li>
 */
public interface MessageHandler {
    /**
     * 处理来自设备的信息
     * @param event MESSAGE请求
     * @param device 设备信息
     */
    void handForDevice(RequestEvent event, MediaDevice device, Element element);

    /**
     * 处理来自平台的信息
     * @param event MESSAGE请求
     * @param parentPlatform 平台信息
     */
    void handForPlatform(RequestEvent event, Platform parentPlatform, Element element);
}
