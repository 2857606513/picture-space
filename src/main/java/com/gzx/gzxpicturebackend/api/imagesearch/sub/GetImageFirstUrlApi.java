package com.gzx.gzxpicturebackend.api.imagesearch.sub;

import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取图片列表接口的 Api（Step 2）
 * 用于从百度图片搜索结果页面中提取 firstUrl
 */
@Slf4j
public class GetImageFirstUrlApi {

    private static final int TIMEOUT = 10000; // 10秒超时
    private static final Pattern FIRST_URL_PATTERN = Pattern.compile("\"firstUrl\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * 获取图片列表页面地址
     *
     * @param url 目标网页URL
     * @return 提取到的firstUrl
     */
    public static String getImageFirstUrl(String url) {
        // 基本参数验证
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL不能为空");
        }

        try {
            log.info("开始获取图片列表页面地址，URL: {}", url);
            
            // 使用 Jsoup 获取 HTML 内容
            Document document = Jsoup.connect(url)
                    .timeout(TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 获取所有 <script> 标签
            Elements scriptElements = document.getElementsByTag("script");
            log.debug("找到 {} 个script标签", scriptElements.size());

            // 遍历找到包含 `firstUrl` 的脚本内容
            for (Element script : scriptElements) {
                String scriptContent = script.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    log.debug("找到包含firstUrl的script标签");
                    String firstUrl = extractFirstUrl(scriptContent);
                    if (firstUrl != null && !firstUrl.trim().isEmpty()) {
                        log.info("成功提取firstUrl: {}", firstUrl);
                        return firstUrl;
                    }
                }
            }

            log.warn("未在页面中找到firstUrl，URL: {}", url);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "页面中未找到firstUrl");
            
        } catch (BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("获取图片列表页面地址失败，URL: {}", url, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表页面地址失败: " + e.getMessage());
        }
    }

    /**
     * 从脚本内容中提取firstUrl
     */
    private static String extractFirstUrl(String scriptContent) {
        Matcher matcher = FIRST_URL_PATTERN.matcher(scriptContent);
        if (matcher.find()) {
            String firstUrl = matcher.group(1);
            // 处理转义字符
            return firstUrl.replace("\\/", "/");
        }
        return null;
    }

    public static void main(String[] args) {
        // 请求目标 URL
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=16250747570487381669&sign=1265ce97cd54acd88139901733452612&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println("搜索成功，结果 URL：" + imageFirstUrl);
    }
}
