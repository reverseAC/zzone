package com.zjh.zzone.iot.media.bo;

import com.alibaba.fastjson2.annotation.JSONField;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.zlm.hook.params.HookParam;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.ObjectUtils;

@Getter
@Setter
public class ZLMServerConfig extends HookParam {

    /**
     {
     "api.apiDebug": "1",
     "api.defaultSnap": "./www/logo.png",
     "api.downloadRoot": "./www",
     "api.secret": "MhCXXYpvRP9loGsYgk7YWjZRdB30wzbb",
     "api.snapRoot": "./www/snap/",
     "cluster.origin_url": "",
     "cluster.retry_count": "3",
     "cluster.timeout_sec": "15",
     "ffmpeg.bin": "/usr/bin/ffmpeg",
     "ffmpeg.cmd": "%s -re -i %s -c:a aac -strict -2 -ar 44100 -ab 48k -c:v libx264 -f flv %s",
     "ffmpeg.log": "./ffmpeg/ffmpeg.log",
     "ffmpeg.restart_sec": "0",
     "ffmpeg.snap": "%s -rtsp_transport tcp -i %s -y -f mjpeg -frames:v 1 %s",
     "general.broadcast_player_count_changed": "0",
     "general.check_nvidia_dev": "1",
     "general.enableVhost": "0",
     "general.enable_ffmpeg_log": "0",
     "general.flowThreshold": "1024",
     "general.listen_ip": "::",
     "general.maxStreamWaitMS": "15000",
     "general.mediaServerId": "your_server_id",
     "general.mergeWriteMS": "0",
     "general.resetWhenRePlay": "1",
     "general.streamNoneReaderDelayMS": "20000",
     "general.unready_frame_cache": "100",
     "general.wait_add_track_ms": "3000",
     "general.wait_audio_track_data_ms": "1000",
     "general.wait_track_ready_ms": "10000",
     "hls.broadcastRecordTs": "0",
     "hls.deleteDelaySec": "10",
     "hls.fastRegister": "0",
     "hls.fileBufSize": "65536",
     "hls.segDelay": "0",
     "hls.segDur": "2",
     "hls.segKeep": "0",
     "hls.segNum": "3",
     "hls.segRetain": "5",
     "hook.alive_interval": "10",
     "hook.enable": "1",
     "hook.on_flow_report": "",
     "hook.on_http_access": "",
     "hook.on_play": "http://192.168.2.53:8582/index/hook/on_play",
     "hook.on_publish": "http://192.168.2.53:8582/index/hook/on_publish",
     "hook.on_record_mp4": "http://192.168.2.53:8582/index/hook/on_record_mp4",
     "hook.on_record_ts": "",
     "hook.on_rtp_server_timeout": "http://192.168.2.53:8582/index/hook/on_rtp_server_timeout",
     "hook.on_rtsp_auth": "",
     "hook.on_rtsp_realm": "",
     "hook.on_send_rtp_stopped": "http://192.168.2.53:8582/index/hook/on_send_rtp_stopped",
     "hook.on_server_exited": "",
     "hook.on_server_keepalive": "http://192.168.2.53:8582/index/hook/on_server_keepalive",
     "hook.on_server_started": "http://192.168.2.53:8582/index/hook/on_server_started",
     "hook.on_shell_login": "",
     "hook.on_stream_changed": "http://192.168.2.53:8582/index/hook/on_stream_changed",
     "hook.on_stream_none_reader": "http://192.168.2.53:8582/index/hook/on_stream_none_reader",
     "hook.on_stream_not_found": "http://192.168.2.53:8582/index/hook/on_stream_not_found",
     "hook.retry": "1",
     "hook.retry_delay": "3.0",
     "hook.stream_changed_schemas": "rtsp/rtmp/fmp4/ts/hls/hls.fmp4",
     "hook.timeoutSec": "30",
     "http.allow_cross_domains": "1",
     "http.allow_ip_range": "::1,127.0.0.1,172.16.0.0-172.31.255.255,192.168.0.0-192.168.255.255,10.0.0.0-10.255.255.255",
     "http.charSet": "utf-8",
     "http.dirMenu": "1",
     "http.forbidCacheSuffix": "",
     "http.forwarded_ip_header": "",
     "http.keepAliveSecond": "30",
     "http.maxReqSize": "40960",
     "http.notFound": "<html><head><title>404 Not Found</title></head><body bgcolor=\"white\"><center><h1>您访问的资源不存在！</h1></center><hr><center>ZLMediaKit(git hash:beff8c0/2025-04-15T09:48:00+08:00,branch:master,build time:2025-04-15T01:49:11)</center></body></html>",
     "http.port": "80",
     "http.rootPath": "./www",
     "http.sendBufSize": "65536",
     "http.sslport": "443",
     "http.virtualPath": "",
     "multicast.addrMax": "239.255.255.255",
     "multicast.addrMin": "239.0.0.0",
     "multicast.udpTTL": "64",
     "protocol.add_mute_audio": "1",
     "protocol.auto_close": "0",
     "protocol.continue_push_ms": "3000",
     "protocol.enable_audio": "1",
     "protocol.enable_fmp4": "1",
     "protocol.enable_hls": "1",
     "protocol.enable_hls_fmp4": "0",
     "protocol.enable_mp4": "0",
     "protocol.enable_rtmp": "1",
     "protocol.enable_rtsp": "1",
     "protocol.enable_ts": "1",
     "protocol.fmp4_demand": "0",
     "protocol.hls_demand": "0",
     "protocol.hls_save_path": "./www",
     "protocol.modify_stamp": "2",
     "protocol.mp4_as_player": "0",
     "protocol.mp4_max_second": "3600",
     "protocol.mp4_save_path": "./www",
     "protocol.paced_sender_ms": "0",
     "protocol.rtmp_demand": "0",
     "protocol.rtsp_demand": "0",
     "protocol.ts_demand": "0",
     "record.appName": "record",
     "record.enableFmp4": "0",
     "record.fastStart": "0",
     "record.fileBufSize": "65536",
     "record.fileRepeat": "0",
     "record.sampleMS": "500",
     "rtc.bfilter": "0",
     "rtc.datachannel_echo": "1",
     "rtc.externIP": "",
     "rtc.maxRtpCacheMS": "5000",
     "rtc.maxRtpCacheSize": "2048",
     "rtc.max_bitrate": "0",
     "rtc.min_bitrate": "0",
     "rtc.nackIntervalRatio": "1.0",
     "rtc.nackMaxCount": "15",
     "rtc.nackMaxMS": "3000",
     "rtc.nackMaxSize": "2048",
     "rtc.nackRtpSize": "8",
     "rtc.port": "8000",
     "rtc.preferredCodecA": "PCMA,PCMU,opus,mpeg4-generic",
     "rtc.preferredCodecV": "H264,H265,AV1,VP9,VP8",
     "rtc.rembBitRate": "0",
     "rtc.start_bitrate": "0",
     "rtc.tcpPort": "8000",
     "rtc.timeoutSec": "15",
     "rtmp.directProxy": "1",
     "rtmp.enhanced": "0",
     "rtmp.handshakeSecond": "15",
     "rtmp.keepAliveSecond": "15",
     "rtmp.port": "1935",
     "rtmp.sslport": "0",
     "rtp.audioMtuSize": "600",
     "rtp.h264_stap_a": "1",
     "rtp.lowLatency": "0",
     "rtp.rtpMaxSize": "10",
     "rtp.videoMtuSize": "1400",
     "rtp_proxy.dumpDir": "",
     "rtp_proxy.gop_cache": "1",
     "rtp_proxy.h264_pt": "98",
     "rtp_proxy.h265_pt": "99",
     "rtp_proxy.opus_pt": "100",
     "rtp_proxy.port": "10000",
     "rtp_proxy.port_range": "44800-44999",
     "rtp_proxy.ps_pt": "96",
     "rtp_proxy.rtp_g711_dur_ms": "100",
     "rtp_proxy.timeoutSec": "15",
     "rtp_proxy.udp_recv_socket_buffer": "4194304",
     "rtsp.authBasic": "0",
     "rtsp.directProxy": "1",
     "rtsp.handshakeSecond": "15",
     "rtsp.keepAliveSecond": "15",
     "rtsp.lowLatency": "0",
     "rtsp.port": "554",
     "rtsp.rtpTransportType": "-1",
     "rtsp.sslport": "0",
     "shell.maxReqSize": "1024",
     "shell.port": "0",
     "srt.latencyMul": "4",
     "srt.passPhrase": "",
     "srt.pktBufSize": "8192",
     "srt.port": "9000",
     "srt.timeoutSec": "5"
     }
     */
    @JSONField(name = "api.apiDebug")
    private String apiDebug;

