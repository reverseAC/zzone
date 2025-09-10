package com.zjh.zzone.common.core.util;

import cn.hutool.core.util.HexUtil;
import java.nio.charset.StandardCharsets;

/**
 * 字符串处理工具
 *
 * @author zjh
 * @date 2025-09-10 17:29
 */
public class StringUtil {

    /**
     * 将字节数组转换为字符串，自动处理UTF-8和十六进制
     */
    public static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        // 尝试用 UTF-8 解码
        String utf8Str = new String(bytes, StandardCharsets.UTF_8);
        if (isMostlyPrintable(utf8Str)) {
            return utf8Str;
        }

        // 否则，将字节数组转换为十六进制字符串
        return HexUtil.encodeHexStr(bytes);
    }

    /**
     * 判断字符串是否大部分是可打印字符
     * @param s 字符串
     */
    private static boolean isMostlyPrintable(String s) {
        int printable = 0;
        for (char c : s.toCharArray()) {
            if (c >= 32 && c <= 126) { // ASCII 可打印字符
                printable++;
            }
        }
        return printable >= s.length() * 0.8; // 超过80%是可打印字符则认为是字符串
    }

}
