package com.zjh.zzone.iot.media.gb28181.transmit.event.request;

import com.google.common.primitives.Bytes;
import com.ylg.iot.media.bo.Platform;
import com.ylg.iot.media.gb28181.transmit.SIPSender;
import com.ylg.iot.media.utils.SipUtils;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SIP请求处理器父类
 *
 * @author zjh
 * @since 2025-03-24 15:36
 */
@Slf4j
public abstract class SIPRequestParentProcessor {

    @Autowired
    private SIPSender sipSender;

    /**
     * 获取SIP请求头工厂，用于创建SIP协议的请求Request和响应Response消息头对象
     *
     * @return SIP请求头工厂
     */
    public HeaderFactory getHeaderFactory() {
        try {
            return SipFactory.getInstance().createHeaderFactory();
        } catch (PeerUnavailableException e) {
            log.error("未处理的异常 ", e);
        }
        return null;
    }

    /**
     * 获取SIP消息工厂，用于创建SIP协议的请求Request和响应Response消息对象
     *
     * @return SIP消息工厂
     */
    public MessageFactory getMessageFactory() {
        try {
            return SipFactory.getInstance().createMessageFactory();
        } catch (PeerUnavailableException e) {
            log.error("未处理的异常 ", e);
        }
        return null;
    }

    static class ResponseAckExtraParam{
        String content;
        ContentTypeHeader contentTypeHeader;
        SipURI sipURI;
        int expires = -1;
    }

    /***
     * 回复状态码
     * 200 OK
     */
    public SIPResponse responseAck(SIPRequest sipRequest, int statusCode) throws SipException, InvalidArgumentException, ParseException {
        return responseAck(sipRequest, statusCode, null);
    }

    public SIPResponse responseAck(SIPRequest sipRequest, int statusCode, String msg) throws SipException, InvalidArgumentException, ParseException {
        return responseAck(sipRequest, statusCode, msg, null);
    }

    public SIPResponse responseAck(SIPRequest sipRequest, int statusCode, String msg, ResponseAckExtraParam responseAckExtraParam) throws SipException, InvalidArgumentException, ParseException {
        if (sipRequest.getToHeader().getTag() == null) {
            sipRequest.getToHeader().setTag(SipUtils.getNewTag());
        }
        SIPResponse response = (SIPResponse)getMessageFactory().createResponse(statusCode, sipRequest);
        response.setStatusCode(statusCode);
        if (msg != null) {
            response.setReasonPhrase(msg);
        }

        if (responseAckExtraParam != null) {
            if (responseAckExtraParam.sipURI != null && sipRequest.getMethod().equals(Request.INVITE)) {
                log.debug("responseSdpAck SipURI: {}:{}", responseAckExtraParam.sipURI.getHost(), responseAckExtraParam.sipURI.getPort());
                Address concatAddress = SipFactory.getInstance().createAddressFactory().createAddress(
                        SipFactory.getInstance().createAddressFactory().createSipURI(responseAckExtraParam.sipURI.getUser(),  responseAckExtraParam.sipURI.getHost()+":"+responseAckExtraParam.sipURI.getPort()
                        ));
                response.addHeader(SipFactory.getInstance().createHeaderFactory().createContactHeader(concatAddress));
            }
            if (responseAckExtraParam.contentTypeHeader != null) {
                response.setContent(responseAckExtraParam.content, responseAckExtraParam.contentTypeHeader);
            }

            if (sipRequest.getMethod().equals(Request.SUBSCRIBE)) {
                if (responseAckExtraParam.expires == -1) {
                    log.error("[参数不全] 2xx的SUBSCRIBE回复，必须设置Expires header");
                } else {
                    ExpiresHeader expiresHeader = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(responseAckExtraParam.expires);
                    response.addHeader(expiresHeader);
                }
            }
        } else {
            if (sipRequest.getMethod().equals(Request.SUBSCRIBE)) {
                log.error("[参数不全] 2xx的SUBSCRIBE回复，必须设置Expires header");
            }
        }

        // 发送response
        sipSender.transmitRequest(sipRequest.getLocalAddress().getHostAddress(), response);

        return response;
    }

