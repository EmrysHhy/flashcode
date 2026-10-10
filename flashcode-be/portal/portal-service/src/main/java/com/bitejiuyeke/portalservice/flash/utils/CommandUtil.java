package com.bitejiuyeke.portalservice.flash.utils;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 在指定目录执行本地命令，以及用 Docker 启动生成的 jar。
 */
@Slf4j
public class CommandUtil {

    /**
     * 在 workDir 下执行一条命令（如 npm install、mvn clean package）。
     */
    public static void runCommand(String command, Path workDir) {
        if (workDir == null || !Files.isDirectory(workDir)) {
            throw new ServiceException("命令工作目录不存在: " + workDir);
        }
        log.info("执行命令: {}, dir={}", command, workDir.toAbsolutePath());
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder processBuilder = windows
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("sh", "-c", command);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("npm_config_registry", FlashcodeConstant.NPM_REGISTRY);
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                    log.debug("[cmd] {}", line);
                }
            }
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new ServiceException("命令执行超时: " + command);
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("命令失败, cmd={}, code={}, out=\n{}", command, exitCode, output);
                throw new ServiceException(buildCommandFailureMessage(command, workDir, exitCode, output.toString()));
            }
            log.info("命令完成: {}", command);
        } catch (ServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("命令执行被中断: " + command);
        } catch (Exception e) {
            log.error("命令执行异常: {}", command, e);
            throw new ServiceException("命令执行失败: " + command + "，" + e.getMessage());
        }
    }

    private static String buildCommandFailureMessage(String command, Path workDir, int exitCode, String output) {
        return String.format(
                "命令执行失败: %s%n工作目录: %s%n退出码: %d%n输出:%n%s",
                command,
                workDir.toAbsolutePath(),
                exitCode,
                truncateOutput(output, 8000)
        );
    }

    private static String truncateOutput(String output, int maxLength) {
        if (output == null || output.length() <= maxLength) {
            return output == null ? "" : output;
        }
        // 编译错误多在末尾
        return "... (省略前 " + (output.length() - maxLength) + " 字符) ...\n"
                + output.substring(output.length() - maxLength);
    }

    /**
     * 在已有预览容器（如 flashcode-userapp-preview）里启动 jar。
     * 容器内路径：/workspace/user-preview/{appId}/{jar}，端口由 appId 算出。
     *
     * @param jarOrDir copyJarPreview 返回的 jar 文件，或包含 jar 的预览目录
     */
    public static void runJar(DockerClient dockerClient, Path jarOrDir, Long appId, String containerName) {
        if (dockerClient == null) {
            throw new ServiceException("DockerClient 未初始化");
        }
        if (containerName == null || containerName.isBlank()) {
            throw new ServiceException("预览容器名不能为空");
        }
        Path jarFile = resolveJarFile(jarOrDir);
        int port = generatePort(appId);
        String jarName = jarFile.getFileName().toString();
        String jarInContainer = "/workspace/" + FlashcodeConstant.USER_PREVIEW_DIR + "/" + appId + "/" + jarName;
        String pidFile = "/tmp/flashcode-" + appId + ".pid";
        String logFile = "/workspace/" + FlashcodeConstant.USER_PREVIEW_DIR + "/" + appId + "/app.log";
        String startScript = String.join(" ",
                "kill $(cat " + pidFile + ") 2>/dev/null || true;",
                "nohup java -jar " + jarInContainer,
                "--server.port=" + port,
                ">" + logFile, "2>&1", "</dev/null &",
                "echo $! > " + pidFile
        );

        ensureContainerRunning(dockerClient, containerName);
        execInContainer(dockerClient, containerName, "启动 jar", "bash", "-c", startScript);
        log.info("jar 已在容器中启动, appId={}, container={}, port={}, jar={}",
                appId, containerName, port, jarInContainer);
        updateNginxConfig(dockerClient, containerName, appId, port);
    }

    /**
     * 在该应用自己的发布容器里启动 jar。容器内固定 8080，由发布 nginx 把 /{appId}/api 转过来。
     */
    public static void runDeployJar(DockerClient dockerClient, String containerName, Long appId, String jarName) {
        if (dockerClient == null) {
            throw new ServiceException("DockerClient 未初始化");
        }
        if (containerName == null || containerName.isBlank() || jarName == null || jarName.isBlank()) {
            throw new ServiceException("发布容器或 jar 不能为空");
        }
        String jarInContainer = "/workspace/user-deploy/" + appId + "/" + jarName;
        String pidFile = "/tmp/flashcode-deploy-" + appId + ".pid";
        String logFile = "/workspace/user-deploy/" + appId + "/app.log";
        String startScript = String.join(" ",
                "kill $(cat " + pidFile + ") 2>/dev/null || true;",
                "nohup java -jar " + jarInContainer,
                "--server.port=8080",
                ">" + logFile, "2>&1", "</dev/null &",
                "echo $! > " + pidFile
        );
        ensureContainerRunning(dockerClient, containerName);
        execInContainer(dockerClient, containerName, "启动发布 jar", "bash", "-c", startScript);
        execInContainer(dockerClient, containerName, "重载发布 nginx", "nginx", "-s", "reload");
        log.info("发布 jar 已启动, appId={}, container={}, jar={}", appId, containerName, jarInContainer);
    }

    /**
     * 按 appId 算出固定端口，范围 8001-9999。
     * 预览时这是共享容器里的进程端口；发布时这是宿主机映射端口。
     * 同一个 appId 结果不变，不同应用不会都占用 8080。
     */
    public static int generatePort(Long appId) {
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        int port = FlashcodeConstant.JAR_HOST_PORT_BASE
                + (int) (Math.floorMod(appId, FlashcodeConstant.JAR_HOST_PORT_RANGE));
        log.info("为 appId {} 分配端口: {}", appId, port);
        return port;
    }

    private static void updateNginxConfig(DockerClient dockerClient, String containerName, Long appId, int port) {
        String script = FlashcodeConstant.NGINX_UPDATE_SCRIPT;
        // Windows 检出的脚本带 CRLF 时，bash 会把 \r 当成命令，必须先剥掉
        execInContainer(dockerClient, containerName, "更新 nginx 配置",
                "bash", "-c",
                "sed -i 's/\\r$//' " + script + " && bash " + script + " " + appId + " " + port);
        execInContainer(dockerClient, containerName, "重载 nginx", "nginx", "-s", "reload");
        log.info("nginx 配置已更新并重载, appId={}, port={}", appId, port);
    }

    private static void ensureContainerRunning(DockerClient dockerClient, String containerName) {
        try {
            var inspect = dockerClient.inspectContainerCmd(containerName).exec();
            if (Boolean.FALSE.equals(inspect.getState().getRunning())) {
                dockerClient.startContainerCmd(containerName).exec();
                log.info("预览容器未运行，已启动: {}", containerName);
            }
        } catch (NotFoundException e) {
            throw new ServiceException("预览容器不存在: " + containerName);
        }
    }

    /**
     * 在指定容器中执行命令，等待结束并校验退出码。
     */
    private static void execInContainer(DockerClient dockerClient, String containerName,
                                        String actionDesc, String... cmd) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        String execId = dockerClient.execCreateCmd(containerName)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(cmd)
                .exec()
                .getId();
        try {
            dockerClient.execStartCmd(execId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            byte[] payload = frame.getPayload();
                            if (payload == null || payload.length == 0) {
                                return;
                            }
                            if (frame.getStreamType() == StreamType.STDERR) {
                                stderr.write(payload, 0, payload.length);
                            } else {
                                stdout.write(payload, 0, payload.length);
                            }
                        }
                    })
                    .awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(actionDesc + "被中断");
        }

        InspectExecResponse inspect = dockerClient.inspectExecCmd(execId).exec();
        Long exitCode = inspect.getExitCodeLong();
        if (exitCode == null || exitCode != 0) {
            String errorOutput = stderr.toString(StandardCharsets.UTF_8);
            throw new ServiceException(actionDesc + "失败，退出码=" + exitCode + "，错误输出: " + errorOutput);
        }
        if (stdout.size() > 0) {
            log.info("{} 成功，输出: {}", actionDesc, stdout.toString(StandardCharsets.UTF_8).trim());
        } else {
            log.info("{} 成功", actionDesc);
        }
    }

    /**
     * copyJarPreview 返回 jar 文件路径；也兼容传入预览目录再扫描 *.jar。
     */
    private static Path resolveJarFile(Path jarOrDir) {
        if (jarOrDir == null) {
            throw new ServiceException("预览 jar 路径为空");
        }
        if (Files.isRegularFile(jarOrDir) && jarOrDir.getFileName().toString().endsWith(".jar")) {
            return jarOrDir;
        }
        if (!Files.isDirectory(jarOrDir)) {
            throw new ServiceException("预览目录不存在: " + jarOrDir);
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(jarOrDir, "*.jar")) {
            for (Path jarFile : stream) {
                return jarFile;
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("查找 jar 失败");
        }
        throw new ServiceException("预览目录中未找到 jar 文件");
    }
}
