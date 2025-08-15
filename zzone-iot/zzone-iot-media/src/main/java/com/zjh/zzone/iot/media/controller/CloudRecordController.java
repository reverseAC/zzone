package com.zjh.zzone.iot.media.controller;

import com.ylg.core.domain.R;
import com.ylg.iot.media.bo.StreamDetail;
import com.ylg.iot.media.callback.ErrorCallback;
import com.ylg.iot.media.config.UserSetting;
import com.ylg.iot.media.contant.ResultCode;
import com.ylg.iot.media.service.CloudRecordService;
import com.ylg.iot.vo.DownloadFileInfo;
import com.ylg.iot.vo.StreamContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/cloud/record")
public class CloudRecordController {

    @Autowired
    private CloudRecordService cloudRecordService;

    @Autowired
    private UserSetting userSetting;


    /**
     * 获取播放地址
     *
     * @param recordId 录像记录的ID
     * @return 录像信息
     */
    @ResponseBody
    @GetMapping("/play/path")
    public DownloadFileInfo getPlayUrlPath(@RequestParam(required = true) Long recordId) {
        return cloudRecordService.getPlayUrlPath(recordId);
    }

    /**
     * 加载录像文件形成播放地址
     *
     * @param app     应用名
     * @param stream  流ID
     * @param date    日期，例如 2025-04-10
     * @return 录像信息
     */
    @ResponseBody
    @GetMapping("/loadRecord")
    public DeferredResult<R<StreamContent>> loadRecord(HttpServletRequest request,
                                   @RequestParam String app, @RequestParam String stream, @RequestParam String date) {

        DeferredResult<R<StreamContent>> result = new DeferredResult<>();

        result.onTimeout(()->{
            log.info("[加载录像文件超时] app={}, stream={}, date={}", app, stream, date);
            R<StreamContent> wvpResult = new R<>();
            wvpResult.setCode(500);
            wvpResult.setMsg("加载录像文件超时");
            result.setResult(wvpResult);
        });

        ErrorCallback<StreamDetail> callback = (code, msg, streamInfo) -> {

            R<StreamContent> wvpResult = new R<>();
            if (code == ResultCode.SUCCESS.getCode()) {
                wvpResult.setCode(ResultCode.SUCCESS.getCode());
                wvpResult.setMsg(ResultCode.SUCCESS.getMsg());

                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo=streamInfo.clone();//深拷贝
                        String host;
                        try {
                            URL url=new URL(request.getRequestURL().toString());
                            host=url.getHost();
                        } catch (MalformedURLException e) {
                            host=request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    if (!org.springframework.util.ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix()) && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    wvpResult.setData(streamInfo.getStreamContent());
                } else {
                    wvpResult.setCode(code);
                    wvpResult.setMsg(msg);
                }
            } else {
                wvpResult.setCode(code);
                wvpResult.setMsg(msg);
            }
            result.setResult(wvpResult);
        };

        cloudRecordService.loadRecord(app, stream, date, callback);
        return result;
    }

    @ResponseBody
    @GetMapping("/load")
    public DeferredResult<ResponseEntity<Void>> load(HttpServletRequest request,
                                     @RequestParam String app, @RequestParam String stream, String date) {

        DeferredResult<ResponseEntity<Void>> response = new DeferredResult<>();

        response.onTimeout(()->{
            log.info("[加载录像文件超时] app={}, stream={}, date={}", app, stream, date);
            ResponseEntity<Void> result = ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            response.setResult(result);
        });

        ErrorCallback<StreamDetail> callback = (code, msg, streamInfo) -> {

            ResponseEntity<Void> result = null;
            if (code == ResultCode.SUCCESS.getCode()) {

                if (streamInfo != null) {
                    if (!org.springframework.util.ObjectUtils.isEmpty(streamInfo.getMediaServer().getTranscodeSuffix()) && !"null".equalsIgnoreCase(streamInfo.getMediaServer().getTranscodeSuffix())) {
                        streamInfo.setStream(streamInfo.getStream() + "_" + streamInfo.getMediaServer().getTranscodeSuffix());
                    }
                    StreamContent streamContent = streamInfo.getStreamContent();
                    String fmp4 = streamContent.getFmp4();
                    result = ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(fmp4))
                            .build();
                }
            } else {
                result = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            response.setResult(result);
        };

        cloudRecordService.loadRecord(app, stream, date, callback);

        return response;

    }

    /**
     * 删除录像文件
     *
     * @param ids 文件id
     */
    @ResponseBody
    @DeleteMapping("/delete")
    public void deleteFileByIds(@RequestBody Set<Long> ids) {
        cloudRecordService.deleteFileByIds(ids);
    }

}
