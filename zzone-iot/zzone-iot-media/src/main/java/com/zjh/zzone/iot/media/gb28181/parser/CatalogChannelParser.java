package com.zjh.zzone.iot.media.gb28181.parser;

import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.media.bo.GbCode;
import com.ylg.iot.media.utils.XmlUtil;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;

/**
 * 设备通道信息解析器
 *
 * @author zjh
 * @since 2025-07-17 14:07
 */
public class CatalogChannelParser {

    public static MediaDeviceChannel decode(Element element) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        MediaDeviceChannel deviceChannel = XmlUtil.elementDecode(element, MediaDeviceChannel.class);
        Element statusElement = element.element("Status");
        if (statusElement != null) {
            deviceChannel.setOnline(statusElement.getText().equals("ON") ? "1" : "0");
        }

        GbCode gbCode = GbCode.decode(deviceChannel.getGbId());
        if (gbCode != null && "138".equals(gbCode.getTypeCode())) {
            deviceChannel.setHasAudio(true);
        }
        return deviceChannel;
    }

    public static MediaDeviceChannel decodeWithOnlyDeviceId(Element element) {
        Element deviceElement = element.element("DeviceID");
        MediaDeviceChannel deviceChannel = new MediaDeviceChannel();
        deviceChannel.setGbId(deviceElement.getText());
        return deviceChannel;
    }

}
