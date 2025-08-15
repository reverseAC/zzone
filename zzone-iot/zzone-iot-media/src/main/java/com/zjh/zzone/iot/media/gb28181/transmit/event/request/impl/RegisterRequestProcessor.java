package com.zjh.zzone.iot.media.gb28181.transmit.event.request.impl;

import com.ylg.iot.constant.DeviceStatusEnum;
import com.ylg.iot.media.gb28181.auth.DigestServerAuthenticationHelper;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.contant.StreamModeType;
import com.ylg.iot.media.bo.GbSipDate;
import com.ylg.iot.media.bo.RemoteAddressInfo;
import com.ylg.iot.media.bo.SipTransactionInfo;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestParentProcessor;
import com.ylg.iot.media.gb28181.transmit.event.request.SIPRequestProcessor;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import com.ylg.iot.media.gb28181.transmit.SIPSender;
import com.ylg.iot.media.utils.SipUtils;
import com.ylg.iot.media.vo.MediaDeviceVO;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Locale;

/**
 * SIP命令类型： REGISTER请求
 *
 * @author zjh
 * @since 2025-06-20 13:47
 */
@Slf4j
@Component
public class RegisterRequestProcessor extends SIPRequestParentProcessor implements InitializingBean, SIPRequestProcessor {

    public final String method = "REGISTER";

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private SIPProcessorHandler sipProcessorHandler;

    @Autowired
    private MediaDeviceService deviceService;

    @Autowired
    private SIPSender sipSender;

