package com.zjh.zzone.iot.media.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylg.core.base.BaseController;
import com.ylg.core.domain.R;
import com.ylg.core.utils.StringUtils;
import com.ylg.core.web.Query;
import com.ylg.iot.entity.RecordShareLog;
import com.ylg.iot.media.contant.RecordStatus;
import com.ylg.iot.media.dto.RecordShareLogDTO;
import com.ylg.iot.media.service.RecordShareLogService;
import com.ylg.iot.media.vo.RecordShareLogVO;
import com.ylg.iot.vo.RecordInfo;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.entity.MediaDevice;
import com.ylg.iot.entity.MediaDeviceChannel;
import com.ylg.iot.enums.media.InviteSessionType;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.contant.InviteResultCode;
import com.ylg.iot.media.gb28181.transmit.callback.DeferredResultHolder;
import com.ylg.iot.media.gb28181.transmit.callback.RequestMessage;
import com.ylg.iot.media.service.MediaDeviceChannelService;
import com.ylg.iot.media.service.MediaDeviceService;
import com.ylg.iot.media.service.PlayService;
import com.ylg.iot.media.utils.DateUtil;
import com.ylg.iot.vo.StreamContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 设备本地录像 控制器
 *
 * @author zjh
 * @since 2025-03-25 20:06
 */
@Slf4j
@RestController
@RequestMapping("/record")
public class RecordController extends BaseController {

	@Autowired
	private MediaDeviceService deviceService;

	@Autowired
	private MediaDeviceChannelService deviceChannelService;

	@Autowired
	private PlayService playService;

	@Autowired
	private RecordShareLogService recordShareLogService;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private UserSetting userSetting;

	/**
	 * 录像控制
	 *
	 * @param deviceGbId 设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param recordCmdStr 开始（Record）/停止录像（StopRecord）
	 * @return 操作结果
	 */
	@GetMapping("/control")
	public DeferredResult<R<String>> recordApi(@RequestParam String deviceGbId, @RequestParam String channelGbId, @RequestParam String recordCmdStr) {
		if (log.isDebugEnabled()) {
			log.debug("开始/停止录像API调用");
		}

		this.deviceService.checkDeviceEnable(deviceGbId);

        MediaDevice device = deviceService.getByGbId(deviceGbId);
		Assert.notNull(device, "设备不存在");

		MediaDeviceChannel channel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
		Assert.notNull(channel, "通道不存在");

		DeferredResult<R<String>> deferredResult = new DeferredResult<>();

		deviceService.record(device, channelGbId, recordCmdStr, (code, msg, data) -> {
			deferredResult.setResult(R.restResult(data, code, msg));
		});
		deferredResult.onTimeout(() -> {
			log.warn("[开始/停止录像] 操作超时, 设备未返回应答指令, {}", deviceGbId);
			deferredResult.setResult(R.fail("操作超时, 设备未应答"));
		});
		return deferredResult;
	}

	/**
	 * 查询设备通道录像状态
	 *
	 * @param deviceGbId 设备国标id
	 * @param channelGbId 通道国标id
	 * @return 设备通道录像状态
	 */
	@GetMapping("/{deviceGbId}/{channelGbId}/status")
	public DeferredResult<R<String>> getRecordStatus(@PathVariable String deviceGbId, @PathVariable String channelGbId) {

		this.deviceService.checkDeviceEnable(deviceGbId);

		DeferredResult<R<String>> deferredResult = new DeferredResult<>();

		this.deviceChannelService.getRecordStatus(deviceGbId, channelGbId, (code, msg, data) -> {
			if (data != null) {
				RecordStatus recordStatus = RecordStatus.getByCode(data);
				String recordStatusStr = recordStatus == null ? data : recordStatus.getDesc();
				deferredResult.setResult(R.restResult(recordStatusStr, code, msg));
			}
		});
		deferredResult.onTimeout(() -> {
			log.warn("[查询录像状态] 操作超时, 设备未返回应答指令, {}", deviceGbId);
			deferredResult.setResult(R.fail("操作超时, 设备未应答"));
		});
		return deferredResult;
	}

