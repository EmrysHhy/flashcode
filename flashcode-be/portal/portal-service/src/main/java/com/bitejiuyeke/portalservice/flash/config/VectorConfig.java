package com.bitejiuyeke.portalservice.flash.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class VectorConfig implements ApplicationRunner {

    private static final int EMBEDDING_BATCH_SIZE = 10;

    @Autowired
    private MilvusVectorStore vectorStore;

    @Value("classpath:docs/job.pdf")
    private Resource jobPdf;

    @Override
    public void run(ApplicationArguments args) {
        List<Document> seedDocs = List.of(
                new Document("1", "我学校的课程有Java、C++、测试", Map.of(
                        "type", "course",
                        "name", "学校课程介绍"
                )),
                new Document("2", "我学校的老师有小宇、小李、小张", Map.of(
                        "type", "teacher",
                        "name", "我学校的老师介绍"
                )),
                new Document("3", "我学校的的小宇老师擅长Java、Linux.负责Java课程讲解", Map.of(
                        "type", "teacher_detail",
                        "name", "小宇老师介绍"
                )),
                new Document("4", "我学校的小李老师擅长C++、网络。负责C++课程讲解", Map.of(
                        "type", "teacher_detail",
                        "name", "小李老师介绍"
                )),
                new Document("5", "我学校的小张老师擅长测试、运维负责测试课程讲解", Map.of(
                        "type", "teacher_detail",
                        "name", "小张老师介绍"
                ))
        );
        try {
            vectorStore.add(seedDocs);
            ingestJobPdf();
            log.info("Vector data initialized");
        } catch (Exception e) {
            log.error("向量库初始化失败，portal 继续启动。请检查容器能否访问 https://dashscope.aliyuncs.com", e);
        }
    }

    private void ingestJobPdf() {
        if (jobPdf == null || !jobPdf.exists()) {
            log.warn("classpath:docs/job.pdf 不存在，跳过 PDF 入库");
            return;
        }
        List<Document> existing = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("岗位职责")
                        .topK(1)
                        .filterExpression("source == 'job.pdf'")
                        .build()
        );
        if (existing != null && !existing.isEmpty()) {
            log.info("job.pdf 已在向量库中，跳过重复入库");
            return;
        }

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageExtractedTextFormatter(
                        ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                .withPagesPerDocument(1)
                .build();
        List<Document> pages = new PagePdfDocumentReader(jobPdf, config).read();
        for (Document page : pages) {
            page.getMetadata().put("source", "job.pdf");
            page.getMetadata().put("type", "job");
        }
        List<Document> chunks = new TokenTextSplitter().apply(pages);
        for (int i = 0; i < chunks.size(); i += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, chunks.size());
            vectorStore.add(new ArrayList<>(chunks.subList(i, end)));
            log.info("job.pdf 已入库 {} / {}", end, chunks.size());
        }
    }
}
