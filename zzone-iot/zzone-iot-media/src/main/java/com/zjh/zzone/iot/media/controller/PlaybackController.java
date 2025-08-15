package com.zjh.zzone.iot.media.controller;

import com.ylg.core.base.BaseController;
import com.ylg.core.domain.R;
import com.ylg.iot.media.bo.InviteInfo;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.contant.InviteResultCode;
import com.ylg.iot.media.gb28181.transmit.callback.DeferredResultHolder;
import com.ylg.iot.media.gb28181.transmit.callback.RequestMessage;
import com.ylg.iot.media.gb28181.transmit.cmd.SIPCommander;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.service.PlayService;
import com.ylg.iot.vo.StreamContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.UUID;

/**
 * 国标设备点播
 *
 * @author zjh
 * @since 2025-03-25 20:06
 */
@RestController
@RequestMapping("/playback")
@Slf4j
public class PlaybackController extends BaseController {

    @Resource
    private PlayService playService;

    @Resource
    private MediaDeviceService deviceService;

    @Resource
    private MediaDeviceChannelService channelService;

    @Resource
    private InviteStreamService inviteStreamService;

    @Resource
    private UserSetting userSetting;

    @Resource
    private DeferredResultHolder resultHolder;

    @Autowired
    private SIPCommander cmder;

    /**
     * 开始视频回放
     *
     * @param request http请求对象
     * @param deviceGbId 设备国标编号
     * @param channelGbId 通道国标编号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 视频信息
     */
    @GetMapping("/start/{deviceGbId}/{channelGbId}")
    public DeferredResult<R<StreamContent>> start(HttpServletRequest request, @PathVariable String deviceGbId, @PathVariable String channelGbId,
                                                  String startTime, String endTime) {

        if (log.isDebugEnabled()) {
            log.debug(String.format("设备回放 API调用，deviceGbId：%s ，channelGbId：%s", deviceGbId, channelGbId));
        }

        this.deviceService.checkDeviceEnable(deviceGbId);

        String uuid = UUID.randomUUID().toString();
        String key = DeferredResultHolder.CALLBACK_CMD_PLAYBACK + deviceGbId + channelGbId;
        DeferredResult<R<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());
        resultHolder.put(key, uuid, result);


        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setKey(key);
        requestMessage.setId(uuid);
        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在：" + deviceGbId);

        MediaDeviceChannel channel = channelService.getChannelByGbId(device.getId(), channelGbId);
        Assert.notNull(device, "通道不存在：" + channelGbId);

        playService.playBack(device, channel, startTime, endTime,
                (code, msg, data)->{

                    R<StreamContent> resultContent = null;
                    if (code == InviteResultCode.SUCCESS.getCode()) {
                        resultContent = R.ok();

                        if (data != null) {
                            StreamDetail streamInfo = data;
                            if (userSetting.getUseSourceIpAsStreamIp()) {
                                streamInfo = streamInfo.clone();//深拷贝
                                String host;
                                try {
                                    URL url=new URL(request.getRequestURL().toString());
                                    host=url.getHost();
                                } catch (MalformedURLException e) {
                                    host=request.getLocalAddr();
                                }
                                streamInfo.changeStreamIp(host);
                            }
                            resultContent = R.ok(streamInfo.getStreamContent());
                        }
                    } else {
                        resultContent = R.fail(msg);
                    }
                    requestMessage.setData(resultContent);
                    resultHolder.invokeResult(requestMessage);
                });

        return result;
    }

    /**
     * 停止视频回放
     *
     * @param deviceGbId  设备国标编号
     * @param channelGbId 通道国标编号
     * @param stream      流ID
     */
    @GetMapping("/stop/{deviceGbId}/{channelGbId}/{stream}")
    public R<?> playStop(@PathVariable String deviceGbId, @PathVariable String channelGbId, @PathVariable String stream) {
        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在：" + deviceGbId);

        MediaDeviceChannel deviceChannel = channelService.getChannelByGbId(device.getId(), channelGbId);
        Assert.notNull(deviceChannel, "通道不存在：" + deviceGbId);

        playService.stop(InviteSessionType.PLAYBACK, device, deviceChannel, stream);
        return success("成功");
    }

    /**
     * 回放暂停
     *
     * @param streamId 回放流ID
     */
    @GetMapping("/pause/{streamId}")
    public R<Void> playPause(@PathVariable String streamId) {
        try {
            playService.pauseRtp(streamId);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            return R.fail(e.getMessage());
        }

        return R.ok();
    }

    /**
     * 回放恢复
     *
     * @param streamId 回放流ID
     */
    @GetMapping("/resume/{streamId}")
    public R<Void> playResume(@PathVariable String streamId) {
        try {
            playService.resumeRtp(streamId);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 回放拖动播放
     *
     * @param streamId 回放流ID
     * @param seekTime 拖动偏移量，单位s
     */
    @GetMapping("/seek/{streamId}/{seekTime}")
    public R<Void> playSeek(@PathVariable String streamId, @PathVariable long seekTime) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

        if (null == inviteInfo || inviteInfo.getStreamDetail() == null) {
            log.warn("获取不到InviteInfo：{}-{}-{}", InviteSessionType.PLAYBACK.name(), "seek", streamId);
            return R.fail("会话已过期，请刷新后重试");
        }
        MediaDevice device = deviceService.getByGbId(inviteInfo.getDeviceId());
        MediaDeviceChannel channel = channelService.getById(inviteInfo.getChannelId());
        try {
            cmder.playSeekCmd(device, channel, inviteInfo.getStreamDetail(), seekTime);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 回放倍速播放
     *
     * @param streamId 回放流ID
     * @param speed 倍速0.25 0.5 1、2、4、8
     */
    @GetMapping("/speed/{streamId}/{speed}")
    public R<Void> playSpeed(@PathVariable String streamId, @PathVariable Double speed) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(InviteSessionType.PLAYBACK, streamId);

        if (null == inviteInfo || inviteInfo.getStreamDetail() == null) {
            log.warn("获取不到InviteInfo：{}-{}-{}", InviteSessionType.PLAYBACK.name(), "speed", streamId);
            return R.fail("会话已过期，请刷新后重试");
        }
        MediaDevice device = deviceService.getByGbId(inviteInfo.getDeviceId());
        MediaDeviceChannel channel = channelService.getById(inviteInfo.getChannelId());
        try {
            cmder.playSpeedCmd(device, channel, inviteInfo.getStreamDetail(), speed);
        } catch (InvalidArgumentException | ParseException | SipException e) {
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 录像回放
     *
     * @param deviceId  设备id
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成功或失败提示
     */
    @GetMapping("/playback/{deviceId}")
    public R<StreamContent> playBack(@PathVariable Long deviceId, @RequestParam String startTime, @RequestParam String endTime) {
        return null;
    }

    /**
     * 录像回放暂停
     *
     * @param streamId 流id
     * @return 成功或失败提示
     */
    @GetMapping("/playback/pause/{streamId}")
    public R<Void> playbackPause(@PathVariable String streamId) {
        return null;
    }

    /**
     * 录像回放恢复
     *
     * @param streamId 流id
     * @return 成功或失败提示
     */
    @GetMapping("/playback/resume/{streamId}")
    public R<Void> playbackResume(@PathVariable String streamId) {
        return null;
    }

    /**
     * 录像回放停止
     *
     * @param streamId 流id
     * @return 成功或失败提示
     */
    @GetMapping("/playback/stop/{deviceId}/{streamId}")
    public R<Void> playbackStop(@PathVariable Long deviceId, @PathVariable String streamId) {
        return null;
    }


}