    @JSONField(name = "api.secret")
    private String apiSecret;

    @JSONField(name = "api.snapRoot")
    private String apiSnapRoot;

    @JSONField(name = "api.defaultSnap")
    private String apiDefaultSnap;

    @JSONField(name = "ffmpeg.bin")
    private String ffmpegBin;

    @JSONField(name = "ffmpeg.cmd")
    private String ffmpegCmd;

    @JSONField(name = "ffmpeg.snap")
    private String ffmpegSnap;

    @JSONField(name = "ffmpeg.log")
    private String ffmpegLog;

    @JSONField(name = "ffmpeg.restart_sec")
    private String ffmpegRestartSec;

    @JSONField(name = "protocol.modify_stamp")
    private String protocolModifyStamp;

    @JSONField(name = "protocol.enable_audio")
    private String protocolEnableAudio;

    @JSONField(name = "protocol.add_mute_audio")
    private String protocolAddMuteAudio;

    @JSONField(name = "protocol.continue_push_ms")
    private String protocolContinuePushMs;

    @JSONField(name = "protocol.enable_hls")
    private String protocolEnableHls;

    @JSONField(name = "protocol.enable_mp4")
    private String protocolEnableMp4;

    @JSONField(name = "protocol.enable_rtsp")
    private String protocolEnableRtsp;

