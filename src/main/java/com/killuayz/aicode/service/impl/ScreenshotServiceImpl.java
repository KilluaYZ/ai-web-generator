package com.killuayz.aicode.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.killuayz.aicode.manager.CosManager;
import com.killuayz.aicode.service.ScreenshotService;
import com.killuayz.aicode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 参数校验
        if (StrUtil.isBlank(webUrl)) {
            log.warn("截图的网址为空");
            return null;
        }
        log.info("开始生成网页截图，URL：{}", webUrl);
        String localScreenshotPath = null;
        try {
            // 本地截图（可能因目标不可达、连接被拒等失败）
            localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
            if (StrUtil.isBlank(localScreenshotPath)) {
                log.warn("生成网页截图失败，URL：{}", webUrl);
                return null;
            }
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            if (StrUtil.isBlank(cosUrl)) {
                log.warn("上传截图到对象存储失败，URL：{}", webUrl);
                return null;    
            }
            log.info("截图上传成功，URL：{}", cosUrl);
            return cosUrl;
        } catch (Exception e) {
            log.warn("生成或上传截图异常，URL：{}，错误：{}", webUrl, e.getMessage(), e);
            return null;
        } finally {
            if (StrUtil.isNotBlank(localScreenshotPath)) {
                cleanupLocalFile(localScreenshotPath);
            }
        }
    }

    /**
     * 上传截图到对象存储
     *
     * @param localScreenshotPath 本地截图路径
     * @return 对象存储访问URL，失败返回null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图的对象存储键
     * 格式：/screenshots/2025/07/31/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本地文件
     *
     * @param localFilePath 本地文件路径
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            FileUtil.del(localFile);
            log.info("清理本地文件成功: {}", localFilePath);
        }
    }
}
