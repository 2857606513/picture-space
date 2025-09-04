package com.gzx.gzxpicturebackend.utils;

/**
 * 颜色转换工具类
 */
public class ColorTransformUtils {
    private ColorTransformUtils() {
    }
        public static String getStandardColor(String rawColor){
            if (rawColor == null || !rawColor.startsWith("0x"))
            {
                throw new IllegalArgumentException("必须以0x开头的十六进制字符串");
            }
            String hex = rawColor.substring(2).toLowerCase();
            int length = hex.length();
            String r= "00", g= "00", b= "00";


        if(length == 6){
            return "0x" + hex;
        }
        if (length == 5) {
            r = hex.substring(0, 2);
            if (r.startsWith("0")) {
                r = "00";
                g = hex.substring(1, 3);
                b = hex.substring(3, 5);
            }else {
                g = hex.substring(2, 4);
                if (g.startsWith("0")) {
                    g = "00";
                    b = hex.substring(3, 5);
                } else{
                    b = "0" + hex.substring(length - 1);
                }
            }
        }
        if (length == 4) {
            r = hex.substring(0, 2);
            if (r.startsWith("0")) {
                r = "00";
                g = hex.substring(1, 3);
                if (g.startsWith("0")) {
                    g = "00";
                    b = hex.substring(2, 4);
                } else {
                    b = "0" + hex.substring(length - 1);
                }
            }else {
                g = "0" + hex.substring(2,3);
                b = "0" + hex.substring(length - 1);
            }
        }
        if (length == 3) {
            r = "0" + hex.charAt(0);
            g = "0" + hex.charAt(1);
            b = "0" + hex.charAt(2);
        }
        return "0x" + r + g + b;
    }
}
