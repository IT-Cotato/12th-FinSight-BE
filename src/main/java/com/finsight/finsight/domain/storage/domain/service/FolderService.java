package com.finsight.finsight.domain.storage.domain.service;

import com.finsight.finsight.domain.auth.exception.AuthException;
import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.domain.storage.application.dto.request.CreateFolderRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateFolderOrderRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateFolderRequest;
import com.finsight.finsight.domain.storage.application.dto.response.FolderResponse;
import com.finsight.finsight.domain.storage.exception.StorageException;
import com.finsight.finsight.domain.storage.exception.code.StorageErrorCode;
import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderRepository;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderService {

    private static final int MAX_FOLDER_COUNT = 10;

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;

    // 폴더 목록 조회
    public List<FolderResponse> getFolders(Long userId, FolderType folderType) {
        List<FolderEntity> folders = folderRepository.findByUserUserIdAndFolderTypeOrderBySortOrderAsc(userId, folderType);
        return folders.stream()
                .map(FolderResponse::from)
                .toList();
    }

    // 폴더 생성
    @Transactional
    public FolderResponse createFolder(Long userId, CreateFolderRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        FolderType folderType;
        try {
            folderType = FolderType.valueOf(request.folderType());
        } catch (IllegalArgumentException e) {
            throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
        }

        // 폴더 개수 제한 체크
        long folderCount = folderRepository.countByUserUserIdAndFolderType(userId, folderType);
        if (folderCount >= MAX_FOLDER_COUNT) {
            throw new StorageException(StorageErrorCode.FOLDER_LIMIT_EXCEEDED);
        }

        // 폴더명 중복 체크
        if (folderRepository.existsByUserUserIdAndFolderTypeAndFolderName(userId, folderType, request.folderName())) {
            throw new StorageException(StorageErrorCode.FOLDER_NAME_DUPLICATE);
        }

        // 새 폴더 순서 = 마지막 + 1
        int nextOrder = (int) folderCount + 1;

        FolderEntity folder = FolderEntity.builder()
                .user(user)
                .folderType(folderType)
                .folderName(request.folderName())
                .sortOrder(nextOrder)
                .build();

        FolderEntity saved = folderRepository.save(folder);
        return FolderResponse.from(saved);
    }

    // 폴더 수정
    @Transactional
    public FolderResponse updateFolder(Long userId, Long folderId, UpdateFolderRequest request) {
        FolderEntity folder = folderRepository.findByFolderIdAndUserUserId(folderId, userId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.FOLDER_NOT_FOUND));

        // 폴더명 중복 체크 (자기 자신 제외)
        if (!folder.getFolderName().equals(request.folderName()) &&
                folderRepository.existsByUserUserIdAndFolderTypeAndFolderName(userId, folder.getFolderType(), request.folderName())) {
            throw new StorageException(StorageErrorCode.FOLDER_NAME_DUPLICATE);
        }

        folder.updateFolderName(request.folderName());
        return FolderResponse.from(folder);
    }

    // 폴더 삭제
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        FolderEntity folder = folderRepository.findByFolderIdAndUserUserId(folderId, userId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.FOLDER_NOT_FOUND));

        FolderType folderType = folder.getFolderType();
        int deletedOrder = folder.getSortOrder();

        folderRepository.delete(folder);

        // 삭제된 폴더보다 뒤에 있는 폴더들 순서 재정렬
        List<FolderEntity> remainingFolders = folderRepository.findByUserUserIdAndFolderTypeOrderBySortOrderAsc(userId, folderType);
        int newOrder = 1;
        for (FolderEntity f : remainingFolders) {
            f.updateSortOrder(newOrder++);
        }
    }

    // 폴더 순서 변경
    @Transactional
    public List<FolderResponse> updateFolderOrder(Long userId, UpdateFolderOrderRequest request) {
        FolderType folderType;
        try {
            folderType = FolderType.valueOf(request.folderType());
        } catch (IllegalArgumentException e) {
            throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
        }

        for (UpdateFolderOrderRequest.FolderOrder order : request.folders()) {
            FolderEntity folder = folderRepository.findByFolderIdAndUserUserId(order.folderId(), userId)
                    .orElseThrow(() -> new StorageException(StorageErrorCode.FOLDER_NOT_FOUND));
            
            // 폴더 타입 검증
            if (folder.getFolderType() != folderType) {
                throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
            }
            
            folder.updateSortOrder(order.sortOrder());
        }

        return getFolders(userId, folderType);
    }
}