    @Autowired
    private UserSetting userSetting;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 添加消息处理的订阅
        sipProcessorHandler.addRequestProcessor(method, this);
    }

    /**
     * 收到注册请求 处理
     *
     * @param event 注册请求
     */
    @Override
    public void process(RequestEvent event) {
        try {
            SIPRequest request = (SIPRequest) event.getRequest();
            Response response = null;
            boolean passwordCorrect = false;
            // 设备发送的注册请求携带设备有效时间expires，通过不断地发送注册请求来给平台设备续期，expires为0表示注销设备
            if (request.getExpires() == null) {
                response = getMessageFactory().createResponse(Response.BAD_REQUEST, request);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }
            boolean registerFlag = request.getExpires().getExpires() != 0;

            FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);  // <sip:34020000001320000001@192.168.1.100>;tag=12345
            AddressImpl address = (AddressImpl) fromHeader.getAddress();  // <sip:34020000001320000001@192.168.1.100>
            SipUri uri = (SipUri) address.getURI();  // 34020000001320000001@192.168.1.100
            String gbId = uri.getUser();  // 34020000001320000001

            RemoteAddressInfo remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request,
                    userSetting.getUseSipSourceIpAsRemoteAddress());
            String requestAddress = remoteAddressInfo.getIp() + ":" + remoteAddressInfo.getPort();
            String title = registerFlag ? "[注册请求]" : "[注销请求]";
            log.info("{}设备：{}, 开始处理: {}", title, gbId, requestAddress);

            MediaDeviceVO device = deviceService.getByGbId(gbId);

            // 设备续约或注销的情况：设备已存在，同一设备的REGISTER请求的Call-ID是相同的
              if (device != null && device.getSipTransactionInfo() != null &&
                    request.getCallIdHeader().getCallId().equals(device.getSipTransactionInfo().getCallId())) {

                log.info("{}设备：{}, 注册续订: {}", title, device.getGbId(), device.getGbId());
                if (registerFlag) {
                    device.setExpires(request.getExpires().getExpires());
                    device.setIp(remoteAddressInfo.getIp());
                    device.setPort(remoteAddressInfo.getPort());
                    device.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));

                    device.setLocalIp(request.getLocalAddress().getHostAddress()); // 本地ip（内网）
                    device.setRegisterTime(LocalDateTime.now());
                    // 判断TCP还是UDP
                    ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                    String transport = reqViaHeader.getTransport();
                    device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

                    // 构造请求response
                    Response registerOkResponse = getRegisterOkResponse(request);

                    SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse) registerOkResponse);
                    // 设备信息更新
                    deviceService.online(device, sipTransactionInfo);

                    // 发送response
                    sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), registerOkResponse);
                } else {
                    deviceService.offline(gbId, "主动注销");
                }
                return;
            }

            // 首次注册的情况
            String password = (device != null && !ObjectUtils.isEmpty(device.getPassword())) ? device.getPassword() : sipConfig.getPassword();
            AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
            if (authHead == null && !ObjectUtils.isEmpty(password)) {
                log.info("{} 设备：{}, 回复401: {}", title, gbId, requestAddress);
                response = getMessageFactory().createResponse(Response.UNAUTHORIZED, request);
                // 提供认证信息
                new DigestServerAuthenticationHelper().generateChallenge(getHeaderFactory(), response, sipConfig.getDomain());
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            // 无需密码或校验密码正确
            passwordCorrect = ObjectUtils.isEmpty(password) ||
                    new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password);

            if (!passwordCorrect) {
                // 注册失败
                response = getMessageFactory().createResponse(Response.FORBIDDEN, request);
                response.setReasonPhrase("wrong password");
                log.info("{} 设备：{}, 密码/SIP服务器ID错误, 回复403: {}", title, gbId, requestAddress);
                sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);
                return;
            }

            if (device == null) {
                device = new MediaDeviceVO();
                device.setGbId(gbId);
                device.setStreamMode(StreamModeType.TCP_PASSIVE.name());
                device.setCharset("GB2312");
                device.setGeoCoordSys("WGS84");

                device.setOnline(DeviceStatusEnum.OFFLINE.getCode());
            } else {
                if (ObjectUtils.isEmpty(device.getStreamMode())) {
                    device.setStreamMode(StreamModeType.TCP_PASSIVE.name());
                }
                if (ObjectUtils.isEmpty(device.getCharset())) {
                    device.setCharset("GB2312");
                }
                if (ObjectUtils.isEmpty(device.getGeoCoordSys())) {
                    device.setGeoCoordSys("WGS84");
                }
            }

            device.setIp(remoteAddressInfo.getIp());
            device.setPort(remoteAddressInfo.getPort());
            device.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));
            device.setLocalIp(request.getLocalAddress().getHostAddress());

            // 携带授权头并且密码正确
            response = getRegisterOkResponse(request);;
            sipSender.transmitRequest(request.getLocalAddress().getHostAddress(), response);

            if (registerFlag) {
                device.setExpires(request.getExpires().getExpires());
                // 判断TCP还是UDP
                ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                String transport = reqViaHeader.getTransport();
                device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
                device.setRegisterTime(LocalDateTime.now());

                SipTransactionInfo sipTransactionInfo = new SipTransactionInfo((SIPResponse) response);
                deviceService.online(device, sipTransactionInfo);

                log.info("[注册成功] gbId: {}->{}", gbId, requestAddress);
            } else {
                deviceService.offline(gbId, "主动注销");
                log.info("[注销成功] gbId: {}->{}", gbId, requestAddress);
            }
        } catch (SipException | NoSuchAlgorithmException | ParseException e) {
            log.error("未处理的异常 ", e);
        }
    }

    private Response getRegisterOkResponse(Request request) throws ParseException {
        // 携带授权头并且密码正确
        Response response = getMessageFactory().createResponse(Response.OK, request);
        // 添加date头
        SIPDateHeader dateHeader = new SIPDateHeader();
        GbSipDate gbSipDate = new GbSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis());
        dateHeader.setDate(gbSipDate);
        response.addHeader(dateHeader);

        // 添加Contact头
        response.addHeader(request.getHeader(ContactHeader.NAME));
        // 添加Expires头
        response.addHeader(request.getExpires());

        return response;

    }
}
