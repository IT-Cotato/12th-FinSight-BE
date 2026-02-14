package com.finsight.finsight.domain.storage.presentation;

import com.finsight.finsight.domain.storage.application.dto.request.SaveNewsRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateNewsFoldersRequest;
import com.finsight.finsight.domain.storage.application.dto.response.SavedNewsListResponse;
import com.finsight.finsight.domain.storage.domain.service.SavedNewsService;
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
@RequestMapping("/api/storage/news")
@RequiredArgsConstructor
@Tag(name = "Storage - News", description = "뉴스 보관함 API")
public class SavedNewsController {

    private final SavedNewsService savedNewsService;

    @PostMapping
    @Operation(summary = "뉴스 저장", description = "뉴스를 보관함에 저장합니다.")
    public ResponseEntity<DataResponse<Void>> saveNews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SaveNewsRequest request
    ) {
        savedNewsService.saveNews(userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @GetMapping
    @Operation(summary = "저장된 뉴스 목록 조회", description = "폴더별 저장된 뉴스를 조회합니다.")
    public ResponseEntity<DataResponse<SavedNewsListResponse>> getSavedNews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long folderId,
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int size
    ) {
        SavedNewsListResponse response = savedNewsService.getSavedNews(
                userDetails.getUserId(), folderId, section, page, size);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/search")
    @Operation(summary = "저장된 뉴스 검색", description = "특정 폴더 내에서 저장된 뉴스를 제목/내용으로 검색합니다.")
    public ResponseEntity<DataResponse<SavedNewsListResponse>> searchSavedNews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long folderId,
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int size
    ) {
        SavedNewsListResponse response = savedNewsService.searchSavedNews(
                userDetails.getUserId(), folderId, q, page, size);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PutMapping("/{savedItemId}/folders")
    @Operation(summary = "뉴스 폴더 수정", description = "저장된 뉴스가 속한 폴더를 변경합니다.")
    public ResponseEntity<DataResponse<Void>> updateNewsFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long savedItemId,
            @Valid @RequestBody UpdateNewsFoldersRequest request
    ) {
        savedNewsService.updateNewsFolders(userDetails.getUserId(), savedItemId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @DeleteMapping("/{savedItemId}")
    @Operation(summary = "뉴스 삭제", description = "해당 폴더에서 뉴스를 삭제합니다.")
    public ResponseEntity<DataResponse<Void>> deleteNews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long savedItemId
    ) {
        savedNewsService.deleteNews(userDetails.getUserId(), savedItemId);
        return ResponseEntity.ok(DataResponse.ok());
    }
}
