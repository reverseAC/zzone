package com.zjh.zzone.admin.api.feign;

import com.zjh.zzone.admin.api.bo.UserInfo;
import com.zjh.zzone.admin.api.dto.UserDTO;
import com.zjh.zzone.common.core.base.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 用户信息 Feign
 *
 * @author zjh
 * @date 2025-08-14 19:58
 */
@FeignClient(contextId = "remoteUserService", value = "admin")
public interface RemoteUserService {

    @GetMapping("/user/info")
    R<UserInfo> userInfo(@SpringQueryMap UserDTO user);
}
