package com.zjh.zzone.common.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MybatisPlus自动填充处理器，公共字段填充
 *
 * @author zjh
 * @date 2025/7/8 21:49
 */
@Slf4j
public class MetaObjectAutoFillHandler implements MetaObjectHandler {

	/**
	 * 插入时自动填充字段
	 * @param metaObject 元对象，用于操作实体类属性
	 */
	@Override
	public void insertFill(MetaObject metaObject) {
		log.debug("mybatis plus start insert fill ....");
		this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
		this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
		this.strictInsertFill(metaObject, "createBy", this::getUserName, String.class);
		this.strictInsertFill(metaObject, "createBy", this::getUserName, String.class);
	}

	/**
	 * 更新时自动填充字段
	 * @param metaObject 元对象
	 */
	@Override
	public void updateFill(MetaObject metaObject) {
		log.debug("mybatis plus start update fill ....");
		this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
		this.strictInsertFill(metaObject, "createBy", this::getUserName, String.class);
	}

	/**
	 * 获取 spring security 当前的用户名
	 * @return 当前用户名
	 */
	private String getUserName() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		// 匿名接口直接返回
		if (authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}

		if (Optional.ofNullable(authentication).isPresent()) {
			return authentication.getName();
		}

		return null;
	}

}
