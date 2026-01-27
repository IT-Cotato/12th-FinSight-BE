package com.finsight.finsight.domain.storage.domain.service;

import com.finsight.finsight.domain.storage.application.dto.request.SaveTermRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateTermFoldersRequest;
import com.finsight.finsight.domain.storage.application.dto.response.SavedTermListResponse;
import com.finsight.finsight.domain.storage.application.dto.response.SavedTermResponse;
import com.finsight.finsight.domain.storage.exception.StorageException;
import com.finsight.finsight.domain.storage.exception.code.StorageErrorCode;
import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderItemEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderItemRepository;
import com.finsight.finsight.domain.storage.persistence.repository.FolderRepository;
import com.finsight.finsight.domain.term.persistence.entity.TermEntity;
import com.finsight.finsight.domain.term.persistence.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedTermService {

    private final FolderRepository folderRepository;
    private final FolderItemRepository folderItemRepository;
    private final TermRepository termRepository;

    /**
     * 용어 저장
     */
    @Transactional
    public void saveTerm(Long userId, SaveTermRequest request) {
        // 1. 용어 존재 확인
        TermEntity term = termRepository.findById(request.termId())
                .orElseThrow(() -> new StorageException(StorageErrorCode.TERM_NOT_FOUND));

        // 2. 폴더들 조회 및 검증
        List<FolderEntity> folders = folderRepository.findAllById(request.folderIds());

        if (folders.size() != request.folderIds().size()) {
            throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
        }

        for (FolderEntity folder : folders) {
            if (!folder.getUser().getUserId().equals(userId)) {
                throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
            }
            if (folder.getFolderType() != FolderType.TERM) {
                throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
            }
        }

        // 3. 이미 저장된 용어인지 확인
        boolean alreadySaved = folderItemRepository.existsByUserIdAndItemTypeAndItemId(
                userId, FolderType.TERM, request.termId());

        if (alreadySaved) {
            throw new StorageException(StorageErrorCode.ALREADY_SAVED_TERM);
        }

        // 4. 각 폴더에 저장
        for (FolderEntity folder : folders) {
            FolderItemEntity item = FolderItemEntity.builder()
                    .folder(folder)
                    .itemType(FolderType.TERM)
                    .itemId(request.termId())
                    .build();
            folderItemRepository.save(item);
        }
    }

    /**
     * 저장된 용어 목록 조회
     */
    public SavedTermListResponse getSavedTerms(Long userId, Long folderId, int page, int size) {
        int internalPage = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(internalPage, size);

        // 1. 폴더 조회
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser().getUserId().equals(userId)) {
            throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
        }

        // 2. 용어 조회
        Page<Object[]> resultPage = folderItemRepository.findSavedTermsByFolder(folder, pageable);

        // 3. Response 생성
        List<SavedTermResponse> responses = resultPage.getContent().stream()
                .map(row -> new SavedTermResponse(
                        (Long) row[0],          // savedItemId
                        (Long) row[1],          // termId
                        (String) row[2],        // displayName
                        (String) row[3],        // definition
                        (LocalDateTime) row[4]  // savedAt
                ))
                .toList();

        return new SavedTermListResponse(
                responses,
                page,
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.hasNext()
        );
    }

    /**
     * 저장된 용어 검색
     */
    public SavedTermListResponse searchSavedTerms(Long userId, String query, int page, int size) {
        int internalPage = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(internalPage, size);

        // 1. 검색
        Page<Object[]> resultPage = folderItemRepository.searchSavedTermsByQuery(userId, query, pageable);

        // 2. Response 생성
        List<SavedTermResponse> responses = resultPage.getContent().stream()
                .map(row -> new SavedTermResponse(
                        (Long) row[0],          // savedItemId
                        (Long) row[1],          // termId
                        (String) row[2],        // displayName
                        (String) row[3],        // definition
                        (LocalDateTime) row[4]  // savedAt
                ))
                .toList();

        return new SavedTermListResponse(
                responses,
                page,
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.hasNext()
        );
    }

    /**
     * 용어 폴더 수정
     */
    @Transactional
    public void updateTermFolders(Long userId, Long savedItemId, UpdateTermFoldersRequest request) {
        // 1. 기존 저장 항목 조회
        FolderItemEntity item = folderItemRepository.findById(savedItemId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.SAVED_ITEM_NOT_FOUND));

        if (!item.getFolder().getUser().getUserId().equals(userId)) {
            throw new StorageException(StorageErrorCode.SAVED_ITEM_NOT_FOUND);
        }

        Long termId = item.getItemId();

        // 2. 새 폴더들 검증
        List<FolderEntity> newFolders = folderRepository.findAllById(request.folderIds());

        if (newFolders.size() != request.folderIds().size()) {
            throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
        }

        for (FolderEntity folder : newFolders) {
            if (!folder.getUser().getUserId().equals(userId)) {
                throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
            }
            if (folder.getFolderType() != FolderType.TERM) {
                throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
            }
        }

        // 3. 기존 연결 모두 삭제
        folderItemRepository.deleteByUserIdAndItemTypeAndItemId(userId, FolderType.TERM, termId);

        // 4. 새로운 폴더들에 저장
        for (FolderEntity folder : newFolders) {
            FolderItemEntity newItem = FolderItemEntity.builder()
                    .folder(folder)
                    .itemType(FolderType.TERM)
                    .itemId(termId)
                    .build();
            folderItemRepository.save(newItem);
        }
    }

    /**
     * 용어 삭제
     */
    @Transactional
    public void deleteTerm(Long userId, Long savedItemId) {
        // 1. 저장 항목 조회 및 검증
        FolderItemEntity item = folderItemRepository.findById(savedItemId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.SAVED_ITEM_NOT_FOUND));

        if (!item.getFolder().getUser().getUserId().equals(userId)) {
            throw new StorageException(StorageErrorCode.SAVED_ITEM_NOT_FOUND);
        }

        // 2. 해당 항목만 삭제
        folderItemRepository.delete(item);
    }
}