    @JSONField(name = "protocol.enable_rtmp")
    private String protocolEnableRtmp;

    @JSONField(name = "protocol.enable_ts")
    private String protocolEnableTs;

    @JSONField(name = "protocol.enable_fmp4")
    private String protocolEnableFmp4;

    @JSONField(name = "protocol.mp4_as_player")
    private String protocolMp4AsPlayer;

    @JSONField(name = "protocol.mp4_max_second")
    private String protocolMp4MaxSecond;

    @JSONField(name = "protocol.mp4_save_path")
    private String protocolMp4SavePath;

    @JSONField(name = "protocol.hls_save_path")
    private String protocolHlsSavePath;

    @JSONField(name = "protocol.hls_demand")
    private String protocolHlsDemand;

    @JSONField(name = "protocol.rtsp_demand")
    private String protocolRtspDemand;

    @JSONField(name = "protocol.rtmp_demand")
    private String protocolRtmpDemand;

    @JSONField(name = "protocol.ts_demand")
    private String protocolTsDemand;

    @JSONField(name = "protocol.fmp4_demand")
    private String protocolFmp4Demand;

    @JSONField(name = "general.enableVhost")
    private String generalEnableVhost;

    @JSONField(name = "general.flowThreshold")
    private String generalFlowThreshold;

    @JSONField(name = "general.maxStreamWaitMS")
    private String generalMaxStreamWaitMS;

    @JSONField(name = "general.streamNoneReaderDelayMS")
    private int generalStreamNoneReaderDelayMS;

    @JSONField(name = "general.resetWhenRePlay")
    private String generalResetWhenRePlay;

    @JSONField(name = "general.mergeWriteMS")
    private String generalMergeWriteMS;

    @JSONField(name = "general.mediaServerId")
    private String generalMediaServerId;

    @JSONField(name = "general.wait_track_ready_ms")
    private String generalWaitTrackReadyMs;

