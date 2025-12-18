package com.finsight.finsight.domain.example.presentation;

import com.finsight.finsight.domain.example.application.dto.request.ExampleRequest;
import com.finsight.finsight.domain.example.application.dto.response.ExampleResponse;
import com.finsight.finsight.domain.example.domain.service.ExampleService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.response.DefaultIdResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/examples")
public class ExampleController {

    private final ExampleService exampleService;

    @PostMapping
    public ResponseEntity<DataResponse<DefaultIdResponse>> save(@RequestBody ExampleRequest request) {
        return ResponseEntity.ok(
                DataResponse.created(
                        DefaultIdResponse.of(exampleService.save(request.name()))
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<ExampleResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(
                DataResponse.from(exampleService.findById(id))
        );
    }
}
