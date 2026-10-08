package com.bitejiuyeke.portalservice.flash.domain.dto.result;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 一批正在转存到 OSS 的配图。
 * 提示词里只有 IMG_n，OSS 地址要等 {@link #restore(String)} 再写回源码。
 */
public final class StockImageBatch {

    private static final StockImageBatch EMPTY = new StockImageBatch(List.of(), List.of());

    private final List<StockImageDTO> hints;
    private final List<CompletableFuture<String>> ossUrls;

    public StockImageBatch(List<StockImageDTO> hints, List<CompletableFuture<String>> ossUrls) {
        this.hints = hints == null ? List.of() : List.copyOf(hints);
        this.ossUrls = ossUrls == null ? List.of() : List.copyOf(ossUrls);
    }

    public static StockImageBatch empty() {
        return EMPTY;
    }

    /**
     * 给提示词用的列表。url 为空，避免把图床或 OSS 地址发给模型。
     */
    public List<StockImageDTO> hints() {
        return hints;
    }

    public boolean isEmpty() {
        return hints.isEmpty();
    }

    /**
     * 等转存结束。成功的 IMG_n 换成 OSS 地址，失败的从源码里删掉。
     * 从大编号往回换，避免 IMG_1 碰到 IMG_10。
     */
    public String restore(String content) {
        if (content == null || hints.isEmpty()) {
            return content;
        }
        String result = content;
        int count = Math.min(hints.size(), ossUrls.size());
        for (int i = count; i >= 1; i--) {
            String token = "IMG_" + i;
            String oss = join(ossUrls.get(i - 1));
            result = result.replace(token, oss == null ? "" : oss);
        }
        return result;
    }

    private static String join(CompletableFuture<String> future) {
        if (future == null) {
            return null;
        }
        try {
            String url = future.join();
            if (url == null || url.isBlank()) {
                return null;
            }
            return url;
        } catch (Exception e) {
            return null;
        }
    }
}
