package com.zjh.zzone.iot.media.controller;

import com.ylg.core.domain.R;
import com.ylg.core.enums.StatusEnum;
import com.ylg.core.exception.CheckedException;
import com.ylg.iot.RemoteIotService;
import com.ylg.iot.dto.DeviceQueryDTO;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.service.PtzService;
import com.ylg.iot.vo.DeviceInternalVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 设备控制 控制器
 *
 * @author zjh
 * @since 2024-12-04 14:27:12
 */
@Slf4j
@RestController
@RequestMapping("/device/ptz")
public class PtzController {

    @Autowired
    private MediaDeviceService deviceService;

	@Autowired
	private PtzService ptzService;

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private RemoteIotService remoteIotService;

	// 定长线程池，用于延迟任务
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);


	/**
	 * 通用控制 (参考国标文档A.3.1指令格式)
	 *
	 * @param deviceGbId 设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param cmdCode 指令码
	 * @param parameter1 数据一
	 * @param parameter2 数据二
	 * @param combindCode2 组合码二
	 */
	@GetMapping("/common/{deviceId}/{channelId}")
	public void frontEndCommand(@PathVariable String deviceGbId, @PathVariable String channelGbId,Integer cmdCode, Integer parameter1, Integer parameter2, Integer combindCode2){

		if (log.isDebugEnabled()) {
			log.debug("设备云台控制 API调用，deviceId：{} ，channelId：{} ，cmdCode：{} parameter1：{} parameter2：{}", deviceGbId, channelGbId, cmdCode, parameter1, parameter2);
		}

		this.deviceService.checkDeviceEnable(deviceGbId);

		if (parameter1 == null || parameter1 < 0 || parameter1 > 255) {
			throw new CheckedException("parameter1 为 0-255的数字");
		}
		if (parameter2 == null || parameter2 < 0 || parameter2 > 255) {
			throw new CheckedException("parameter2 为 0-255的数字");
		}
		if (combindCode2 == null || combindCode2 < 0 || combindCode2 > 15) {
			throw new CheckedException("combindCode2 为 0-15的数字");
		}

		MediaDevice device = deviceService.getByGbId(deviceGbId);
		Assert.notNull(device, "设备[" + deviceGbId + "]不存在");

		ptzService.frontEndCommand(device, channelGbId, cmdCode, parameter1, parameter2, combindCode2);
	}

	/**
	 * 云台控制
	 *
	 * @param deviceGbId 设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param command 控制指令,允许值: left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop
	 * @param horizonSpeed 水平速度(0-255)
	 * @param verticalSpeed 垂直速度(0-255)
	 * @param zoomSpeed 缩放速度(0-15)
	 */
	@GetMapping("/{deviceGbId}/{channelGbId}")
	public R<Void> ptz(@PathVariable String deviceGbId, @PathVariable String channelGbId, String command,
					@RequestParam(defaultValue = "100") Integer horizonSpeed,
					@RequestParam(defaultValue = "100") Integer verticalSpeed,
					@RequestParam(defaultValue = "15") Integer zoomSpeed){

		if (log.isDebugEnabled()) {
			log.debug("设备云台控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，horizonSpeed：{} ，verticalSpeed：{} ，zoomSpeed：{}", deviceGbId, channelGbId, command, horizonSpeed, verticalSpeed, zoomSpeed);
		}
		if (horizonSpeed < 0 || horizonSpeed > 255) {
			return R.fail("水平速度为 0-255 的数字");
		}
		if (verticalSpeed < 0 || verticalSpeed > 255) {
			return R.fail("垂直速度为 0-255 的数字");
		}
		if (zoomSpeed < 0 || zoomSpeed > 15) {
			return R.fail("缩放速度为 0-15 的数字");
		}

		int cmdCode = 0;
		switch (command){
			case "left":
				cmdCode = 2;
				break;
			case "right":
				cmdCode = 1;
				break;
			case "up":
				cmdCode = 8;
				break;
			case "down":
				cmdCode = 4;
				break;
			case "upleft":
				cmdCode = 10;
				break;
			case "upright":
				cmdCode = 9;
				break;
			case "downleft":
				cmdCode = 6;
				break;
			case "downright":
				cmdCode = 5;
				break;
			case "zoomin":
				cmdCode = 16;
				break;
			case "zoomout":
				cmdCode = 32;
				break;
			case "stop":
				horizonSpeed = 0;
				verticalSpeed = 0;
				zoomSpeed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceGbId, channelGbId, cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);

		return R.ok();
	}

	/**
	 * 光圈控制
	 *
	 * @param deviceGbId 设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param command 控制指令,允许值: in, out, stop
	 * @param speed 光圈速度(0-255)
	 */
	@GetMapping("/iris/{deviceGbId}/{channelGbId}")
	public R<Void> iris(@PathVariable String deviceGbId,@PathVariable String channelGbId, String command,
					 @RequestParam(defaultValue = "100") Integer speed){

		if (log.isDebugEnabled()) {
			log.debug("设备光圈控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceGbId, channelGbId, command, speed);
		}

		if (speed < 0 || speed > 255) {
			R.fail("光圈速度为 0-255 的数字");
		}

		int cmdCode = 0x40;
		switch (command){
			case "in":
				cmdCode = 0x44;
				break;
			case "out":
				cmdCode = 0x48;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceGbId, channelGbId, cmdCode, 0, speed, 0);

		return R.ok();
	}

	/**
	 * 聚焦控制
	 *
	 * @param deviceGbId 设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param command 控制指令,允许值: near, far, stop
	 * @param speed 聚焦速度(0-255)
	 */
	@GetMapping("/focus/{deviceGbId}/{channelGbId}")
	public R<Void> focus(@PathVariable String deviceGbId,@PathVariable String channelGbId, String command,
					  @RequestParam(defaultValue = "100") Integer speed){

		if (log.isDebugEnabled()) {
			log.debug("设备聚焦控制 API调用，deviceId：{} ，channelId：{} ，command：{} ，speed：{} ",deviceGbId, channelGbId, command, speed);
		}

		if (speed < 0 || speed > 255) {
			R.fail("光圈速度为 0-255 的数字");
		}

		int cmdCode = 0x40;
		switch (command){
			case "near":
				cmdCode = 0x42;
				break;
			case "far":
				cmdCode = 0x41;
				break;
			case "stop":
				speed = 0;
				break;
			default:
				break;
		}
		frontEndCommand(deviceGbId, channelGbId, cmdCode, speed, 0, 0);

		return R.ok();
	}

	/**
	 * 云台控制（监测定制接口）
	 *
	 * @param deviceId 设备id
	 * @param command  控制命令（left, right, up, down, upleft, upright, downleft, downright, zoomin, zoomout, stop）
	 * @return 操作结束
	 */
	@GetMapping("/{deviceId}")
	public R<Void> list(@PathVariable Long deviceId, @RequestParam String command) {

		DeviceQueryDTO deviceQueryDTO = new DeviceQueryDTO();
		deviceQueryDTO.setDeviceId(deviceId);
		R<List<DeviceInternalVO>> iotDeviceR = remoteIotService.internalDeviceList(deviceQueryDTO);
		if (R.SUCCESS != iotDeviceR.getCode()) {
			throw new CheckedException(iotDeviceR.getMsg());
		}

		List<DeviceInternalVO> iotDeviceList = iotDeviceR.getData();
		if (CollectionUtils.isEmpty(iotDeviceList)) {
			return R.fail("设备不存在:" + deviceId);
		}
		DeviceInternalVO deviceInternal = iotDeviceList.get(0);
		if (StatusEnum.DISABLE.getCode().equals(deviceInternal.getStatus())) {
			return R.fail("设备已禁用,无法操作");
		}

		MediaDevice device = deviceService.getByGbId(deviceInternal.getSn());
		Assert.notNull(device, "设备不存在");
		log.error("媒体设备设备不存在: {}", deviceInternal.getSn());

		ptz(device.getGbId(), device.getGbId(), command, userSetting.getPtzHorizonSpeed(), userSetting.getPtzVerticalSpeed(), userSetting.getPtzZoomSpeed());

		// 使用 CountDownLatch 来等待“停止命令”执行完成
		CountDownLatch latch = new CountDownLatch(1);
		// 延时调用停止命令
		scheduler.schedule(() -> {
			// 发送停止命令
			ptz(device.getGbId(), device.getGbId(), "stop", userSetting.getPtzHorizonSpeed(), userSetting.getPtzVerticalSpeed(), userSetting.getPtzZoomSpeed());
			// 标记任务完成
			latch.countDown();
		}, userSetting.getPtzActionDurationSeconds(), TimeUnit.SECONDS);

		try {
			// 3. 等待停止命令发送完成（最大等待 3 秒，避免死等）
			boolean success = latch.await(3, TimeUnit.SECONDS);
			if (!success) {
				throw new RuntimeException("停止命令发送超时");
			}
		} catch (Exception e) {
			log.error("设备（{}）云台控制异常：{}", deviceId, e.getMessage());
			return R.fail("操作失败");
		}

		return R.ok(null, "成功");
	}

}
