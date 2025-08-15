package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl.message.response;

import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.message.MessageParentHandler;
import com.ylg.iot.media.gb28181.transmit.event.request.impl.MessageRequestProcessor;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;

/**
 * 命令类型： 请求动作的应答
 * 命令类型： 设备控制, 报警通知, 设备目录信息查询, 目录信息查询, 目录收到, 设备信息查询, 设备状态信息查询 ......
 */
@Component
public class ResponseMessageHandler extends MessageParentHandler implements InitializingBean  {

    private final String messageType = "Response";

    @Autowired
    private MessageRequestProcessor messageRequestProcessor;

    @Override
    public void afterPropertiesSet() throws Exception {
        messageRequestProcessor.addHandler(messageType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, MediaDevice device, Element element) {
        super.handForDevice(evt, device, element);
    }
}
