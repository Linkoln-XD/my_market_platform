package com.market.notification.admin.web;

import com.market.notification.admin.AdminNotificationService;
import com.market.notification.admin.dto.NotificationPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public NotificationPageResponse list(
            @PageableDefault(size = 20, sort = "deliveredAt") Pageable pageable) {
        return adminNotificationService.list(pageable);
    }
}
