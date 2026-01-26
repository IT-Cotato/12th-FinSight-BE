package com.finsight.finsight.domain.storage.domain.service;

import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.naver.persistence.entity.NaverArticleEntity;
import com.finsight.finsight.domain.naver.persistence.repository.NaverArticleRepository;
import com.finsight.finsight.domain.storage.application.dto.request.SaveNewsRequest;
import com.finsight.finsight.domain.storage.application.dto.request.UpdateNewsFoldersRequest;
import com.finsight.finsight.domain.storage.application.dto.response.SavedNewsListResponse;
import com.finsight.finsight.domain.storage.application.dto.response.SavedNewsResponse;
import com.finsight.finsight.domain.storage.exception.StorageException;
import com.finsight.finsight.domain.storage.exception.code.StorageErrorCode;
import com.finsight.finsight.domain.storage.persistence.entity.FolderEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderItemEntity;
import com.finsight.finsight.domain.storage.persistence.entity.FolderType;
import com.finsight.finsight.domain.storage.persistence.repository.FolderItemRepository;
import com.finsight.finsight.domain.storage.persistence.repository.FolderRepository;
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
public class SavedNewsService {

    private final FolderRepository folderRepository;
    private final FolderItemRepository folderItemRepository;
    private final NaverArticleRepository naverArticleRepository;

    /**
     * 뉴스 저장
     */
    @Transactional
    public void saveNews(Long userId, SaveNewsRequest request) {
        // 1. 뉴스 존재 확인
        NaverArticleEntity article = naverArticleRepository.findById(request.articleId())
                .orElseThrow(() -> new StorageException(StorageErrorCode.NEWS_NOT_FOUND));

        // 2. 폴더들 조회 및 검증
        List<FolderEntity> folders = folderRepository.findAllById(request.folderIds());

        if (folders.size() != request.folderIds().size()) {
            throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
        }

        for (FolderEntity folder : folders) {
            if (!folder.getUser().getUserId().equals(userId)) {
                throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
            }
            if (folder.getFolderType() != FolderType.NEWS) {
                throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
            }
        }

        // 3. 이미 저장된 뉴스인지 확인
        boolean alreadySaved = folderItemRepository.existsByUserIdAndItemTypeAndItemId(
                userId, FolderType.NEWS, request.articleId());

        if (alreadySaved) {
            throw new StorageException(StorageErrorCode.ALREADY_SAVED);
        }

        // 4. 각 폴더에 저장
        for (FolderEntity folder : folders) {
            FolderItemEntity item = FolderItemEntity.builder()
                    .folder(folder)
                    .itemType(FolderType.NEWS)
                    .itemId(request.articleId())
                    .build();
            folderItemRepository.save(item);
        }
    }

    /**
     * 저장된 뉴스 목록 조회
     */
    public SavedNewsListResponse getSavedNews(Long userId, Long folderId, String section, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // 1. 폴더 조회 (필수)
        FolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.FOLDER_NOT_FOUND));

        if (!folder.getUser().getUserId().equals(userId)) {
            throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
        }

        // 2. section String -> enum 변환 (null 허용)
        NaverEconomySection sectionEnum = null;
        if (section != null && !section.isBlank()) {
            try {
                sectionEnum = NaverEconomySection.valueOf(section.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 section이면 필터링 없이 전체 조회
                sectionEnum = null;
            }
        }

        // 3. 뉴스 조회 (섹션 필터링 포함)
        Page<Object[]> resultPage = folderItemRepository.findSavedNewsWithFilter(folder, sectionEnum, pageable);

        // 4. Response 생성
        List<SavedNewsResponse> responses = resultPage.getContent().stream()
                .map(row -> new SavedNewsResponse(
                        (Long) row[0],                              // savedItemId
                        (Long) row[1],                              // articleId
                        (String) row[2],                            // title
                        (String) row[3],                            // press
                        ((NaverEconomySection) row[4]).name(),      // section
                        (String) row[5],                            // thumbnailUrl
                        (LocalDateTime) row[6],                     // publishedAt
                        (LocalDateTime) row[7]                      // savedAt
                ))
                .toList();

        return new SavedNewsListResponse(
                responses,
                page,
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.hasNext()
        );
    }

    /**
     * 저장된 뉴스 검색
     */
    public SavedNewsListResponse searchSavedNews(Long userId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // 1. 검색 (JOIN 쿼리로 한 번에)
        Page<Object[]> resultPage = folderItemRepository.searchSavedNewsByQuery(userId, query, pageable);

        // 2. Response 생성
        List<SavedNewsResponse> responses = resultPage.getContent().stream()
                .map(row -> new SavedNewsResponse(
                        (Long) row[0],                              // savedItemId
                        (Long) row[1],                              // articleId
                        (String) row[2],                            // title
                        (String) row[3],                            // press
                        ((NaverEconomySection) row[4]).name(),      // section
                        (String) row[5],                            // thumbnailUrl
                        (LocalDateTime) row[6],                     // publishedAt
                        (LocalDateTime) row[7]                      // savedAt
                ))
                .toList();

        return new SavedNewsListResponse(
                responses,
                page,
                resultPage.getTotalPages(),
                resultPage.getTotalElements(),
                resultPage.hasNext()
        );
    }

    /**
     * 뉴스 폴더 수정
     */
    @Transactional
    public void updateNewsFolders(Long userId, Long savedItemId, UpdateNewsFoldersRequest request) {
        // 1. 기존 저장 항목 조회
        FolderItemEntity item = folderItemRepository.findById(savedItemId)
                .orElseThrow(() -> new StorageException(StorageErrorCode.SAVED_ITEM_NOT_FOUND));

        if (!item.getFolder().getUser().getUserId().equals(userId)) {
            throw new StorageException(StorageErrorCode.SAVED_ITEM_NOT_FOUND);
        }

        Long articleId = item.getItemId();

        // 2. 새 폴더들 검증
        List<FolderEntity> newFolders = folderRepository.findAllById(request.folderIds());

        if (newFolders.size() != request.folderIds().size()) {
            throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
        }

        for (FolderEntity folder : newFolders) {
            if (!folder.getUser().getUserId().equals(userId)) {
                throw new StorageException(StorageErrorCode.FOLDER_NOT_FOUND);
            }
            if (folder.getFolderType() != FolderType.NEWS) {
                throw new StorageException(StorageErrorCode.FOLDER_TYPE_MISMATCH);
            }
        }

        // 3. 기존 연결 모두 삭제
        folderItemRepository.deleteByUserIdAndItemTypeAndItemId(userId, FolderType.NEWS, articleId);

        // 4. 새로운 폴더들에 저장
        for (FolderEntity folder : newFolders) {
            FolderItemEntity newItem = FolderItemEntity.builder()
                    .folder(folder)
                    .itemType(FolderType.NEWS)
                    .itemId(articleId)
                    .build();
            folderItemRepository.save(newItem);
        }
    }

    /**
     * 뉴스 삭제 (해당 폴더에서만)
     */
    @Transactional
    public void deleteNews(Long userId, Long savedItemId) {
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
