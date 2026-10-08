package com.bitejiuyeke.portalservice.flash.utils;

import com.bitejiuyeke.portalservice.flash.constants.FlashcodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * 有参考图时切到 VL 模型，并把图片挂到用户消息上。
 * 参考图必须落在 user-reference 目录内，避免把任意本地路径塞给模型。
 *
 * @author Emrys
 */
@Slf4j
public final class VisionChatSupport {

    /** 允许作为参考图的后缀，其它文件不传给 VL 模型。 */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    private VisionChatSupport() {
    }

    /**
     * 校验并解析参考图路径。
     * 空路径、不在 user-reference 下、不是普通文件、或后缀不是图片时返回 null。
     *
     * @param pathStr 工作流里保存的参考图路径
     * @return 规范化后的绝对路径；无效时为 null
     */
    public static Path resolveImage(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            return null;
        }
        Path root = Path.of(FlashcodeConstant.USER_REFERENCE_DIR).toAbsolutePath().normalize();
        Path path = Path.of(pathStr).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            log.warn("参考图路径无效: {}", pathStr);
            return null;
        }
        if (!IMAGE_EXTENSIONS.contains(extension(path))) {
            return null;
        }
        return path;
    }

    /**
     * 把参考图挂到 user 消息的 media 上。image 为 null 时不做任何事。
     *
     * @param user  ChatClient 的 user 构造器
     * @param image {@link #resolveImage(String)} 校验通过后的本地文件
     */
    public static void attachImage(ChatClient.PromptUserSpec user, Path image) {
        if (image == null) {
            return;
        }
        user.media(imageMime(image), new FileSystemResource(image));
    }

    /**
     * 按文件后缀选 MIME，给 {@code user.media} 用。
     *
     * @param path 参考图路径
     * @return 对应的图片 MIME；未知后缀按 png 处理
     */
    private static MimeType imageMime(Path path) {
        return switch (extension(path)) {
            case "jpg", "jpeg" -> MimeTypeUtils.IMAGE_JPEG;
            case "gif" -> MimeTypeUtils.IMAGE_GIF;
            case "webp" -> MimeType.valueOf("image/webp");
            default -> MimeTypeUtils.IMAGE_PNG;
        };
    }

    /**
     * 取小写后缀，不含点。没有后缀或点在末尾时返回空串。
     *
     * @param path 文件路径
     * @return 例如 {@code png}、{@code jpeg}
     */
    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