	/**
	 * 录像查询
	 *
	 * @param deviceGbId  设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param startTime   开始时间
	 * @param endTime     结束时间
	 * @return 录像信息
	 */
	@GetMapping("/query/{deviceGbId}/{channelGbId}")
	public DeferredResult<R<RecordInfo>> recordInfo(@PathVariable String deviceGbId, @PathVariable String channelGbId, String startTime, String endTime) {

		if (log.isDebugEnabled()) {
			log.debug(String.format("录像信息查询 API调用，deviceGbId：%s ，startTime：%s， endTime：%s", deviceGbId, startTime, endTime));
		}

		this.deviceService.checkDeviceEnable(deviceGbId);

		DeferredResult<R<RecordInfo>> result = new DeferredResult<>(userSetting.getRecordInfoTimeout().longValue());
		if (!DateUtil.verification(startTime, DateUtil.formatter)) {
			result.setResult(R.fail("startTime格式为" + DateUtil.PATTERN));
		}
		if (!DateUtil.verification(endTime, DateUtil.formatter)) {
			result.setResult(R.fail("endTime格式为" + DateUtil.PATTERN));
		}

		MediaDevice device = deviceService.getByGbId(deviceGbId);
		Assert.notNull(device, "设备不存在");
		MediaDeviceChannel channel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
		Assert.notNull(channel, "通道不存在");
		deviceChannelService.queryRecordInfo(device, channel, startTime, endTime, (code, msg, data) -> {
			R<RecordInfo> resultR = null;
			if (code == 200) {
				resultR = R.ok(data);
			} else {
				resultR = R.fail(msg);
			}
			result.setResult(resultR);
		});
		result.onTimeout(() -> {
			R<RecordInfo> resultR = R.fail("获取超时");
			result.setResult(resultR);
		});
		return result;
	}

	/**
	 * 录像下载开始
	 *
	 * @param deviceGbId      设备国标编号
	 * @param channelGbId     通道国标编号
	 * @param startTime     开始时间
	 * @param endTime       结束时间
	 * @param downloadSpeed 下载倍速
	 * @return 流信息
	 */
	@GetMapping("/download/start/{deviceGbId}/{channelGbId}")
	public DeferredResult<R<StreamContent>> download(@PathVariable String deviceGbId, @PathVariable String channelGbId,
													 String startTime, String endTime, @RequestParam(defaultValue = "4")Integer downloadSpeed) {

		if (log.isDebugEnabled()) {
			log.debug(String.format("历史媒体下载 API调用，deviceGbId：%s，channelGbId：%s，downloadSpeed：%s", deviceGbId, channelGbId, downloadSpeed));
		}

		this.deviceService.checkDeviceEnable(deviceGbId);

		String uuid = UUID.randomUUID().toString();
		String key = DeferredResultHolder.CALLBACK_CMD_DOWNLOAD + deviceGbId + channelGbId;
		DeferredResult<R<StreamContent>> result = new DeferredResult<>(30000L);
		resultHolder.put(key, uuid, result);
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setId(uuid);
		requestMessage.setKey(key);

		MediaDevice device = deviceService.getByGbId(deviceGbId);
		Assert.notNull(device, "未找到设备 deviceGbId: " + deviceGbId + ",channelGbId:{}" + channelGbId);

		MediaDeviceChannel channel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
		Assert.notNull(channel, "未找到通道 deviceGbId: " + deviceGbId + ",channelGbId:{}" + channelGbId);

		playService.download(device, channel, startTime, endTime, downloadSpeed,
				(code, msg, data) -> {

					R<StreamContent> r = new R<>();
					if (code == InviteResultCode.SUCCESS.getCode()) {
						r = R.ok();

						if (data != null) {
							r.setData(data.getStreamContent());
						}
					} else {
						r.setCode(code);
						r.setMsg(msg);
					}
					requestMessage.setData(r);
					resultHolder.invokeResult(requestMessage);
				});

		return result;
	}

