package com.zjh.zzone.iot.media.event.media;

import com.ylg.iot.vo.MediaInfo;
import com.ylg.iot.entity.MediaServer;
import com.ylg.iot.media.gb28181.event.MediaEvent;
import com.ylg.iot.media.zlm.hook.params.OnStreamChangedHookParam;
import com.ylg.iot.vo.StreamContent;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 流到来事件
 */
@Getter
@Setter
public class MediaArrivalEvent extends MediaEvent {

    private MediaInfo mediaInfo;

    private String callId;

    private StreamContent streamInfo;

    private Map<String, String> paramMap;

    private String serverId;

    public MediaArrivalEvent(Object source) {
        super(source);
    }

    public static MediaArrivalEvent getInstance(Object source, OnStreamChangedHookParam hookParam, MediaServer mediaServer, String serverId){
        MediaArrivalEvent mediaArrivalEvent = new MediaArrivalEvent(source);
        mediaArrivalEvent.setMediaInfo(getMediaInfo(hookParam, mediaServer, serverId));
        mediaArrivalEvent.setApp(hookParam.getApp());
        mediaArrivalEvent.setStream(hookParam.getStream());
        mediaArrivalEvent.setMediaServer(mediaServer);
        mediaArrivalEvent.setSchema(hookParam.getSchema());
        mediaArrivalEvent.setParamMap(hookParam.getParamMap());
        return mediaArrivalEvent;
    }

    private static MediaInfo getMediaInfo(OnStreamChangedHookParam param, MediaServer mediaServer, String serverId) {

            MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.setApp(param.getApp());
            mediaInfo.setStream(param.getStream());
            mediaInfo.setSchema(param.getSchema());
            mediaInfo.setMediaServer(mediaServer);
            mediaInfo.setReaderCount(param.getTotalReaderCount());
            mediaInfo.setOnline(param.isRegist());
            mediaInfo.setOriginType(param.getOriginType());
            mediaInfo.setOriginUrl(param.getOriginUrl());
            mediaInfo.setAliveSecond(param.getAliveSecond());
            mediaInfo.setBytesSpeed(param.getBytesSpeed());
            mediaInfo.setParamMap(param.getParamMap());
            if(mediaInfo.getCallId() == null) {
                mediaInfo.setCallId(param.getParamMap().get("callId"));
            }
            mediaInfo.setServerId(serverId);
            List<OnStreamChangedHookParam.MediaTrack> tracks = param.getTracks();
            if (tracks == null || tracks.isEmpty()) {
                return mediaInfo;
            }
            for (OnStreamChangedHookParam.MediaTrack mediaTrack : tracks) {
                switch (mediaTrack.getCodec_id()) {
                    case 0:
                        mediaInfo.setVideoCodec("H264");
                        break;
                    case 1:
                        mediaInfo.setVideoCodec("H265");
                        break;
                    case 2:
                        mediaInfo.setAudioCodec("AAC");
                        break;
                    case 3:
                        mediaInfo.setAudioCodec("G711A");
                        break;
                    case 4:
                        mediaInfo.setAudioCodec("G711U");
                        break;
                }
                if (mediaTrack.getSample_rate() > 0) {
                    mediaInfo.setAudioSampleRate(mediaTrack.getSample_rate());
                }
                if (mediaTrack.getChannels() > 0) {
                    mediaInfo.setAudioChannels(mediaTrack.getChannels());
                }
                if (mediaTrack.getHeight() > 0) {
                    mediaInfo.setHeight(mediaTrack.getHeight());
                }
                if (mediaTrack.getWidth() > 0) {
                    mediaInfo.setWidth(mediaTrack.getWidth());
                }
            }
            return mediaInfo;
        }

}
