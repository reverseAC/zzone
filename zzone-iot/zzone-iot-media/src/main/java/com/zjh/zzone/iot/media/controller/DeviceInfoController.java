package com.zjh.zzone.iot.media.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ylg.core.domain.R;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.media.vo.MediaDeviceVO;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * 设备信息管理 控制器
 *
 * @author zjh
 * @since 2024-12-04 14:27:12
 */
@Slf4j
@RestController
@RequestMapping("/device")
public class DeviceInfoController {

	@Autowired
	private MediaDeviceService deviceService;

	@Autowired
	private MediaDeviceChannelService deviceChannelService;

	/**
	 * 使用国标ID查询视频设备媒体信息
	 *
	 * @param gbId 国标ID
	 * @return 国标设备媒体信息
	 */
	@GetMapping("/{gbId}")
	public R<MediaDeviceVO> devices(@PathVariable String gbId){
		return R.ok(this.deviceService.getByGbId(gbId));
	}

	/**
	 * 查询设备通道列表
	 *
	 * @param gbId 国标id
	 * @return 设备通道列表
	 */
	@GetMapping("/{gbId}/channels")
	public R<List<MediaDeviceChannel>> channels(@PathVariable String gbId) {
		List<MediaDeviceChannel> channels = this.deviceChannelService.getDeviceChannels(gbId);

		return R.ok(channels);
	}

	/**
	 * 添加设备信息（主要用于在设备注册前预置设备信息）
	 *
	 * @param device 设备信息
	 */
	@PostMapping
	public R<?> addDevice(@RequestBody MediaDevice device){
		Assert.hasText(device.getGbId(), "国标编号不能为空");

		boolean existsByGbId = this.deviceService.exists(new QueryWrapper<MediaDevice>().eq("gb_id", device.getGbId()));
		if (existsByGbId) {
			return R.fail("国标编号已存在");
		}

		this.deviceService.save(device);
		return R.ok("成功");
	}

	/**
	 * 更新设备信息
	 *
	 * @param device 设备信息
	 */
	@PutMapping
	public R<?> updateDevice(@RequestBody MediaDevice device){
		// 不能修改国标编号
		MediaDevice dbDevice = this.deviceService.getOne(new QueryWrapper<MediaDevice>().eq("gb_id", device.getGbId()));
		if (dbDevice != null && !dbDevice.getId().equals(device.getId())) {
			return R.fail("国标编号已存在");
		}

		this.deviceService.updateDevice(device);
		return R.ok("成功");
	}

	/**
	 * 删除设备
	 *
	 * @param gbId 国标id
	 */
	@DeleteMapping("/{gbId}")
	public R<?> delete(@PathVariable String gbId){
		MediaDevice device = deviceService.getOne(new QueryWrapper<MediaDevice>().eq("gb_id", gbId));
		Assert.notNull(device, "设备不存在：" + gbId);
		deviceService.removeById(device.getId());
		return R.ok("成功");
	}

	/**
	 * 开启/关闭通道的音频
	 *
	 * @param channelId 通道id
	 * @param audio 是否开启音频
	 */
	@PostMapping("/channel/audio")
	public R<?> changeAudio(@RequestParam Long channelId, @RequestParam Boolean audio){
		this.deviceChannelService.changeAudio(channelId, audio);
		return R.ok("成功");
	}

	/**
	 * 修改通道的码流类型
	 * NOTE：切换码流类型后，需要前端重新发起点播请求
	 *
	 * @param channel 通道信息
	 */
	@PostMapping("/channel/stream/identification/update")
	public R<?> updateChannelStreamIdentification(@RequestBody MediaDeviceChannel channel) {
		deviceChannelService.updateChannelStreamIdentification(channel);
		return R.ok("成功");
	}

	/**
	 * 请求截图
	 *
	 * @param resp http响应对象
	 * @param deviceGbId 设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param mark 标识
	 */
	@GetMapping("/snap/{deviceGbId}/{channelGbId}")
	public void getSnap(HttpServletResponse resp, @PathVariable String deviceGbId, @PathVariable String channelGbId, @RequestParam(required = false) String mark) {
		this.deviceService.checkDeviceEnable(deviceGbId);
		try {
			final InputStream in = Files.newInputStream(new File("snap" + File.separator + deviceGbId + "_" + channelGbId + (mark == null? ".jpg": ("_" + mark + ".jpg"))).toPath());
			resp.setContentType(MediaType.IMAGE_PNG_VALUE);
			ServletOutputStream outputStream = resp.getOutputStream();
			IOUtils.copy(in, resp.getOutputStream());
			in.close();
			outputStream.close();
		} catch (IOException e) {
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
	}
}
