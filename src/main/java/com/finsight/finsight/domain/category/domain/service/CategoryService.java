package com.finsight.finsight.domain.category.domain.service;

import com.finsight.finsight.domain.auth.exception.AuthException;
import com.finsight.finsight.domain.auth.exception.code.AuthErrorCode;
import com.finsight.finsight.domain.category.application.dto.request.SaveCategoryRequest;
import com.finsight.finsight.domain.category.application.dto.request.UpdateCategoryOrderRequest;
import com.finsight.finsight.domain.category.application.dto.response.CategoryOrderResponse;
import com.finsight.finsight.domain.category.application.dto.response.CategoryResponse;
import com.finsight.finsight.domain.category.exception.CategoryException;
import com.finsight.finsight.domain.category.exception.code.CategoryErrorCode;
import com.finsight.finsight.domain.category.persistence.entity.CategoryEntity;
import com.finsight.finsight.domain.category.persistence.entity.UserCategoryEntity;
import com.finsight.finsight.domain.category.persistence.entity.UserCategoryOrderEntity;
import com.finsight.finsight.domain.category.persistence.repository.CategoryRepository;
import com.finsight.finsight.domain.category.persistence.repository.UserCategoryOrderRepository;
import com.finsight.finsight.domain.category.persistence.repository.UserCategoryRepository;
import com.finsight.finsight.domain.naver.domain.constant.NaverEconomySection;
import com.finsight.finsight.domain.user.persistence.entity.UserEntity;
import com.finsight.finsight.domain.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int MINIMUM_CATEGORY_COUNT = 3;

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserCategoryOrderRepository userCategoryOrderRepository;

    @Transactional
    public void saveCategories(Long userId, SaveCategoryRequest request) {
        List<NaverEconomySection> sections = validateAndParseSections(request.sections());

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        userCategoryRepository.deleteByUserUserId(userId);

        List<UserCategoryEntity> categories = sections.stream()
                .map(section -> UserCategoryEntity.of(user, section))
                .toList();

        userCategoryRepository.saveAll(categories);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategories(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND);
        }

        List<NaverEconomySection> sections = userCategoryRepository.findByUserUserId(userId)
                .stream()
                .map(UserCategoryEntity::getSection)
                .toList();

        return CategoryResponse.from(sections);
    }

    /**
     * 카테고리 순서 조회
     */
    @Transactional(readOnly = true)
    public CategoryOrderResponse getCategoryOrder(Long userId) {
        List<UserCategoryOrderEntity> userOrders = userCategoryOrderRepository
                .findByUserUserIdOrderBySortOrderAsc(userId);

        List<CategoryOrderResponse.CategoryOrderItem> items;

        if (userOrders.isEmpty()) {
            List<CategoryEntity> defaultCategories = categoryRepository.findAllByOrderBySortOrderAsc();
            items = defaultCategories.stream()
                    .map(c -> new CategoryOrderResponse.CategoryOrderItem(
                            c.getCategoryId(),
                            c.getCode(),
                            c.getNameKo(),
                            c.getSortOrder()
                    ))
                    .toList();
        } else {
            items = userOrders.stream()
                    .map(uo -> new CategoryOrderResponse.CategoryOrderItem(
                            uo.getCategory().getCategoryId(),
                            uo.getCategory().getCode(),
                            uo.getCategory().getNameKo(),
                            uo.getSortOrder()
                    ))
                    .toList();
        }

        return new CategoryOrderResponse(items);
    }

    /**
     * 카테고리 순서 변경
     */
    @Transactional
    public void updateCategoryOrder(Long userId, UpdateCategoryOrderRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));

        // 기존 순서 삭제
        userCategoryOrderRepository.deleteByUserUserId(userId);
        userCategoryOrderRepository.flush();  // 삭제 먼저 DB에 반영

        // 새 순서 저장
        for (UpdateCategoryOrderRequest.CategoryOrderItem item : request.orders()) {
            CategoryEntity category = categoryRepository.findById(item.categoryId())
                    .orElseThrow(() -> new CategoryException(CategoryErrorCode.INVALID_CATEGORY_SECTION));

            UserCategoryOrderEntity order = UserCategoryOrderEntity.builder()
                    .user(user)
                    .category(category)
                    .sortOrder(item.sortOrder())
                    .build();
            userCategoryOrderRepository.save(order);
        }
    }

    private List<NaverEconomySection> validateAndParseSections(List<String> sectionNames) {
        if (sectionNames.size() < MINIMUM_CATEGORY_COUNT) {
            throw new CategoryException(CategoryErrorCode.MINIMUM_CATEGORY_REQUIRED);
        }

        return sectionNames.stream()
                .map(this::parseSection)
                .toList();
    }

    private NaverEconomySection parseSection(String sectionName) {
        try {
            return NaverEconomySection.valueOf(sectionName);
        } catch (IllegalArgumentException e) {
            throw new CategoryException(CategoryErrorCode.INVALID_CATEGORY_SECTION);
        }
    }
}
