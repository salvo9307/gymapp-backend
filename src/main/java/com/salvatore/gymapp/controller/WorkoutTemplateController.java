package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.CreateWorkoutPlanRequest;
import com.salvatore.gymapp.dto.auth.WorkoutTemplateResponse;
import com.salvatore.gymapp.dto.auth.WorkoutTemplateSummaryResponse;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.service.WorkoutTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/templates")
@RequiredArgsConstructor
public class WorkoutTemplateController {

    private final WorkoutTemplateService templateService;

    @GetMapping
    public List<WorkoutTemplateSummaryResponse> getTemplates(
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return templateService.getTemplates(currentUser);
    }

    @GetMapping("/{templateId}")
    public WorkoutTemplateResponse getTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return templateService.getTemplate(templateId, currentUser);
    }

    @PostMapping
    public Long createTemplate(
            @Valid @RequestBody CreateWorkoutPlanRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return templateService.createTemplate(request, currentUser);
    }

    @PutMapping("/{templateId}")
    public Long updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CreateWorkoutPlanRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return templateService.updateTemplate(templateId, request, currentUser);
    }

    @DeleteMapping("/{templateId}")
    public void deleteTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        templateService.deleteTemplate(templateId, currentUser);
    }

    @PostMapping("/{templateId}/apply/{userId}")
    public Long applyTemplate(
            @PathVariable Long templateId,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return templateService.applyTemplate(templateId, userId, currentUser);
    }
}