	/**
	 * 录像下载停止
	 *
	 * @param deviceGbId  设备国标编号
	 * @param channelGbId 通道国标编号
	 * @param stream    流ID
	 */
	@GetMapping("/download/stop/{deviceGbId}/{channelGbId}/{stream}")
	public R<Void> downloadStop(@PathVariable String deviceGbId, @PathVariable String channelGbId, @PathVariable String stream) {

		if (log.isDebugEnabled()) {
			log.debug(String.format("设备历史媒体下载停止 API调用，deviceGbId/channelGbId：%s_%s", deviceGbId, channelGbId));
		}

		MediaDevice device = deviceService.getByGbId(deviceGbId);
		Assert.notNull(device, "未找到设备 deviceGbId: " + deviceGbId + ",channelGbId:{}" + channelGbId);

		MediaDeviceChannel channel = deviceChannelService.getChannelByGbId(device.getId(), channelGbId);
		Assert.notNull(device, "未找到通道 deviceGbId: " + deviceGbId + ",channelGbId:{}" + channelGbId);

		playService.stop(InviteSessionType.DOWNLOAD, device, channel, stream);
		return R.ok();
	}

	/**
	 * 获取录像下载进度
	 *
	 * @param deviceId  设备国标编号
	 * @param channelId 通道国标编号
	 * @param stream    流ID
	 * @return 流信息
	 */
	@GetMapping("/download/progress/{deviceId}/{channelId}/{stream}")
	public R<StreamContent> getProgress(@PathVariable String deviceId, @PathVariable String channelId, @PathVariable String stream) {

		this.deviceService.checkDeviceEnable(deviceId);

		MediaDevice device = deviceService.getByGbId(deviceId);
		Assert.notNull(device, "未找到设备 deviceId: " + deviceId + ",channelId:{}" + channelId);

		MediaDeviceChannel channel = deviceChannelService.getByGbId(deviceId, channelId);
		Assert.notNull(device, "未找到通道 deviceId: " + deviceId + ",channelId:{}" + channelId);

		StreamDetail downLoadInfo = playService.getDownLoadInfo(device, channel, stream);
		if (downLoadInfo == null) {
			return R.fail("未找到下载信息");
		}

		return R.ok(downLoadInfo.getStreamContent());
	}

	/**
	 * 设备录像交由浏览器下载
	 *
	 * @param response http响应实体
	 * @param path     /opt后的路径
	 * @param fileName fileName
	 * @param token    token
	 */
	@GetMapping("/file/download")
	public void recordDownloadProgress(HttpServletResponse response, @RequestParam String path, @RequestParam String fileName, @RequestParam String token) {
		String downLoadFilePath = userSetting.getDownloadPath() + path;
		try {
			String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
			// 1. 设置响应头（强制浏览器下载）
			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
			// 2. 使用 RestTemplate 或 HttpURLConnection 获取远程文件流
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.execute(
					downLoadFilePath,
					HttpMethod.GET,
					null,
					clientHttpResponse -> {
						// 尝试从远程响应头中获取 Content-Length
						List<String> contentLengthHeaders = clientHttpResponse.getHeaders().get("Content-Length");
						if (contentLengthHeaders != null && !contentLengthHeaders.isEmpty()) {
							try {
								int contentLength = Integer.parseInt(contentLengthHeaders.get(0));
								response.setContentLength(contentLength);
							} catch (NumberFormatException e) {
								// 忽略非法 content-length
							}
						}
						InputStream inputStream = clientHttpResponse.getBody();
						OutputStream outputStream = response.getOutputStream();
						StreamUtils.copy(inputStream, outputStream);
						response.flushBuffer();
						return null;
					}
			);
		} catch (Exception e) {
			log.error("录像文件（{}）通过浏览器下载异常：{}", downLoadFilePath, e.getMessage());
		}
	}

