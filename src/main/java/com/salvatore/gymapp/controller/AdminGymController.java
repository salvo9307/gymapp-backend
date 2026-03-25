package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.*;
import com.salvatore.gymapp.entity.Gym;
import com.salvatore.gymapp.service.GymService;
import com.salvatore.gymapp.service.GymSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/gyms")
@RequiredArgsConstructor
public class AdminGymController {

    private final GymService gymService;
    private final GymSubscriptionService gymSubscriptionService;

    @PostMapping
    public Long createGym(@Valid @RequestBody CreateGymRequest request) {
        Gym gym = gymService.createGym(request);
        return gym.getId();
    }

    @PostMapping("/with-manager")
    public Long createGymWithManager(@Valid @RequestBody CreateGymWithManagerRequest request) {
        return gymService.createGymWithManager(request);
    }

    @PutMapping("/{gymId}/status")
    public void updateGymStatus(@PathVariable Long gymId,
                                @Valid @RequestBody UpdateGymStatusRequest request) {
        gymService.updateGymStatus(gymId, request);
    }

    @PutMapping("/{gymId}/manager/reset-password")
    public void resetGymManagerPassword(@PathVariable Long gymId,
                                        @Valid @RequestBody ResetUserPasswordRequest request) {
        gymService.resetGymManagerPassword(gymId, request);
    }

    @PutMapping("/{gymId}/renew-subscription")
    public ResponseEntity<Void> renewGymSubscription(
            @PathVariable Long gymId,
            @RequestBody RenewGymSubscriptionRequest request) {

        gymSubscriptionService.renewSubscription(gymId, request.getMonths());
        return ResponseEntity.ok().build();
    }
}