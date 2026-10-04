package com.bitejiuyeke.biteportalservice.flash.agent;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.GenerateAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.vo.GenerateAppVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Emrys
 * content:
 */
@RequestMapping("/flashcode")
@RestController
@Validated
@Slf4j
public class AgentController {

    @Autowired
    MultiAgentWorkFlow multiAgentWorkFlow;

    @PostMapping("/app/generate")
    public R<GenerateAppVO> appGenerate(@RequestParam @NotNull(message = "应用ID不能为空") Long appId,
                                        @RequestParam @NotBlank(message = "需求文档不能为空") String requirement,
                                        @RequestParam(required = false) MultipartFile reference) {
        log.info("appGenerate: appId={}, requirementLength={}, reference={}", appId,
                requirement.length(), reference == null || reference.isEmpty() ? "null" : reference.getOriginalFilename());
        GenerateAppDTO generateAppDTO = multiAgentWorkFlow.generate(appId, requirement, reference);
        log.info("应用生成成功: appId={}, generateAppDTO={}", appId, generateAppDTO);
        return R.ok(generateAppDTO.convertToVO());
    }
}
