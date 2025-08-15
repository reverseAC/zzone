package com.zjh.zzone.iot.media.bo;

import com.ylg.iot.entity.MediaDevice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.dom4j.Element;

import javax.sip.RequestEvent;

/**
 * SIP消息缓存 实体
 * @author zjh
 * @since 2021/4/29
 */
@Getter
@Setter
@AllArgsConstructor
public class SipRequestCacheData {
    /**
     * 请求对象
     */
    private RequestEvent evt;

    /**
     * 设备信息
     */
    private MediaDevice device;

    /**
     * 根节点
     */
    private Element rootElement;
}