    @JSONField(name = "general.wait_add_track_ms")
    private String generalWaitAddTrackMs;

    @JSONField(name = "general.unready_frame_cache")
    private String generalUnreadyFrameCache;


    @JSONField(name = "ip")
    private String ip;

    private String sdpIp;

    private String streamIp;

    private String hookIp;

    private String updateTime;

    private String createTime;

    @JSONField(name = "hls.fileBufSize")
    private String hlsFileBufSize;

    @JSONField(name = "hls.filePath")
    private String hlsFilePath;

    @JSONField(name = "hls.segDur")
    private String hlsSegDur;

    @JSONField(name = "hls.segNum")
    private String hlsSegNum;

    @JSONField(name = "hls.segRetain")
    private String hlsSegRetain;

    @JSONField(name = "hls.broadcastRecordTs")
    private String hlsBroadcastRecordTs;

    @JSONField(name = "hls.deleteDelaySec")
    private String hlsDeleteDelaySec;

    @JSONField(name = "hls.segKeep")
    private String hlsSegKeep;

    @JSONField(name = "hook.access_file_except_hls")
    private String hookAccessFileExceptHLS;

    @JSONField(name = "hook.admin_params")
    private String hookAdminParams;

    @JSONField(name = "hook.alive_interval")
    private Integer hookAliveInterval;

    @JSONField(name = "hook.enable")
    private String hookEnable;

    @JSONField(name = "hook.on_flow_report")
    private String hookOnFlowReport;

    @JSONField(name = "hook.on_http_access")
    private String hookOnHttpAccess;

    @JSONField(name = "hook.on_play")
    private String hookOnPlay;

    @JSONField(name = "hook.on_publish")
    private String hookOnPublish;

    @JSONField(name = "hook.on_record_mp4")
    private String hookOnRecordMp4;

    @JSONField(name = "hook.on_rtsp_auth")
    private String hookOnRtspAuth;

    @JSONField(name = "hook.on_rtsp_realm")
    private String hookOnRtspRealm;

    @JSONField(name = "hook.on_shell_login")
    private String hookOnShellLogin;

    @JSONField(name = "hook.on_stream_changed")
    private String hookOnStreamChanged;

    @JSONField(name = "hook.on_stream_none_reader")
    private String hookOnStreamNoneReader;

    @JSONField(name = "hook.on_stream_not_found")
    private String hookOnStreamNotFound;

    @JSONField(name = "hook.on_server_started")
    private String hookOnServerStarted;

    @JSONField(name = "hook.on_server_keepalive")
    private String hookOnServerKeepalive;

    @JSONField(name = "hook.on_send_rtp_stopped")
    private String hookOnSendRtpStopped;

    @JSONField(name = "hook.on_rtp_server_timeout")
    private String hookOnRtpServerTimeout;

    @JSONField(name = "hook.timeoutSec")
    private String hookTimeoutSec;

    @JSONField(name = "http.charSet")
    private String httpCharSet;

    @JSONField(name = "http.keepAliveSecond")
    private String httpKeepAliveSecond;

    @JSONField(name = "http.maxReqCount")
    private String httpMaxReqCount;

    @JSONField(name = "http.maxReqSize")
    private String httpMaxReqSize;

    @JSONField(name = "http.notFound")
    private String httpNotFound;

    @JSONField(name = "http.port")
    private int httpPort;

    @JSONField(name = "http.rootPath")
    private String httpRootPath;

    @JSONField(name = "http.sendBufSize")
    private String httpSendBufSize;

    @JSONField(name = "http.sslport")
    private int httpSSLport;

    @JSONField(name = "multicast.addrMax")
    private String multicastAddrMax;

    @JSONField(name = "multicast.addrMin")
    private String multicastAddrMin;

    @JSONField(name = "multicast.udpTTL")
    private String multicastUdpTTL;

    @JSONField(name = "record.appName")
    private String recordAppName;

    @JSONField(name = "record.filePath")
    private String recordFilePath;

    @JSONField(name = "record.fileSecond")
    private String recordFileSecond;

