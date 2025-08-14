package com.zjh.zzone.admin.api.entity;

import com.zjh.zzone.common.mybatis.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户表
 *
 * @author zjh
 * @date 2025-08-13 11:39
 */
@Data
public class SysUser extends BaseEntity {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "盐")
    private String salt;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "微信openid")
    private String wxOpenid;

    @Schema(description = "小程序openid")
    private String miniOpenid;

    @Schema(description = "QQ openid")
    private String qqOpenid;

}
