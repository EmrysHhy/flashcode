package com.bitejiuyeke.imagemcp.web;

import com.bitejiuyeke.imagemcp.tool.ImageSearchTool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * portal 用 HTTP 调搜图，避免 MCP SSE 握手连不上。
 */
@RestController
public class ImageSearchController {

    private final ImageSearchTool imageSearchTool;

    public ImageSearchController(ImageSearchTool imageSearchTool) {
        this.imageSearchTool = imageSearchTool;
    }

    /**
     * HTTP 搜图入口，供 portal 调用。
     * 规则：query 为关键词，count 1～8；返回 [{url, alt}] JSON 数组。
     */
    @GetMapping("/search_images")
    public String searchImages(@RequestParam String query,
                               @RequestParam(defaultValue = "8") Integer count) {
        return imageSearchTool.searchImages(query, count);
    }
}
