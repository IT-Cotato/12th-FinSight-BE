package com.finsight.finsight.domain.term.presentation;

import com.finsight.finsight.domain.term.application.dto.response.TermResponse;
import com.finsight.finsight.domain.term.domain.service.TermQueryService;
import com.finsight.finsight.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/terms")
public class TermController {

    private final TermQueryService termQueryService;

    @Operation(summary = "용어 상세 조회 API", description = "용어 ID를 통해 용어의 id, 이름, 설명을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "용어 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 용어")
    })
    @GetMapping("/{termId}")
    public ResponseEntity<DataResponse<TermResponse>> getTerm(@PathVariable Long termId) {
        return ResponseEntity.ok(DataResponse.from(termQueryService.getTerm(termId)));
    }
}
