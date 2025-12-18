package com.finsight.finsight.domain.example.presentation;

import com.finsight.finsight.domain.example.application.dto.request.ExampleRequest;
import com.finsight.finsight.domain.example.application.dto.response.ExampleResponse;
import com.finsight.finsight.domain.example.domain.service.ExampleService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.response.DefaultIdResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/examples")
public class ExampleController {

    private final ExampleService exampleService;

    @Operation(
            summary = "예시 이름 저장 API By 김민규 (개발 완료)",
            description = "이름을 받아와서 DB에 저장합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "실패")
    })
    @PostMapping
    public ResponseEntity<DataResponse<DefaultIdResponse>> save(@RequestBody ExampleRequest request) {
        return ResponseEntity.ok(
                DataResponse.created(
                        DefaultIdResponse.of(exampleService.save(request.name()))
                )
        );
    }

    @Operation(
            summary = "예시 이름 조회 API By 김민규 (개발 완료)",
            description = "id값에 해당하는 이름을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "실패")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ExampleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                DataResponse.from(exampleService.findById(id))
        );
    }
}
