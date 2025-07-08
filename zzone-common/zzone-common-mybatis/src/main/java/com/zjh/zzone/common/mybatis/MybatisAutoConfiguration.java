package com.zjh.zzone.common.mybatis;

import com.zjh.zzone.common.mybatis.config.MetaObjectAutoFillHandler;
import com.zjh.zzone.common.mybatis.resolver.SqlFilterArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MyBatis Plus 统一自动配置类
 * <p>
 * 提供SQL过滤器、分页插件及审计字段自动填充等配置
 *
 * @author lengleng
 * @date 2025/05/31
 */
@Configuration(proxyBeanMethods = false)
public class MybatisAutoConfiguration implements WebMvcConfigurer {

	/**
	 * 添加SQL过滤器参数解析器，避免SQL注入
	 * @param argumentResolvers 方法参数解析器列表
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(new SqlFilterArgumentResolver());
	}

	/**
	 * 创建并返回MybatisPlusMetaObjectHandler实例，用于审计字段自动填充
	 * @return MybatisPlusMetaObjectHandler实例
	 */
	@Bean
	public MetaObjectAutoFillHandler metaObjectAutoFillHandler() {
		return new MetaObjectAutoFillHandler();
	}

}
