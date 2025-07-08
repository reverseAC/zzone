package com.zjh.zzone.common.mybatis.resolver;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlInjectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mybatis Plus Order By SQL注入问题解决类
 *
 * @author zjh
 * @date 2025/07/08 22:57
 */
@Slf4j
public class SqlFilterArgumentResolver implements HandlerMethodArgumentResolver {

	/**
	 * 判断Controller方法参数是否为Page类型
	 * @param parameter 方法参数
	 * @return 如果参数类型是Page则返回true，否则返回false
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType().equals(Page.class);
	}

	/**
	 * 解析分页参数并构建Page对象
	 * @param parameter 方法参数信息
	 * @param mavContainer 模型和视图容器
	 * @param webRequest web请求对象
	 * @param binderFactory 数据绑定工厂
	 * @return 包含分页和排序信息的Page对象
	 * @throws NumberFormatException 当分页参数转换失败时抛出
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

		String[] ascs = request.getParameterValues("ascs");
		String[] descs = request.getParameterValues("descs");
		String current = request.getParameter("current");
		String size = request.getParameter("size");

		Page<?> page = new Page<>();
		if (StrUtil.isNotBlank(current)) {
			page.setCurrent(Long.parseLong(current));
		}

		if (StrUtil.isNotBlank(size)) {
			page.setSize(Long.parseLong(size));
		}

		List<OrderItem> orderItemList = new ArrayList<>();
		Optional.ofNullable(ascs)
			.ifPresent(s -> orderItemList.addAll(Arrays.stream(s)
				.filter(asc -> !SqlInjectionUtils.check(asc))
				.map(OrderItem::asc)
				.collect(Collectors.toList())));
		Optional.ofNullable(descs)
			.ifPresent(s -> orderItemList.addAll(Arrays.stream(s)
				.filter(desc -> !SqlInjectionUtils.check(desc))
				.map(OrderItem::desc)
				.collect(Collectors.toList())));
		page.addOrder(orderItemList);

		return page;
	}

}
