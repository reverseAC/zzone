package com.zjh.zzone.iot.media.gb28181.session;

import com.ylg.iot.media.config.ServerInstanceConfig;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.constant.MediaCacheConstants;
import com.ylg.iot.media.bo.SsrcTransaction;
import com.ylg.redis.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频流session管理器，管理视频预览、预览回放的通信句柄
 */
@Component
public class SipInviteSessionManager {

	@Autowired
	private UserSetting userSetting;

	@Autowired
	private ServerInstanceConfig serverInstance;

	/**
	 * 添加一个点播/回放的事务信息
	 */
	public void put(SsrcTransaction ssrcTransaction){
		String streamKey = MediaCacheConstants.SIP_INVITE_SESSION_STREAM + serverInstance.getInstanceId()
				+ ":"
				+ ssrcTransaction.getApp() + ssrcTransaction.getStream();
		RedisUtils.setObject(streamKey, ssrcTransaction);

		String callIdKey = MediaCacheConstants.SIP_INVITE_SESSION_CALL_ID + serverInstance.getInstanceId()
				+ ":"
				+ ssrcTransaction.getCallId();
		RedisUtils.setObject(callIdKey, ssrcTransaction);
	}

	public SsrcTransaction getSsrcTransactionByStream(String app, String stream){
		String key = MediaCacheConstants.SIP_INVITE_SESSION_STREAM + serverInstance.getInstanceId()
				+ ":"
				+ app + stream;
		return RedisUtils.getObject(key, SsrcTransaction.class);
	}

	public SsrcTransaction getSsrcTransactionByCallId(String callId){
		String key = MediaCacheConstants.SIP_INVITE_SESSION_CALL_ID + serverInstance.getInstanceId()
				+ ":"
				+ callId;
		return RedisUtils.getObject(key, SsrcTransaction.class);
	}

	public List<SsrcTransaction> getSsrcTransactionByDeviceId(String deviceId){
		String key = MediaCacheConstants.SIP_INVITE_SESSION_CALL_ID + serverInstance.getInstanceId();
		List<SsrcTransaction> values = RedisUtils.getCacheList(key, SsrcTransaction.class);

		List<SsrcTransaction> result = new ArrayList<>();
		for (SsrcTransaction value : values) {
			if (value != null && deviceId.equals(value.getDeviceId())) {
				result.add(value);
			}
		}
		return result;
	}
	
	public void removeByStream(String app, String stream) {
		SsrcTransaction ssrcTransaction = getSsrcTransactionByStream(app, stream);
		if (ssrcTransaction == null ) {
			return;
		}
		RedisUtils.deleteObject(MediaCacheConstants.SIP_INVITE_SESSION_STREAM + serverInstance.getInstanceId() + ":" + app + stream);

		if (ssrcTransaction.getCallId() != null) {
			RedisUtils.deleteObject(MediaCacheConstants.SIP_INVITE_SESSION_CALL_ID + serverInstance.getInstanceId() + ":" + ssrcTransaction.getCallId());
		}
	}

	public void removeByCallId(String callId) {
		SsrcTransaction ssrcTransaction = getSsrcTransactionByCallId(callId);
		if (ssrcTransaction == null ) {
			return;
		}
		RedisUtils.deleteObject(MediaCacheConstants.SIP_INVITE_SESSION_CALL_ID + serverInstance.getInstanceId() + ":" + ssrcTransaction.getCallId());

		if (ssrcTransaction.getStream() != null) {
			RedisUtils.deleteObject(MediaCacheConstants.SIP_INVITE_SESSION_STREAM + serverInstance.getInstanceId() + ":" + ssrcTransaction.getApp() + ssrcTransaction.getStream());
		}
	}

	public List<SsrcTransaction> getAll() {
		String key = MediaCacheConstants.SIP_INVITE_SESSION_CALL_ID + serverInstance.getInstanceId();
		return RedisUtils.getCacheList(key, SsrcTransaction.class);
	}
}
