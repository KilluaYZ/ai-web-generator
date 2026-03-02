package com.killuayz.aicode.core.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * 构建 Vue 项目
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 异步构建 Vue 项目
     *
     * @param projectPath
     */
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildProject(projectPath);
                    } catch (Exception e) {
                        log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在：{}", projectPath);
            return false;
        }
        // 检查是否有 package.json 文件
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.error("项目目录中没有 package.json 文件：{}", projectPath);
            return false;
        }
        log.info("开始构建 Vue 项目：{}", projectPath);
        // 执行 npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 执行失败：{}", projectPath);
            return false;
        }
        // 修正 index.html 中错误的入口路径（/main.js -> /src/main.js），避免 Vite 构建报错
        fixIndexHtmlEntryPath(projectDir);
        // 执行 npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 执行失败：{}", projectPath);
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            log.error("构建完成但 dist 目录未生成：{}", projectPath);
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录：{}", projectPath);
        return true;
    }

    /**
     * 修正 index.html 中错误的脚本入口路径。
     * 若引用的是 /main.js，Vite 会按绝对路径解析导致构建失败，需改为 /src/main.js。
     */
    private void fixIndexHtmlEntryPath(File projectDir) {
        File indexFile = new File(projectDir, "index.html");
        if (!indexFile.isFile()) {
            return;
        }
        try {
            String content = Files.readString(indexFile.toPath(), StandardCharsets.UTF_8);
            String original = content;
            // 常见错误写法："/main.js" 或 '/main.js'，改为 /src/main.js
            content = content.replace("src=\"/main.js\"", "src=\"/src/main.js\"")
                    .replace("src='/main.js'", "src=\"/src/main.js\"")
                    .replace("\"/main.js\"", "\"/src/main.js\"")
                    .replace("'/main.js'", "\"/src/main.js\"");
            if (!content.equals(original)) {
                Files.writeString(indexFile.toPath(), content, StandardCharsets.UTF_8);
                log.info("已修正 index.html 中的入口路径：/main.js -> /src/main.js");
            }
        } catch (Exception e) {
            log.warn("修正 index.html 入口路径时出错: {}", e.getMessage());
        }
    }

    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }

    /**
     * 根据操作系统构造命令
     *
     * @param baseCommand
     * @return
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 操作系统检测
     *
     * @return
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 执行命令，失败时记录进程的 stdout/stderr 便于排查
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        Process process = null;
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            String[] cmdArray = isWindows()
                    ? new String[]{"cmd", "/c", command}
                    : command.split("\\s+");
            ProcessBuilder pb = new ProcessBuilder(cmdArray)
                    .directory(workingDir)
                    .redirectErrorStream(false);
            process = pb.start();
            final Process process_final = process;
            // 异步读取 stdout/stderr，避免缓冲区满导致进程阻塞
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Charset charset = Charset.defaultCharset();
            Thread outReader = new Thread(() -> readStream(process_final.getInputStream(), stdout, charset), "npm-stdout");
            Thread errReader = new Thread(() -> readStream(process_final.getErrorStream(), stderr, charset), "npm-stderr");
            outReader.start();
            errReader.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            outReader.join(5000);
            errReader.join(5000);

            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            }
            String out = stdout.toString().trim();
            String err = stderr.toString().trim();
            log.error("命令执行失败，退出码: {}", exitCode);
            if (!out.isEmpty()) {
                log.error("命令 stdout: {}", out);
            }
            if (!err.isEmpty()) {
                log.error("命令 stderr: {}", err);
            }
            if (out.isEmpty() && err.isEmpty()) {
                log.error("未捕获到输出，请在该项目目录下手动执行相同命令查看具体错误");
            }
            return false;
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static void readStream(java.io.InputStream is, StringBuilder sb, Charset charset) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        } catch (Exception e) {
            // 忽略读取时的异常
        }
    }

}
