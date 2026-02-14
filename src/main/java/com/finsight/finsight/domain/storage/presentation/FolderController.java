package com.finsight.finsight.domain.storage.presentation;

import com.finsight.finsight.domain.storage.application.dto.request.CreateFolderRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateFolderOrderRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateFolderRequest;
import com.finsight.finsight.domain.storage.application.dto.response.FolderResponse;
import com.finsight.finsight.domain.storage.domain.service.FolderService;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.global.response.DataResponse;
import com.finsight.finsight.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage/folders")
@RequiredArgsConstructor
@Tag(name = "Storage - Folder", description = "보관함 폴더 API")
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    @Operation(summary = "폴더 목록 조회", description = "사용자의 폴더 목록을 조회합니다.")
    public ResponseEntity<DataResponse<List<FolderResponse>>> getFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String type
    ) {
        FolderType folderType = FolderType.valueOf(type);
        List<FolderResponse> response = folderService.getFolders(userDetails.getUserId(), folderType);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PostMapping
    @Operation(summary = "폴더 생성", description = "새 폴더를 생성합니다. (최대 10개)")
    public ResponseEntity<DataResponse<FolderResponse>> createFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateFolderRequest request
    ) {
        FolderResponse response = folderService.createFolder(userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PutMapping("/{folderId}")
    @Operation(summary = "폴더 수정", description = "폴더 이름을 수정합니다.")
    public ResponseEntity<DataResponse<FolderResponse>> updateFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @Valid @RequestBody UpdateFolderRequest request
    ) {
        FolderResponse response = folderService.updateFolder(userDetails.getUserId(), folderId, request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "폴더 삭제", description = "폴더를 삭제합니다. (폴더 내 항목도 함께 삭제)")
    public ResponseEntity<DataResponse<Void>> deleteFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        folderService.deleteFolder(userDetails.getUserId(), folderId);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @PutMapping("/order")
    @Operation(summary = "폴더 순서 변경", description = "폴더 순서를 변경합니다.")
    public ResponseEntity<DataResponse<List<FolderResponse>>> updateFolderOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateFolderOrderRequest request
    ) {
        List<FolderResponse> response = folderService.updateFolderOrder(userDetails.getUserId(), request);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "아이템이 저장된 폴더 조회", description = "특정 뉴스/용어가 저장된 폴더 목록을 반환합니다.")
    public ResponseEntity<DataResponse<List<FolderResponse>>> getItemFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam String type
    ) {
        List<FolderResponse> response = folderService.getItemFolders(userDetails.getUserId(), type, itemId);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
