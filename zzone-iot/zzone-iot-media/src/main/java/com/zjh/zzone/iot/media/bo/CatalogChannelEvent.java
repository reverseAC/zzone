package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.media.event.CatalogEvent;
import com.ylg.iot.media.gb28181.parser.CatalogChannelParser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class CatalogChannelEvent extends MediaDeviceChannel {

    private String event;

    private MediaDeviceChannel channel;

    public static CatalogChannelEvent decode(Element element) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Element eventElement = element.element("Event"); // Event字段，非SIP协议标准，但实际设备中很常见，属于常见的拓展字段
        CatalogChannelEvent catalogChannelEvent = new CatalogChannelEvent();
        if (eventElement != null) {
            catalogChannelEvent.setEvent(eventElement.getText());
        } else {
            catalogChannelEvent.setEvent(CatalogEvent.ADD); // 如果不设备不携带此属性
        }
        MediaDeviceChannel deviceChannel;
        if (CatalogEvent.ADD.equalsIgnoreCase(catalogChannelEvent.getEvent()) ||
                CatalogEvent.UPDATE.equalsIgnoreCase(catalogChannelEvent.getEvent()) ){
            deviceChannel = CatalogChannelParser.decode(element);
        } else {
            deviceChannel = CatalogChannelParser.decodeWithOnlyDeviceId(element);
        }
        catalogChannelEvent.setChannel(deviceChannel);
        return catalogChannelEvent;
    }
}
