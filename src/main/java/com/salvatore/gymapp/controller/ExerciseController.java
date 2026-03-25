package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.CreateExerciseRequest;
import com.salvatore.gymapp.dto.auth.ExerciseResponse;
import com.salvatore.gymapp.dto.auth.UpdateExerciseRequest;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping
    public ExerciseResponse createExercise(@Valid @RequestBody CreateExerciseRequest request,
                                           @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return exerciseService.createExercise(request, currentUser);
    }

    @GetMapping
    public List<ExerciseResponse> getAllExercises(@RequestParam(required = false) String name,
                                                  @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        if (name != null && !name.isBlank()) {
            return exerciseService.searchExercises(name, currentUser);
        }

        return exerciseService.getAllExercises(currentUser);
    }

    @PutMapping("/{id}")
    public ExerciseResponse updateExercise(@PathVariable Long id,
                                           @Valid @RequestBody UpdateExerciseRequest request,
                                           @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return exerciseService.updateExercise(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    public void deleteExercise(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        exerciseService.deleteExercise(id, currentUser);
    }
}