    /**
     * 回复带sdp的200
     */
    public SIPResponse responseSdpAck(SIPRequest request, String sdp, Platform platform) throws SipException, InvalidArgumentException, ParseException {

        ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("APPLICATION", "SDP");

        // 兼容国标中的使用编码@域名作为RequestURI的情况
        SipURI sipURI = (SipURI)request.getRequestURI();
        if (sipURI.getPort() == -1) {
            sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(),  platform.getServerIp()+":"+platform.getServerPort());
        }
        ResponseAckExtraParam responseAckExtraParam = new ResponseAckExtraParam();
        responseAckExtraParam.contentTypeHeader = contentTypeHeader;
        responseAckExtraParam.content = sdp;
        responseAckExtraParam.sipURI = sipURI;

        SIPResponse sipResponse = responseAck(request, Response.OK, null, responseAckExtraParam);

        return sipResponse;
    }

    /**
     * 回复带xml的200
     */
    public SIPResponse responseXmlAck(SIPRequest request, String xml, Platform platform, Integer expires) throws SipException, InvalidArgumentException, ParseException {
        ContentTypeHeader contentTypeHeader = SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("Application", "MANSCDP+xml");

        SipURI sipURI = (SipURI)request.getRequestURI();
        if (sipURI.getPort() == -1) {
            sipURI = SipFactory.getInstance().createAddressFactory().createSipURI(platform.getServerGBId(),  platform.getServerIp()+":"+platform.getServerPort());
        }
        ResponseAckExtraParam responseAckExtraParam = new ResponseAckExtraParam();
        responseAckExtraParam.contentTypeHeader = contentTypeHeader;
        responseAckExtraParam.content = xml;
        responseAckExtraParam.sipURI = sipURI;
        responseAckExtraParam.expires = expires;
        return responseAck(request, Response.OK, null, responseAckExtraParam);
    }

    public Element getRootElement(RequestEvent evt) throws DocumentException {
        return getRootElement(evt, "gb2312");
    }

    /**
     * 获取SIP请求xml的根节点
     * @param evt SIP请求对象
     * @param charset 字符集
     * @return  根节点
     */
    public Element getRootElement(RequestEvent evt, String charset) throws DocumentException {

        byte[] rawContent = evt.getRequest().getRawContent();
        if (evt.getRequest().getContentLength().getContentLength() == 0
                || rawContent == null
                || rawContent.length == 0
                || ObjectUtils.isEmpty(new String(rawContent))) {
            return null;
        }

        if (charset == null) {
            charset = "gb2312";
        }
        SAXReader reader = new SAXReader();
        reader.setEncoding(charset);
        // 对海康出现的未转义字符做处理。
        String[] destStrArray = new String[]{"&lt;","&gt;","&amp;","&apos;","&quot;"};
        // 或许可扩展兼容其他字符
        char despChar = '&';
        byte destBye = (byte) despChar;
        List<Byte> result = new ArrayList<>();
        for (int i = 0; i < rawContent.length; i++) {
            if (rawContent[i] == destBye) {
                boolean resul = false;
                for (String destStr : destStrArray) {
                    if (i + destStr.length() <= rawContent.length) {
                        byte[] bytes = Arrays.copyOfRange(rawContent, i, i + destStr.length());
                        resul = resul || (Arrays.equals(bytes,destStr.getBytes()));
                    }
                }
                if (resul) {
                    result.add(rawContent[i]);
                }
            } else {
                result.add(rawContent[i]);
            }
        }
        byte[] bytesResult = Bytes.toArray(result);

        Document xml;
        try {
            xml = reader.read(new ByteArrayInputStream(bytesResult));
        } catch (DocumentException e) {
            log.warn("[xml解析异常]： 原文如下： \r\n{}", new String(bytesResult));
            log.warn("[xml解析异常]： 原文如下： 尝试兼容性处理");
            String[] xmlLineArray = new String(bytesResult).split("\\r?\\n");

            // 兼容海康的address字段带有<破换xml结构导致无法解析xml的问题
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : xmlLineArray) {
                if (s.startsWith("<Address")) {
                    continue;
                }
                stringBuilder.append(s);
            }
            xml = reader.read(new ByteArrayInputStream(stringBuilder.toString().getBytes()));
        }
        return xml.getRootElement();
    }

}
