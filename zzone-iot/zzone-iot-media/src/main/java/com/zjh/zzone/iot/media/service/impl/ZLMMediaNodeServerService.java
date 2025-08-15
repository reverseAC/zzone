package com.zjh.zzone.iot.media.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ylg.core.exception.CheckedException;
import com.ylg.iot.vo.MediaInfo;
import com.ylg.iot.media.bo.SendRtpInfo;
import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.service.MediaNodeServerService;
import com.ylg.iot.media.zlm.ZLMRESTFulUtils;
import com.ylg.iot.media.zlm.ZLMServerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流媒体服务器 服务实现类
 *
 * @author zjh
 * @since 2025-04-01 19:24
 */
@Service("zlm")
@Slf4j
public class ZLMMediaNodeServerService implements MediaNodeServerService {

    @Autowired
    private ZLMRESTFulUtils zlmRestFulUtils;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ServerInstanceConfig serverInstanceConfig;

    @Override
    public int createRTPServer(MediaServer mediaServer, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        return zlmServerFactory.createRTPServer(mediaServer, streamId, ssrc, port, onlyAuto, reUsePort, tcpMode);
    }

    @Override
    public void closeRtpServer(MediaServer mediaServer, String streamId) {
        zlmServerFactory.closeRtpServer(mediaServer, streamId);
    }

    @Override
    public void closeStreams(MediaServer mediaServer, String app, String stream) {
        zlmRestFulUtils.closeStreams(mediaServer, app, stream);
    }

    @Override
    public void online(MediaServer mediaServer) {

    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServer, String streamId, String ssrc) {
        return zlmServerFactory.updateRtpServerSSRC(mediaServer, streamId, ssrc);
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream) {
        JSONObject jsonObject = zlmRestFulUtils.connectRtpServer(mediaServer, address, port, stream);
        log.info("[TCP主动连接对方] 结果： {}", jsonObject);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public void getSnap(MediaServer mediaServer, String streamUrl, int timeoutSec, int expireSec, String path, String fileName) {
        zlmRestFulUtils.getSnap(mediaServer, streamUrl, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        JSONObject jsonObject = zlmRestFulUtils.getMediaInfo(mediaServer, app, "rtsp", stream);
        if (jsonObject.getInteger("code") != 0) {
            return null;
        }
        return MediaInfo.getInstance(jsonObject, mediaServer, serverInstanceConfig.getInstanceId());
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmRestFulUtils.pauseRtpCheck(mediaServer, streamKey);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmRestFulUtils.resumeRtpCheck(mediaServer, streamKey);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        if (!ObjectUtils.isEmpty(ssrc)) {
            param.put("ssrc", ssrc);
        }
        JSONObject jsonObject = zlmRestFulUtils.stopSendRtp(mediaInfo, param);
        if (jsonObject.getInteger("code") != null && jsonObject.getInteger("code") == 0) {
            log.info("[停止发流] 成功: 参数：{}", JSON.toJSONString(param));
            return true;
        } else {
            log.info("停止发流结果: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            return false;
        }
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        log.info("[zlm-deleteRecordDirectory] 删除磁盘文件, server: {} {}:{}->{}/{}", mediaServer.getId(), app, stream, date, fileName);
        JSONObject jsonObject = zlmRestFulUtils.deleteRecordDirectory(mediaServer, app,
                stream, date, fileName);
        if (jsonObject.getInteger("code") == 0) {
            return true;
        } else {
            log.info("[zlm-deleteRecordDirectory] 删除磁盘文件错误, server: {} {}:{}->{}/{}, 结果： {}", mediaServer.getId(), app, stream, date, fileName, jsonObject);
            throw new RuntimeException("删除磁盘文件失败");
        }
    }


    @Override
    public Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", sendRtpItem.isTcp() ? "0" : "1");
        param.put("recv_stream_id", sendRtpItem.getReceiveStream());
        if (timeout  != null) {
            param.put("close_delay_ms", timeout);
        }
        if (!sendRtpItem.isTcp()) {
            // 开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
        }
        if (!sendRtpItem.isTcpActive()) {
            param.put("dst_url",sendRtpItem.getIp());
            param.put("dst_port", sendRtpItem.getPort());
        }

        JSONObject jsonObject = zlmServerFactory.startSendRtpPassive(mediaServer, param, null);
        if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
            log.error("启动监听TCP被动推流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            throw new RuntimeException(jsonObject.getInteger("code") + jsonObject.getString("msg"));
        }
        log.info("调用ZLM-TCP被动推流接口, 结果： {}",  jsonObject);
        log.info("启动监听TCP被动推流成功[ {}/{} ]，{}->{}:{}, " , sendRtpItem.getApp(), sendRtpItem.getStream(),
                jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
        return jsonObject.getInteger("local_port");
    }


    @Override
    public void startSendRtpStream(MediaServer mediaServer, SendRtpInfo sendRtpItem) {
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", sendRtpItem.isTcp() ? "0" : "1");
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "500" : "0");
        }
        param.put("dst_url", sendRtpItem.getIp());
        param.put("dst_port", sendRtpItem.getPort());
        JSONObject jsonObject = zlmRestFulUtils.startSendRtp(mediaServer, param);
        if (jsonObject == null ) {
            throw new RuntimeException("连接zlm失败");
        }else if (jsonObject.getInteger("code") != 0) {
            throw new RuntimeException(jsonObject.getInteger("code") + jsonObject.getString("msg"));
        }
        log.info("[推流结果]：{} ，参数： {}",jsonObject, JSONObject.toJSONString(param));
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        MediaInfo mediaInfo = getMediaInfo(mediaServer, app, stream);
        if (mediaInfo == null) {
            log.warn("[获取下载进度] 查询进度失败, 节点Id： {}， {}/{}", mediaServer.getId(), app, stream);
            return null;
        }
        return mediaInfo.getDuration();
    }

    @Override
    public void loadMP4File(MediaServer mediaServer, String app, String stream, String datePath) {
        JSONObject jsonObject =  zlmRestFulUtils.loadMP4File(mediaServer, app, stream, datePath);
        if (jsonObject == null) {
            throw new CheckedException("请求失败");
        }
        if (jsonObject.getInteger("code") != 0) {
            throw new RuntimeException(jsonObject.getInteger("code") + jsonObject.getString("msg"));
        }
    }


    @Override
    public List<String> listRtpServer(MediaServer mediaServer) {
        JSONObject jsonObject = zlmRestFulUtils.listRtpServer(mediaServer);
        List<String> result = new ArrayList<>();
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            return result;
        }
        JSONArray data = jsonObject.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (int i = 0; i < data.size(); i++) {
            JSONObject dataJSONObject = data.getJSONObject(i);
            result.add(dataJSONObject.getString("stream_id"));
        }
        return result;
    }
}
