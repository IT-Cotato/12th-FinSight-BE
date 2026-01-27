package com.finsight.finsight.domain.storage.presentation;

import com.finsight.finsight.domain.storage.application.dto.request.SaveTermRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateTermFoldersRequest;
import com.finsight.finsight.domain.storage.application.dto.response.SavedTermListResponse;
import com.finsight.finsight.domain.storage.domain.service.SavedTermService;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storage/terms")
@RequiredArgsConstructor
@Tag(name = "Storage - Term", description = "용어 보관함 API")
public class SavedTermController {

    private final SavedTermService savedTermService;

    @PostMapping
    @Operation(summary = "용어 저장", description = "용어를 보관함에 저장합니다.")
    public ResponseEntity<DataResponse<Void>> saveTerm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveTermRequest request
    ) {
        savedTermService.saveTerm(userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @GetMapping
    @Operation(summary = "저장된 용어 목록 조회", description = "폴더별 저장된 용어를 조회합니다.")
    public ResponseEntity<DataResponse<SavedTermListResponse>> getSavedTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long folderId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        SavedTermListResponse response = savedTermService.getSavedTerms(
                userDetails.getUserId(), folderId, page, size);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/search")
    @Operation(summary = "저장된 용어 검색", description = "저장된 용어에서 용어명으로 검색합니다.")
    public ResponseEntity<DataResponse<SavedTermListResponse>> searchSavedTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        SavedTermListResponse response = savedTermService.searchSavedTerms(
                userDetails.getUserId(), q, page, size);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PutMapping("/{savedItemId}/folders")
    @Operation(summary = "용어 폴더 수정", description = "저장된 용어가 속한 폴더를 변경합니다.")
    public ResponseEntity<DataResponse<Void>> updateTermFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long savedItemId,
            @Valid @RequestBody UpdateTermFoldersRequest request
    ) {
        savedTermService.updateTermFolders(userDetails.getUserId(), savedItemId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @DeleteMapping("/{savedItemId}")
    @Operation(summary = "용어 삭제", description = "해당 폴더에서 용어를 삭제합니다.")
    public ResponseEntity<DataResponse<Void>> deleteTerm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long savedItemId
    ) {
        savedTermService.deleteTerm(userDetails.getUserId(), savedItemId);
        return ResponseEntity.ok(DataResponse.ok());
    }
}
