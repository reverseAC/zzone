package com.zjh.zzone.iot.media.gb28181;

import com.ylg.iot.media.config.DefaultSipStackProperties;
import com.ylg.iot.media.config.SipConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.bo.GbStringMsgParserFactory;
import com.ylg.iot.media.gb28181.transmit.SIPProcessorHandler;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIP组件管理
 *
 * @author zjh
 * @since 2025-03-24 14:34
 */
@Slf4j
@Component
@Order(value=10)
public class SipLayer implements CommandLineRunner {

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SIPProcessorHandler sipProcessorObserver;

    private final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
    private final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();
    private final List<String> monitorIps = new ArrayList<>();

    /**
     * 设置sip服务监听
     * @param args
     */
    @Override
    public void run(String... args) {
        if (ObjectUtils.isEmpty(sipConfig.getIp())) {
            try {
                // 获得本机的所有网络接口
                Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
                while (nifs.hasMoreElements()) {
                    NetworkInterface nif = nifs.nextElement();
                    // 获得与该网络接口绑定的 IP 地址，一般只有一个
                    Enumeration<InetAddress> addresses = nif.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        log.info(addr.getHostAddress());
                        if (addr instanceof Inet4Address) {
                            if (addr.getHostAddress().equals("127.0.0.1")){
                                continue;
                            }
                            if (nif.getName().startsWith("docker")) {
                                continue;
                            }
                            log.info("[自动配置SIP监听网卡] 网卡接口地址： {}", addr.getHostAddress());// 只关心 IPv4 地址
                            monitorIps.add(addr.getHostAddress());
                        }
                    }
                }
            }catch (Exception e) {
                log.error("[读取网卡信息失败]", e);
            }
            if (monitorIps.isEmpty()) {
                log.error("[自动配置SIP监听网卡信息失败]， 请手动配置SIP.IP后重新启动");
                System.exit(1);
            }
        } else {
            // 使用逗号分割多个ip
            String separator = ",";
            if (sipConfig.getIp().indexOf(separator) > 0) {
                String[] split = sipConfig.getIp().split(separator);
                monitorIps.addAll(Arrays.asList(split));
            } else {
                monitorIps.add(sipConfig.getIp());
            }
        }
        if (ObjectUtils.isEmpty(sipConfig.getShowIp())){
            sipConfig.setShowIp(String.join(",", monitorIps));
        }
        // 指定SIP协议栈的实现类所在的包路径，当前使用的JAIN-SIP，建议设置。
        SipFactory.getInstance().setPathName("gov.nist");
        if (!monitorIps.isEmpty()) {
            for (String monitorIp : monitorIps) {
                // 设置监听
                addListeningPoint(monitorIp, sipConfig.getPort());
            }
            if (udpSipProviderMap.size() + tcpSipProviderMap.size() == 0) {
                System.exit(1);
            }
        }
    }

    private void addListeningPoint(String monitorIp, int port){
        SipStackImpl sipStack;
        try {
            sipStack = (SipStackImpl)SipFactory.getInstance().createSipStack(DefaultSipStackProperties.getProperties("GB28181_SIP", userSetting.getSipLog(), userSetting.isSipCacheServerConnections()));
            sipStack.setMessageParserFactory(new GbStringMsgParserFactory());
        } catch (PeerUnavailableException e) {
            log.error("[SIP SERVER] SIP服务启动失败， 监听地址{}失败,请检查ip是否正确", monitorIp);
            return;
        }

        // TCP端口监听
        try {
            ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "TCP");
            SipProviderImpl tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);

            tcpSipProvider.setDialogErrorsAutomaticallyHandled();
            tcpSipProvider.addSipListener(sipProcessorObserver);
            tcpSipProviderMap.put(monitorIp, tcpSipProvider);
            log.info("[SIP SERVER] tcp://{}:{} 启动成功", monitorIp, port);
        } catch (TransportNotSupportedException
                 | TooManyListenersException
                 | ObjectInUseException
                 | InvalidArgumentException e) {
            log.error("[SIP SERVER] tcp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
                    , monitorIp, port);
            log.error("消息解析异常", e);
        }

        // UDP端口监听
        try {
            ListeningPoint udpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "UDP");

            SipProviderImpl udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
            udpSipProvider.addSipListener(sipProcessorObserver);
            udpSipProvider.setDialogErrorsAutomaticallyHandled();
            udpSipProviderMap.put(monitorIp, udpSipProvider);

            log.info("[SIP SERVER] udp://{}:{} 启动成功", monitorIp, port);
        } catch (TransportNotSupportedException
                 | TooManyListenersException
                 | ObjectInUseException
                 | InvalidArgumentException e) {
            log.error("[SIP SERVER] udp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
                    , monitorIp, port);
            log.error("消息解析异常", e);
        }
    }

    public SipProviderImpl getUdpSipProvider(String ip) {
        if (udpSipProviderMap.size() == 1) {
            return udpSipProviderMap.values().stream().findFirst().get();
        }
        if (ObjectUtils.isEmpty(ip)) {
            return null;
        }
        return udpSipProviderMap.get(ip);
    }

    public SipProviderImpl getUdpSipProvider() {
        if (udpSipProviderMap.size() != 1) {
            return null;
        }
        return udpSipProviderMap.values().stream().findFirst().get();
    }

    public SipProviderImpl getTcpSipProvider(String ip) {
        if (tcpSipProviderMap.size() == 1) {
            return tcpSipProviderMap.values().stream().findFirst().get();
        }
        if (ObjectUtils.isEmpty(ip)) {
            return null;
        }
        return tcpSipProviderMap.get(ip);
    }

    public SipProviderImpl getTcpSipProvider() {
        if (tcpSipProviderMap.size() != 1) {
            return null;
        }
        return tcpSipProviderMap.values().stream().findFirst().get();
    }

    public String getLocalIp(String deviceLocalIp) {
        if (monitorIps.size() == 1) {
            return monitorIps.get(0);
        }
        if (!ObjectUtils.isEmpty(deviceLocalIp)) {
            return deviceLocalIp;
        }
        return getUdpSipProvider().getListeningPoint().getIPAddress();
    }
}
