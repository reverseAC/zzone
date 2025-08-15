package com.zjh.zzone.iot.media.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ylg.core.base.BaseController;
import com.ylg.core.domain.R;
import com.ylg.core.exception.CheckedException;
import com.ylg.iot.entity.Device;
import com.ylg.iot.media.service.InviteStreamService;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.service.PlayService;
import com.ylg.iot.media.vo.AudioBroadcastResultVO;
import com.ylg.iot.media.contant.InviteResultCode;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.vo.MediaDeviceVO;
import com.ylg.iot.media.gb28181.transmit.callback.DeferredResultHolder;
import com.ylg.iot.vo.StreamContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 国标设备点播 控制器
 *
 * @author zjh
 * @since 2025-03-25 20:06
 */
@RestController
@RequestMapping("/play")
@Slf4j
public class PlayController extends BaseController {

    @Resource
    private PlayService playService;

    @Resource
    private MediaDeviceService deviceService;

    @Resource
    private MediaDeviceChannelService deviceChannelService;

    @Resource
    private InviteStreamService inviteStreamService;

    @Resource
    private UserSetting userSetting;

    @Resource
    private DeferredResultHolder resultHolder;

    /**
     * 点播
     *
     * @param deviceGbId 设备国标编号
     * @param channelGbId 通道国标编号
     * @return 流信息
     */
    @GetMapping("/start/{deviceGbId}/{channelGbId}")
    public DeferredResult<R<StreamContent>> play(@PathVariable String deviceGbId, @PathVariable String channelGbId) {

        log.info("[开始点播] deviceId：{}, channelId：{}, ", deviceGbId, channelGbId);
        Assert.notNull(deviceGbId, "设备国标编号不可为空");
        Assert.notNull(channelGbId, "通道国标编号不可为空");

        this.deviceService.checkDeviceEnable(deviceGbId);

        // 获取可用的zlm
        MediaDeviceVO device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在");
        MediaDeviceChannel channel = deviceChannelService.getByGbId(device.getId(), channelGbId);
        Assert.notNull(channel, "通道不存在");

        // 使用DeferredResult提供异步处理机制，实现超时回调、结果延迟设置
        DeferredResult<R<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        // 设置一个超时回调
        result.onTimeout(()->{
            log.info("[点播等待超时] deviceId：{}, channelId：{}, ", deviceGbId, channelGbId);
            result.setResult(R.fail("点播超时"));

            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, channel.getId());
            deviceChannelService.stopPlay(channel.getId());
        });

        ErrorCallback<StreamDetail> callback  = (code, msg, streamInfo) -> {
            R<StreamContent> resultR = null;
            if (code == InviteResultCode.SUCCESS.getCode()) {
                resultR = R.ok();

                if (streamInfo != null) {
                    if (!ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix()) && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    resultR.setData(streamInfo.getStreamContent());
                } else {
                    resultR = R.fail(msg);
                }
            } else {
                resultR = R.fail(msg);
            }
            result.setResult(resultR);
        };
        playService.play(device, channel, callback);
        return result;
    }

    /**
     * 点播停止
     *
     * @param deviceGbId 设备国标编号
     * @param channelGbId 通道国标编号
     */
    @GetMapping("/stop/{deviceGbId}/{channelGbId}")
    public R<Void> playStop(@PathVariable String deviceGbId, @PathVariable String channelGbId) {

        log.debug("设备预览/回放停止API调用，streamId：{}_{}", deviceGbId, channelGbId);

        if (deviceGbId == null || channelGbId == null) {
            throw new CheckedException("deviceId或channelId不能为空");
        }

        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在");
        MediaDeviceChannel channel = deviceChannelService.getByGbId(device.getId(), channelGbId);
        Assert.notNull(channel, "通道不存在");
        String streamId = String.format("%s_%s", device.getGbId(), channel.getGbId());
        playService.stop(InviteSessionType.PLAY, device, channel, streamId);

        return R.ok();
    }

    /**
     * 语音广播命令
     *
     * @param deviceGbId 设备国标编号
     * @param channelGbId 通道国标编号
     * @param broadcastMode 广播/对讲
     * @return 语音信息
     */
    @PostMapping("/broadcast/{deviceGbId}/{channelGbId}")
    public R<AudioBroadcastResultVO> broadcastApi(@PathVariable String deviceGbId, @PathVariable String channelGbId, @RequestParam Boolean broadcastMode) {
        this.deviceService.checkDeviceEnable(deviceGbId);

        AudioBroadcastResultVO audioBroadcastResultVO = playService.audioBroadcast(deviceGbId, channelGbId, broadcastMode);
        return R.ok(audioBroadcastResultVO);
    }

    /**
     * 停止语音广播
     *
     * @param deviceGbId 设备Id
     * @param channelGbId  通道Id
     */
    @PostMapping("/broadcast/stop/{deviceGbId}/{channelGbId}")
    public R<Void> stopBroadcast(@PathVariable String deviceGbId, @PathVariable String channelGbId) {
        if (log.isDebugEnabled()) {
            log.debug("停止语音广播API调用");
        }

        MediaDevice device = deviceService.getByGbId(deviceGbId);
        Assert.notNull(device, "设备不存在");
        MediaDeviceChannel channel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
        Assert.notNull(channel, "通道不存在");
        playService.stopAudioBroadcast(device, channel);
        return R.ok();
    }

    /**
     * 多设备点播开始（监测定制接口）
     *
     * @param deviceIdList 设备id列表
     * @return List<StreamContent>
     */
    @PostMapping("/start/list")
    public R<List<StreamContent>> playStart(@RequestBody List<Long> deviceIdList) {
        // 校验设备是否存在，不存在的输出log，只返回正常的流信息
        List<Device> iotDeviceList = null; // TODO 调用feign接口（或其他方式）获取iot设备信息，获取设备国标编码

        List<Long> iotDeviceIdList = iotDeviceList
                .stream().map(Device::getId)
                .collect(Collectors.toList());
        deviceIdList.stream().filter(id -> !iotDeviceIdList.contains(id)).forEach(id -> log.error("点播的设备不存在{}", id));

        List<String> iotDeviceSnList = iotDeviceList
                .stream().map(Device::getSn)
                .collect(Collectors.toList());

        List<MediaDevice> deviceList = deviceService.list(new QueryWrapper<MediaDevice>().in("gb_id", iotDeviceSnList).select("id", "gb_id"));

        List<StreamContent> streamContentList = new ArrayList<>();
        for (MediaDevice device : deviceList) {
            try {
                // 当前版本默认单通道，请保持设备国标id与通道id相同
                DeferredResult<R<StreamContent>> play = play(device.getGbId(), device.getGbId());


                StreamContent streamContent = null;
                streamContent.setDeviceId(device.getId());
                streamContentList.add(streamContent);

            } catch (Exception e) {
                log.error("设备（{}）点播异常：{}", device.getId(), e.getMessage());
            }
        }
        return success(streamContentList);
    }

}
