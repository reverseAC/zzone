package com.zjh.zzone.iot.media.gb28181.transmit;

import com.ylg.iot.media.gb28181.event.EventPublisher;
import com.ylg.iot.media.callback.SipSubscribe;
import com.ylg.iot.media.gb28181.event.sip.SipEvent;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import com.ylg.iot.media.gb28181.transmit.event.response.SIPResponseProcessor;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sip.*;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIP消息处理器，统一收发设备SIP消息
 *
 * @author zjh
 * @since 2025-03-24 15:27
 */
@Slf4j
@Component
public class SIPProcessorHandler implements SipListener {

    private static final Map<String, SIPRequestProcessor> requestProcessorMap = new ConcurrentHashMap<>();
    private static final Map<String, SIPResponseProcessor> responseProcessorMap = new ConcurrentHashMap<>();

    @Autowired
    private SipSubscribe sipSubscribe;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * 添加 request订阅
     * @param method 方法名
     * @param processor 处理程序
     */
    public void addRequestProcessor(String method, SIPRequestProcessor processor) {
        requestProcessorMap.put(method, processor);
    }

    /**
     * 添加 response订阅
     * @param method 方法名
     * @param processor 处理程序
     */
    public void addResponseProcessor(String method, SIPResponseProcessor processor) {
        responseProcessorMap.put(method, processor);
    }

    /**
     * 监听SIP请求：INVITE、REGISTER、MESSAGE、OPTIONS等请求
     *
     * @param requestEvent RequestEvent事件
     */
    @Override
    @Async("taskExecutor")
    public void processRequest(RequestEvent requestEvent) {
        log.info("收到请求：" + (SIPRequest) requestEvent.getRequest());
        String method = requestEvent.getRequest().getMethod();
        SIPRequestProcessor sipRequestProcessor = requestProcessorMap.get(method);
        if (sipRequestProcessor == null) {
            log.warn("不支持方法{}的request", method);
            return;
        }
        requestProcessorMap.get(method).process(requestEvent);
    }

    /**
     * 监听SIP响应
     * 响应码参考：{@link javax.sip.message.Response}
     *
     * @param responseEvent responseEvent事件
     */
    @Override
    @Async("taskExecutor")
    public void processResponse(ResponseEvent responseEvent) {
        SIPResponse response = (SIPResponse)responseEvent.getResponse();
        log.info("收到响应：" + response);
        int status = response.getStatusCode();

        // Success
        if (((status >= Response.OK) && (status < Response.MULTIPLE_CHOICES)) || status == Response.UNAUTHORIZED) {
            if (status != Response.UNAUTHORIZED && responseEvent.getResponse() != null && !sipSubscribe.isEmpty() ) {
                CallIdHeader callIdHeader = response.getCallIdHeader();
                CSeqHeader cSeqHeader = response.getCSeqHeader();
                if (callIdHeader != null) {
                    SipEvent sipEvent = sipSubscribe.getSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                    if (sipEvent != null) {
                        if (sipEvent.getOkEvent() != null) {
                            SipSubscribe.EventResult<ResponseEvent> eventResult = new SipSubscribe.EventResult<>(responseEvent);
                            sipEvent.getOkEvent().response(eventResult);
                        }
                        sipSubscribe.removeSubscribe(callIdHeader.getCallId());
                    }
                }
            }
            SIPResponseProcessor sipRequestProcessor = responseProcessorMap.get(response.getCSeqHeader().getMethod());
            if (sipRequestProcessor != null) {
                sipRequestProcessor.process(responseEvent);
            }
        } else if ((status >= Response.TRYING) && (status < Response.OK)) {
            // ACK回复，无需处理
        } else {
            log.warn("接收到失败的response响应！status：{},message:{}", status, response.getReasonPhrase());
            if (responseEvent.getResponse() != null && !sipSubscribe.isEmpty() ) {
                CallIdHeader callIdHeader = response.getCallIdHeader();
                CSeqHeader cSeqHeader = response.getCSeqHeader();
                if (callIdHeader != null) {
                    SipEvent sipEvent = sipSubscribe.getSubscribe(callIdHeader.getCallId() + cSeqHeader.getSeqNumber());
                    if (sipEvent != null ) {
                        if (sipEvent.getErrorEvent() != null) {
                            SipSubscribe.EventResult<ResponseEvent> eventResult = new SipSubscribe.EventResult<>(responseEvent);
                            sipEvent.getErrorEvent().response(eventResult);
                        }
                        sipSubscribe.removeSubscribe(callIdHeader.getCallId());
                    }
                }
            }
            if (responseEvent.getDialog() != null) {
                responseEvent.getDialog().delete();
            }
        }
    }

    /**
     * 监听SIP事务超时
     *
     * @param timeoutEvent timeoutEvent事件
     */
    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("[消息发送超时]");
    }

    /**
     * 监听IO异常
     *
     * @param exceptionEvent exceptionEvent事件
     */
    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("[IO异常]");
    }

    /**
     * 监听SIP事务终止
     *
     * @param transactionTerminatedEvent transactionTerminatedEvent事件
     */
    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("[事务终止]");
    }

    /**
     * 监听SIP对话终止
     *
     * @param dialogTerminatedEvent dialogTerminatedEvent事件
     */
    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        CallIdHeader callId = dialogTerminatedEvent.getDialog().getCallId();
        log.info("[对话终止] callId:{}", callId);
    }
}