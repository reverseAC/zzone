package com.zjh.zzone.iot.media.controller;

import com.ylg.core.domain.R;
import com.ylg.core.utils.StringUtils;
import com.ylg.iot.media.vo.PlatformInfoVO;
import com.ylg.iot.media.config.SipConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 视频平台接入信息管理 控制器
 *
 * @author zjh
 * @since 2025-07-01 14:27:12
 */
@Slf4j
@RestController
@RequestMapping("/platform/info")
public class PlatformInfoController {

	@Autowired
	private SipConfig sipConfig;

	/**
	 * 获取视频平台设备接入信息
	 *
	 * @return 接入信息
	 */
	@GetMapping
	public R<PlatformInfoVO> info(){
		PlatformInfoVO platformInfoVO = new PlatformInfoVO();
		platformInfoVO.setId(sipConfig.getId());
		platformInfoVO.setDomain(sipConfig.getDomain());
		platformInfoVO.setIp(StringUtils.isEmpty(sipConfig.getShowIp()) ? sipConfig.getIp() : sipConfig.getShowIp());
		platformInfoVO.setPort(sipConfig.getPort());
		platformInfoVO.setPassword(sipConfig.getPassword());
		return R.ok(platformInfoVO);
	}

}