	/**
	 * 创建分享
	 *
	 * @param params 分享信息
	 */
	@PostMapping("/share")
	public R<String> createShare(@RequestBody RecordShareLogDTO params) {
		Long shareLogId = recordShareLogService.createShare(params);
	    return R.ok(String.valueOf(shareLogId));
	}

	/**
	 * 更新分享
	 *
	 * @param params 分享信息
	 */
	@PutMapping("/share")
	public R<Void> updateShare(@RequestBody RecordShareLogDTO params) {
		recordShareLogService.updateShare(params);
		return R.ok();
	}

	/**
	 * 查询分享详情
	 *
	 * @param logId 分享记录id
	 */
	@GetMapping("/share/{logId}")
	public R<RecordShareLogVO> getShare(@PathVariable Long logId) {
		RecordShareLogVO shareLog = recordShareLogService.getShareLogById(logId);
		return R.ok(shareLog);
	}

	/**
	 * 删除分享
	 *
	 * @param logId 分享记录id
	 */
	@DeleteMapping("/share/{logId}")
	public R<RecordShareLogVO> deleteShare(@PathVariable Long logId) {
		RecordShareLog shareLog = recordShareLogService.getById(logId);
		Assert.notNull(shareLog, "未找到分享记录 logId: " + logId);
		recordShareLogService.removeById(logId);
		return R.ok();
	}

	/**
	 * 分享失效
	 *
	 * @param logId 分享记录id
	 */
	@PostMapping("/share/expire/{logId}")
	public R<Void> expireShare(@PathVariable Long logId) {
		recordShareLogService.expireShare(logId);
		return R.ok();
	}

	/**
	 * 分页查询分享记录
	 *
	 * @param params 查询条件
	 * @param query 分页条件
	 * @return 分页结果
	 */
	@GetMapping("/share/page")
	public R<List<RecordShareLogVO>> sharePage(RecordShareLogDTO params, Query query) {
		if (StringUtils.isEmpty(params.getDeviceGbId()) || StringUtils.isEmpty(params.getChannelGbId())) {
			return R.fail("设备id和通道id不能为空");
		}

		Page<RecordShareLogVO> page = new Page<>(query.getPage(), query.getLimit());
		IPage<RecordShareLogVO> iPage = recordShareLogService.shareLogPage(page, params);
		return success(iPage);
	}

	/**
	 * 录像文件列表（监测适配）
	 *
	 * @param deviceId  设备id
	 * @param startTime 开始时间
	 * @param endTime   结束时间
	 * @return RecordInfo
	 */
	@GetMapping("/record/info/{deviceId}")
	public R<RecordInfo> recordInfo(@PathVariable Long deviceId, @RequestParam String startTime, @RequestParam String endTime) {
		return null;
	}

	/**
	 * 录像下载（监测适配）
	 *
	 * @param deviceId  设备id
	 * @param startTime 开始时间
	 * @param endTime   结束时间
	 * @return 成功或失败提示
	 */
	@GetMapping("/record/download/{deviceId}")
	public R<StreamContent> recordDownload(@PathVariable Long deviceId, @RequestParam String startTime, @RequestParam String endTime, String downloadSpeed) {
		return null;
	}

	/**
	 * 录像下载停止（监测适配）
	 *
	 * @param deviceId 设备id
	 * @param streamId 流id
	 * @return 成功或失败提示
	 */
	@GetMapping("/record/download/stop/{deviceId}/{streamId}")
	public R<Void> recordDownload(@PathVariable Long deviceId, @PathVariable String streamId) {
		return null;
	}

	/**
	 * 录像下载进度查询（监测适配）
	 *
	 * @param deviceId 设备id
	 * @return 成功或失败提示
	 */
	@GetMapping("/record/download/progress/{deviceId}/{streamId}")
	public R<StreamContent> recordDownloadProgress(@PathVariable Long deviceId, @PathVariable String streamId) {
		return null;
	}
}