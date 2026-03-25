package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.CreateExerciseRequest;
import com.salvatore.gymapp.dto.auth.ExerciseResponse;
import com.salvatore.gymapp.dto.auth.UpdateExerciseRequest;
import com.salvatore.gymapp.entity.Exercise;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.exception.ConflictException;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.ExerciseRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;

    public ExerciseResponse createExercise(CreateExerciseRequest request, CustomUserPrincipal currentUser) {
        User user = getCurrentUser(currentUser);
        Long gymId = getGymId(user);
        String exerciseName = normalizeName(request.getName());

        if (exerciseRepository.existsByGymIdAndNameIgnoreCase(gymId, exerciseName)) {
            throw new ConflictException("Esercizio già presente");
        }

        Exercise exercise = new Exercise();
        exercise.setName(exerciseName);
        exercise.setGym(user.getGym());

        Exercise savedExercise = exerciseRepository.save(exercise);
        return toResponse(savedExercise);
    }

    public List<ExerciseResponse> getAllExercises(CustomUserPrincipal currentUser) {
        User user = getCurrentUser(currentUser);
        Long gymId = getGymId(user);

        return exerciseRepository.findByGymIdOrderByNameAsc(gymId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ExerciseResponse> searchExercises(String name, CustomUserPrincipal currentUser) {
        User user = getCurrentUser(currentUser);
        Long gymId = getGymId(user);

        return exerciseRepository.findByGymIdAndNameContainingIgnoreCaseOrderByNameAsc(gymId, name)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExerciseResponse updateExercise(Long id,
                                           UpdateExerciseRequest request,
                                           CustomUserPrincipal currentUser) {

        User user = getCurrentUser(currentUser);
        Long gymId = getGymId(user);
        String exerciseName = normalizeName(request.getName());

        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Esercizio non trovato"));

        checkExerciseBelongsToGym(exercise, gymId);

        if (exerciseRepository.existsByGymIdAndNameIgnoreCase(gymId, exerciseName)
                && !exercise.getName().equalsIgnoreCase(exerciseName)) {
            throw new ConflictException("Esiste già un esercizio con questo nome");
        }

        exercise.setName(exerciseName);

        Exercise updatedExercise = exerciseRepository.save(exercise);
        return toResponse(updatedExercise);
    }

    public void deleteExercise(Long id, CustomUserPrincipal currentUser) {
        User user = getCurrentUser(currentUser);
        Long gymId = getGymId(user);

        Exercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Esercizio non trovato"));

        checkExerciseBelongsToGym(exercise, gymId);

        exerciseRepository.delete(exercise);
    }

    private User getCurrentUser(CustomUserPrincipal currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
    }

    private Long getGymId(User user) {
        if (user.getGym() == null) {
            throw new ForbiddenException("Utente senza palestra associata");
        }
        return user.getGym().getId();
    }

    private void checkExerciseBelongsToGym(Exercise exercise, Long gymId) {
        if (exercise.getGym() == null || !exercise.getGym().getId().equals(gymId)) {
            throw new ForbiddenException("Non puoi gestire questo esercizio");
        }
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getName());
    }
}