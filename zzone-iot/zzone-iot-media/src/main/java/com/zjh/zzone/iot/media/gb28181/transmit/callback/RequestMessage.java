package com.zjh.zzone.iot.media.gb28181.transmit.callback;

import lombok.Data;

/**
 * 请求信息定义
 *
 * @author zjh
 * @since 2025-03-28 17:35
 */
@Data
public class RequestMessage {
	
	private String id;

	private String key;

	private Object data;
}
