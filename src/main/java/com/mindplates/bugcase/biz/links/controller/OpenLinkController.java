package com.mindplates.bugcase.biz.links.controller;

import com.mindplates.bugcase.biz.links.dto.OpenLinkDTO;
import com.mindplates.bugcase.biz.links.service.OpenLinkService;
import com.mindplates.bugcase.biz.links.vo.request.OpenLinkRequest;
import com.mindplates.bugcase.biz.links.vo.response.OpenLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/{spaceCode}/projects/{projectId}/links")
@AllArgsConstructor
public class OpenLinkController {

    private final OpenLinkService openLinkService;

    @Operation(description = "오픈 링크 목록 조회")
    @GetMapping
    public List<OpenLinkResponse> selectOpenLinkList(@PathVariable String spaceCode, @PathVariable long projectId) {
        List<OpenLinkDTO> openLinkList = openLinkService.selectOpenLinkList(projectId);
        return openLinkList.stream().map(OpenLinkResponse::new).collect(Collectors.toList());
    }

    @Operation(description = "오픈 링크 생성")
    @PostMapping
    public OpenLinkResponse createOpenLink(@PathVariable String spaceCode, @PathVariable long projectId, @Valid @RequestBody OpenLinkRequest openLinkRequest) {
        OpenLinkDTO openLink = openLinkService.createOpenLink(projectId, openLinkRequest.toDTO(projectId));
        return new OpenLinkResponse(openLink);
    }


}

