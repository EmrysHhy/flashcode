package com.bitejiuyeke.portalservice.flash.utils;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 把模型生成的代码文件写到本地磁盘。
 * 最终目录形如：当前工作目录/user-code/{appId}/index.html
 *
 * @author Emrys
 * content:
 */
@Slf4j
public class FileWriterUtil {

    private static final Set<String> SKIP_DIR_NAMES = Set.of(
            "node_modules", "dist", "target", ".git", ".idea");

    /**
     * 从已落盘的源码目录读回文本文件。构建产物不读。
     */
    public static Map<String, String> readSourceFiles(Path codePath) {
        return readSourceFiles(codePath == null ? null : codePath.toString());
    }

    /**
     * 从已落盘的源码目录读回文本文件。构建产物不读。
     * 图状态里的 FILES 经常丢（嵌套 Map 被展平或类型对不上），磁盘才是准的。
     */
    public static Map<String, String> readSourceFiles(String codePath) {
        Map<String, String> files = new LinkedHashMap<>();
        if (codePath == null || codePath.isBlank()) {
            return files;
        }
        Path root = Path.of(codePath).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            log.warn("源码目录不存在，无法读回文件: {}", root);
            return files;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(root) && SKIP_DIR_NAMES.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!isCodeFile(file) || attrs.size() > 512 * 1024) {
                        return FileVisitResult.CONTINUE;
                    }
                    String relative = root.relativize(file).toString().replace('\\', '/');
                    try {
                        files.put(relative, Files.readString(file, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.warn("跳过无法按文本读取的文件: {}", relative);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("读取源码失败, dir={}", root, e);
            throw new ServiceException("读取本地源码失败");
        }
        log.info("从磁盘读回源码, dir={}, fileCount={}", root, files.size());
        return files;
    }

    private static boolean isCodeFile(Path path) {
        Path name = path.getFileName();
        if (name == null) {
            return false;
        }
        String fileName = name.toString().toLowerCase();
        return fileName.endsWith(".html")
                || fileName.endsWith(".vue")
                || fileName.endsWith(".js")
                || fileName.endsWith(".ts")
                || fileName.endsWith(".css")
                || fileName.endsWith(".java")
                || fileName.endsWith(".json")
                || fileName.endsWith(".xml");
    }

    /**
     * 把 files 里的每个文件写到 user-code/{appId}/ 下面。
     *
     * @param appId 应用 ID，用作这一层文件夹的名字
     * @param files key = 相对路径（如 index.html、src/App.vue），value = 文件内容
     * @return 应用根目录的绝对路径，例如 .../user-code/123
     */
    public static Path saveCode(Long appId, Map<String, String> files) {
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }

        // 拼出应用根目录：user-code/123
        // toAbsolutePath：相对路径转成绝对路径（前面会加上进程启动目录）
        // normalize：把路径里的 . 和 .. 整理干净
        Path appDir = Paths.get(FlashcodeConstant.USER_CODE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        try {

            if (files == null || files.isEmpty()) {
                return appDir;
            }
            // 同一 appId 重新生成时，先清空旧目录，避免上次留下的启动类、node_modules 混进来
            if (Files.exists(appDir)) {
                deleteDirectory(appDir);
            }
            Files.createDirectories(appDir);

            for (Map.Entry<String, String> entry : files.entrySet()) {
                // 把 "src/App.vue" 拼到 appDir 后面，并检查不能写出 user-code/123 之外
                Path target = resolveSafePath(appDir, entry.getKey());
                if (target == null) {
                    continue;
                }
                // 例如目标是 .../src/App.vue，需要先确保 src 文件夹存在
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                String content = entry.getValue() == null ? "" : entry.getValue();
                // 有则覆盖，没有则新建
                Files.writeString(target, content, StandardCharsets.UTF_8);
            }
            log.info("应用代码写入完成, appId={}, fileCount={}, dir={}", appId, files.size(), appDir);
            return appDir;
        } catch (IOException e) {
            log.error("写入应用代码失败, appId={}", appId, e);
            throw new ServiceException("写入本地文件失败");
        }
    }

    /**
     * 把 user-code/{appId} 下的单个 HTML 复制到 user-preview/{appId}。
     * 容器工作目录是 /workspace 时，目标即为 /workspace/user-preview/{appId}。
     *
     * @return 预览目录的绝对路径
     */
    public static Path copyHtmlToPreview(Path loadedCode, Long appId) {
        Path workAppDir = resolvePreviewDir(loadedCode, appId);
        try {
            Files.createDirectories(workAppDir);//创建工作appid目录

            Path source = loadedCode.resolve("index.html");
            Path target = workAppDir.resolve("dist/index.html");
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            log.info("HTML 已复制到预览目录, src={}, dest={}", source, target);
            return workAppDir;
        } catch (IOException e) {
            log.error("复制 HTML 到预览目录失败, appId={}", appId, e);
            throw new ServiceException("复制预览文件失败");
        }
    }

    /**
     * 把 user-code/{appId} 下的 dist 目录复制到 user-preview/{appId}。
     * 容器工作目录是 /workspace 时，目标即为 /workspace/user-preview/{appId}。
     *
     * @param frontDir
     * @param appId
     */
    public static void copyDistPreview(Path frontDir, Long appId) {
        Path workAppDir = resolvePreviewDir(frontDir, appId); //校验并且生成绝对路径
        //校验dist目录
        Path distDir = frontDir.resolve("dist");
        if (!Files.isDirectory(distDir)) {
            throw new ServiceException("dist目录不存在");
        }
        try {
            copyDirectory(distDir, workAppDir.resolve("dist"));
            log.info("dist 已复制到预览目录, src={}, dest={}", distDir, workAppDir);
        } catch (IOException e) {
            log.error("复制 dist 到预览目录失败, appId={}", appId, e);
            throw new ServiceException("复制预览文件失败");
        }
    }

    /**
     * 发布容器的 nginx 读取 user-deploy/{appId}/dist。
     * 构建产物在 user-preview/{appId}/dist，发布时再复制过去。
     */
    public static void copyPreviewDistToDeploy(Long appId, Path deployDir) {
        if (appId == null || deployDir == null) {
            throw new ServiceException("发布目录不能为空");
        }
        Path source = Paths.get(FlashcodeConstant.USER_PREVIEW_DIR, String.valueOf(appId), "dist")
                .toAbsolutePath()
                .normalize();
        Path target = deployDir.toAbsolutePath().normalize().resolve("dist");
        if (!Files.isDirectory(source)) {
            throw new ServiceException("预览产物不存在，无法发布");
        }
        try {
            copyDirectory(source, target);
            log.info("dist 已复制到发布目录, src={}, dest={}", source, target);
        } catch (IOException e) {
            log.error("复制 dist 到发布目录失败, appId={}", appId, e);
            throw new ServiceException("复制发布文件失败");
        }
    }

    /**
     * 把预览目录里的 jar 复制到发布目录，供发布容器启动后端。
     * 纯前端应用没有 jar，返回 empty。
     */
    public static java.util.Optional<Path> copyPreviewJarToDeploy(Long appId, Path deployDir) {
        if (appId == null || deployDir == null) {
            throw new ServiceException("发布目录不能为空");
        }
        Path previewDir = Paths.get(FlashcodeConstant.USER_PREVIEW_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(previewDir)) {
            return java.util.Optional.empty();
        }
        Path jarFile;
        try {
            jarFile = findSingleJar(previewDir);
        } catch (ServiceException e) {
            return java.util.Optional.empty();
        } catch (IOException e) {
            throw new ServiceException("复制发布 jar 失败");
        }
        Path target = deployDir.toAbsolutePath().normalize().resolve(jarFile.getFileName());
        try {
            Files.createDirectories(target.getParent());
            Files.copy(jarFile, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("jar 已复制到发布目录, src={}, dest={}", jarFile, target);
            return java.util.Optional.of(target);
        } catch (IOException e) {
            log.error("复制 jar 到发布目录失败, appId={}", appId, e);
            throw new ServiceException("复制发布 jar 失败");
        }
    }

    /**
     * 把 source 目录里的内容拷到 target。
     * user-code 和 user-preview 往往不在同一块盘/挂载上，Files.move 会失败，所以按文件树复制。
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        if (source == null || !Files.isDirectory(source)) {
            throw new ServiceException("源目录不存在");
        }
        Files.createDirectories(target);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path dest = target.resolve(source.relativize(file));
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    /**
     * 把用户上传的参考文件写到 user-reference/{appId}/。同一 appId 再次上传时覆盖该目录。
     */
    public static Path writeReferenceFile(Long appId, MultipartFile file) {
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("参考文件不能为空");
        }
        String safeName = sanitizeFileName(file.getOriginalFilename());
        Path refRoot = Paths.get(FlashcodeConstant.USER_REFERENCE_DIR).toAbsolutePath().normalize();
        Path appDir = Paths.get(FlashcodeConstant.USER_REFERENCE_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!appDir.startsWith(refRoot) || appDir.equals(refRoot)) {
            throw new ServiceException("非法参考文件目录");
        }
        try {
            if (Files.exists(appDir)) {
                deleteDirectory(appDir);
            }
            Files.createDirectories(appDir);
            Path target = resolveSafePath(appDir, safeName);
            if (target == null) {
                throw new ServiceException("非法参考文件名");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("参考文件已写入, appId={}, file={}", appId, target);
            return target;
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            log.error("写入参考文件失败, appId={}", appId, e);
            throw new ServiceException("写入参考文件失败");
        }
    }

    /**
     * 删除 user-code/{appId} 下的本地源码（含 npm/maven 构建产物）。
     * 目录不存在时直接返回。不删除 user-preview，预览仍可访问。
     */
    public static void deleteCodeByAppId(Long appId) {
        deleteAppScopedDir(FlashcodeConstant.USER_CODE_DIR, appId, "本地代码");
    }

    /**
     * 删除 user-reference/{appId} 下的参考文件。
     */
    public static void deleteReferenceByAppId(Long appId) {
        deleteAppScopedDir(FlashcodeConstant.USER_REFERENCE_DIR, appId, "参考文件");
    }

    private static void deleteAppScopedDir(String rootDirName, Long appId, String label) {
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        Path root = Paths.get(rootDirName).toAbsolutePath().normalize();
        Path appDir = Paths.get(rootDirName, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
        if (!appDir.startsWith(root) || appDir.equals(root)) {
            throw new ServiceException("非法" + label + "目录");
        }
        if (!Files.exists(appDir)) {
            log.info("{}目录不存在，跳过删除, appId={}, dir={}", label, appId, appDir);
            return;
        }
        try {
            deleteDirectory(appDir);
            log.info("{}已删除, appId={}, dir={}", label, appId, appDir);
        } catch (IOException e) {
            log.error("删除{}失败, appId={}, dir={}", label, appId, appDir, e);
            throw new ServiceException("删除" + label + "失败");
        }
    }

    private static String sanitizeFileName(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename.trim();
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new ServiceException("参考文件名非法");
        }
        return name;
    }

    /**
     * 删除目录及其所有子文件和子目录。
     * @param directory
     * @throws IOException
     */
    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 把 user-code/{appId} 下的 jar 包复制到 user-preview/{appId}。
     * 容器工作目录是 /workspace 时，目标即为 /workspace/user-preview/{appId}。
     *
     * @param backDir
     * @param appId
     */
    public static Path copyJarPreview(Path backDir, Long appId) {
        Path workAppDir = resolvePreviewDir(backDir, appId);
        Path mavenTargetDir = backDir.resolve("target");
        if (!Files.isDirectory(mavenTargetDir)) {
            throw new ServiceException("target目录不存在");
        }
        try {
            Files.createDirectories(workAppDir);
            Path jarFile = findSingleJar(mavenTargetDir);
            Path target = workAppDir.resolve(jarFile.getFileName());
            Files.copy(jarFile, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("jar 已复制到预览目录, src={}, dest={}", jarFile, target);
            return target;
        } catch (IOException e) {
            log.error("复制 jar 到预览目录失败, appId={}", appId, e);
            throw new ServiceException("复制预览文件失败");
        }

    }

    /**
     * 在 maven 的 target 目录中查找唯一的 jar 文件。
     */
    private static Path findSingleJar(Path mavenTargetDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mavenTargetDir, "*.jar")) {
            for (Path jarFile : stream) {
                return jarFile;
            }
        }
        throw new ServiceException("未找到 jar 文件");
    }

    /**
     * 校验应用 ID 和本地代码目录，并拼出 user-preview/{appId} 的绝对路径。
     */
    private static Path resolvePreviewDir(Path sourceDir, Long appId) {
        if (appId == null) {
            throw new ServiceException("应用ID不能为空");
        }
        if (sourceDir == null || !Files.isDirectory(sourceDir)) {
            throw new ServiceException("本地代码目录不存在");
        }
        return Paths.get(FlashcodeConstant.USER_PREVIEW_DIR, String.valueOf(appId))
                .toAbsolutePath()
                .normalize();
    }


    /**
     * 把模型给的相对路径，安全地拼到应用目录后面。
     *
     * 例如 appDir = /workspace/user-code/123，relativePath = src/App.vue
     * 结果 = /workspace/user-code/123/src/App.vue
     *
     * 如果路径想逃出应用目录（如 ../../etc/passwd），返回 null，不写入。
     */
    private static Path resolveSafePath(Path appDir, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        // Windows 的 \ 统一改成 /，方便处理
        String normalized = relativePath.trim().replace('\\', '/');
        // 去掉开头的 /，避免被当成绝对路径
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        // resolve：在 appDir 后面拼接相对路径
        // normalize：把 ../ 展开，例如 a/../b 会变成 b
        Path target = appDir.resolve(normalized).normalize();
        // 展开后如果已经跑到 appDir 外面，或根本不是一个文件路径，就拒绝写入
        if (!target.startsWith(appDir) || target.equals(appDir)) {
            log.warn("非法文件路径，已跳过: {}", relativePath);
            return null;
        }
        return target;
    }



}
