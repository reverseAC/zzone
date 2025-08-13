package com.zjh.zzone.admin.api.bo;

import com.zjh.zzone.admin.api.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 用戶信息及其权限和角色
 *
 * @author zjh
 * @date 2025/8/13 22:14
 */
@Getter
@Setter
public class UserInfo {

    @Schema(description = "用户基本信息")
    private SysUser sysUser;

    @Schema(description = "权限标识集合")
    private String[] permissions;

    @Schema(description = "角色标识集合")
    private Long[] roles;
}
