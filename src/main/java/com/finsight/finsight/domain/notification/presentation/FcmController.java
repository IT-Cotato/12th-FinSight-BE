package com.finsight.finsight.domain.notification.presentation;

import com.finsight.finsight.domain.notification.application.dto.request.FcmTokenRequest;
import com.finsight.finsight.domain.notification.domain.service.FcmService;
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
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
@Tag(name = "FCM", description = "FCM 푸시 알림 API")
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/token")
    @Operation(summary = "FCM 토큰 등록", description = "푸시 알림을 받기 위한 FCM 토큰을 등록합니다.")
    public ResponseEntity<DataResponse<Void>> registerToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        fcmService.saveToken(userDetails.getUserId(), request.fcmToken(), request.deviceType());
        return ResponseEntity.ok(DataResponse.ok());
    }

    @DeleteMapping("/token")
    @Operation(summary = "FCM 토큰 삭제", description = "본인의 FCM 토큰을 삭제합니다.")
    public ResponseEntity<DataResponse<Void>> deleteToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String fcmToken
    ) {
        fcmService.deleteToken(userDetails.getUserId(), fcmToken);
        return ResponseEntity.ok(DataResponse.ok());
    }
}