    @JSONField(name = "record.sampleMS")
    private String recordFileSampleMS;

    @JSONField(name = "rtmp.handshakeSecond")
    private String rtmpHandshakeSecond;

    @JSONField(name = "rtmp.keepAliveSecond")
    private String rtmpKeepAliveSecond;

    @JSONField(name = "rtmp.modifyStamp")
    private String rtmpModifyStamp;

    @JSONField(name = "rtmp.port")
    private int rtmpPort;

    @JSONField(name = "rtmp.sslport")
    private int rtmpSslPort;

    @JSONField(name = "rtp.audioMtuSize")
    private String rtpAudioMtuSize;

    @JSONField(name = "rtp.clearCount")
    private String rtpClearCount;

    @JSONField(name = "rtp.cycleMS")
    private String rtpCycleMS;

    @JSONField(name = "rtp.maxRtpCount")
    private String rtpMaxRtpCount;

    @JSONField(name = "rtp.videoMtuSize")
    private String rtpVideoMtuSize;

    @JSONField(name = "rtp_proxy.checkSource")
    private String rtpProxyCheckSource;

    @JSONField(name = "rtp_proxy.dumpDir")
    private String rtpProxyDumpDir;

    @JSONField(name = "rtp_proxy.port")
    private int rtpProxyPort;

    @JSONField(name = "rtp_proxy.port_range")
    private String portRange;

    @JSONField(name = "rtp_proxy.timeoutSec")
    private String rtpProxyTimeoutSec;

    @JSONField(name = "rtsp.authBasic")
    private String rtspAuthBasic;

    @JSONField(name = "rtsp.handshakeSecond")
    private String rtspHandshakeSecond;

    @JSONField(name = "rtsp.keepAliveSecond")
    private String rtspKeepAliveSecond;

    @JSONField(name = "rtsp.port")
    private int rtspPort;

    @JSONField(name = "rtsp.sslport")
    private int rtspSSlport;

    @JSONField(name = "shell.maxReqSize")
    private String shellMaxReqSize;

    @JSONField(name = "shell.shell")
    private String shellPhell;

    @JSONField(name = "transcode.suffix")
    private String transcodeSuffix;

    public MediaServer getMediaServer(String sipIp) {
        MediaServer mediaServer = new MediaServer();
        mediaServer.setServerId(getGeneralMediaServerId());
        mediaServer.setIp(getIp());
        mediaServer.setHookIp(ObjectUtils.isEmpty(getHookIp()) ? sipIp: getHookIp());
        mediaServer.setSdpIp(ObjectUtils.isEmpty(getSdpIp()) ? getIp(): getSdpIp());
        mediaServer.setStreamIp(ObjectUtils.isEmpty(getStreamIp()) ? getIp(): getStreamIp());
        mediaServer.setHttpPort(getHttpPort());
        mediaServer.setFlvPort(getHttpPort());
        mediaServer.setWsFlvPort(getHttpPort());
        mediaServer.setHttpSSlPort(getHttpSSLport());
        mediaServer.setFlvSSLPort(getHttpSSLport());
        mediaServer.setWsFlvSSLPort(getHttpSSLport());
        mediaServer.setRtmpPort(getRtmpPort());
        mediaServer.setRtmpSSlPort(getRtmpSslPort());
        mediaServer.setRtpProxyPort(getRtpProxyPort());
        mediaServer.setRtspPort(getRtspPort());
        mediaServer.setRtspSSLPort(getRtspSSlport());
        mediaServer.setAutoConfig(true); // 默认值true;
        mediaServer.setSecret(getApiSecret());
        mediaServer.setHookAliveInterval(getHookAliveInterval());
        mediaServer.setRtpEnable(false); // 默认使用单端口;直到用户自己设置开启多端口
        mediaServer.setRtpPortRange(getPortRange().replace("_",",")); // 默认使用30000,30500作为级联时发送流的端口号
        mediaServer.setRecordAssistPort(0); // 默认关闭
        mediaServer.setTranscodeSuffix(getTranscodeSuffix());
        return mediaServer;
    }
}
