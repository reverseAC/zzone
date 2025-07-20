package com.zjh.zzone.common.core.base;

import com.zjh.zzone.common.core.enums.ResultCodeEnum;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @param <T>
 * @author lengleng
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class R<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private int code;

	private String msg;

	private T data;

	public static <T> R<T> ok() {
		return restResult(null, ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage());
	}

	public static <T> R<T> ok(T data) {
		return restResult(data, ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage());
	}

	public static <T> R<T> ok(T data, String msg) {
		return restResult(data, ResultCodeEnum.SUCCESS.getCode(), msg);
	}

	public static <T> R<T> fail() {
		return restResult(null, ResultCodeEnum.BUSINESS_ERROR.getCode(), ResultCodeEnum.BUSINESS_ERROR.getMessage());
	}

	public static <T> R<T> fail(T data) {
		return restResult(data, ResultCodeEnum.BUSINESS_ERROR.getCode(), ResultCodeEnum.BUSINESS_ERROR.getMessage());
	}

	public static <T> R<T> fail(String msg) {
		return restResult(null, ResultCodeEnum.BUSINESS_ERROR.getCode(), msg);
	}

	public static <T> R<T> fail(T data, String msg) {
		return restResult(data, ResultCodeEnum.BUSINESS_ERROR.getCode(), msg);
	}

	public static <T> R<T> restResult(T data, int code, String msg) {
		R<T> apiResult = new R<>();
		apiResult.setCode(code);
		apiResult.setData(data);
		apiResult.setMsg(msg);
		return apiResult;
	}

}
