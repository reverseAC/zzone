package com.zjh.zzone.admin.service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjh.zzone.admin.api.dto.UserDTO;
import com.zjh.zzone.admin.service.service.SysUserService;
import com.zjh.zzone.common.core.base.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 *
 * @author zjh
 * @date 2025-08-13 14:16
 */
@RestController
@AllArgsConstructor
@RequestMapping("/user")
public class SysUserController {

    private SysUserService sysUserService;


    /**
     * 分页查询用户
     * @param page 参数集
     * @param userDTO 查询参数列表
     * @return 用户集合
     */
    @GetMapping("/page")
    public R getUserPage(Page page, UserDTO userDTO) {
        return R.ok(sysUserService.getUsersByPage(page, userDTO));
    }

    /**
     * 删除用户信息
     * @param ids 用户ID数组
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    public R userDel(@RequestBody Long[] ids) {
        return R.ok(sysUserService.deleteUserByIds(ids));
    }

    /**
     * 添加用户
     * @param userDto 用户信息DTO
     * @return 操作结果，成功返回success，失败返回false
     */
    @SysLog("添加用户")
    @PostMapping
    @HasPermission("sys_user_add")
    public R user(@RequestBody UserDTO userDto) {
        return R.ok(userService.saveUser(userDto));
    }

    /**
     * 更新用户信息
     * @param userDto 用户信息DTO对象
     * @return 包含操作结果的R对象
     * @throws javax.validation.Valid 参数校验失败时抛出异常
     */
    @SysLog("更新用户信息")
    @PutMapping
    @HasPermission("sys_user_edit")
    public R updateUser(@Valid @RequestBody UserDTO userDto) {
        return R.ok(userService.updateUser(userDto));
    }


}
