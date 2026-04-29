package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.notification.PushSubscriptionRequest;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.service.PushSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push-subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;

    @PostMapping
    public ResponseEntity<?> saveSubscription(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody PushSubscriptionRequest request
    ) {
        pushSubscriptionService.saveSubscription(principal.getId(), request);

        return ResponseEntity.ok(Map.of(
                "message", "Subscription notifiche salvata correttamente"
        ));
    }
}