package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.UpdateExerciseWeightRequest;
import com.salvatore.gymapp.dto.auth.UserWorkoutDayResponse;
import com.salvatore.gymapp.dto.auth.UserWorkoutExerciseResponse;
import com.salvatore.gymapp.dto.auth.UserWorkoutPlanResponse;
import com.salvatore.gymapp.entity.ExerciseLog;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.entity.WorkoutDay;
import com.salvatore.gymapp.entity.WorkoutDayExercise;
import com.salvatore.gymapp.entity.WorkoutPlan;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.ExerciseLogRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.repository.WorkoutDayExerciseRepository;
import com.salvatore.gymapp.repository.WorkoutDayRepository;
import com.salvatore.gymapp.repository.WorkoutPlanRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserWorkoutService {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final WorkoutDayRepository workoutDayRepository;
    private final WorkoutDayExerciseRepository workoutDayExerciseRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final SubscriptionService subscriptionService;
    private final GymSubscriptionService gymSubscriptionService;

    public UserWorkoutPlanResponse getMyWorkoutPlan(CustomUserPrincipal currentUser) {
        if (!"USER".equals(currentUser.getRole())) {
            throw new ForbiddenException("Solo gli utenti finali possono vedere la propria scheda");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (user.getGym() == null) {
            throw new ForbiddenException("Utente senza palestra associata");
        }

        if (!gymSubscriptionService.hasValidSubscription(user.getGym().getId())) {
            throw new ForbiddenException("Abbonamento palestra scaduto");
        }

        if (!subscriptionService.hasValidSubscription(user.getId())) {
            throw new ForbiddenException("Abbonamento utente scaduto o non presente");
        }

        WorkoutPlan workoutPlan = workoutPlanRepository.findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> new NotFoundException("Nessuna scheda attiva trovata"));

        List<WorkoutDay> workoutDays = workoutDayRepository
                .findByWorkoutPlanIdOrderByDayOrderAsc(workoutPlan.getId());

        List<UserWorkoutDayResponse> days = workoutDays.stream()
                .map(day -> {
                    List<WorkoutDayExercise> dayExercises = workoutDayExerciseRepository
                            .findByWorkoutDayIdOrderByExerciseOrderAsc(day.getId());

                    List<UserWorkoutExerciseResponse> exercises = dayExercises.stream()
                            .map(dayExercise -> {
                                Double weight = exerciseLogRepository
                                        .findByUserIdAndWorkoutDayExerciseId(user.getId(), dayExercise.getId())
                                        .map(log -> log.getWeight() != null ? log.getWeight().doubleValue() : null)
                                        .orElse(null);

                                return new UserWorkoutExerciseResponse(
                                        dayExercise.getId(),
                                        dayExercise.getExercise().getId(),
                                        dayExercise.getExercise().getName(),
                                        dayExercise.getExerciseOrder(),
                                        dayExercise.getSets(),
                                        dayExercise.getReps(),
                                        weight
                                );
                            })
                            .toList();

                    return new UserWorkoutDayResponse(
                            day.getId(),
                            day.getDayOrder(),
                            day.getTitle(),
                            exercises
                    );
                })
                .toList();

        return new UserWorkoutPlanResponse(
                workoutPlan.getId(),
                workoutPlan.getTitle(),
                days
        );
    }

    public void updateExerciseWeight(Long workoutDayExerciseId,
                                     UpdateExerciseWeightRequest request,
                                     CustomUserPrincipal currentUser) {
        if (!"USER".equals(currentUser.getRole())) {
            throw new ForbiddenException("Solo gli utenti finali possono aggiornare i propri pesi");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        WorkoutPlan activePlan = workoutPlanRepository.findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> new NotFoundException("Nessuna scheda attiva trovata"));

        WorkoutDayExercise workoutDayExercise = workoutDayExerciseRepository.findById(workoutDayExerciseId)
                .orElseThrow(() -> new NotFoundException("Esercizio della scheda non trovato"));

        if (!workoutDayExercise.getWorkoutDay().getWorkoutPlan().getId().equals(activePlan.getId())) {
            throw new ForbiddenException("Non puoi modificare un esercizio che non appartiene alla tua scheda attiva");
        }

        ExerciseLog exerciseLog = exerciseLogRepository
                .findByUserIdAndWorkoutDayExerciseId(user.getId(), workoutDayExerciseId)
                .orElseGet(() -> {
                    ExerciseLog log = new ExerciseLog();
                    log.setUser(user);
                    log.setWorkoutDayExercise(workoutDayExercise);
                    return log;
                });

        exerciseLog.setWeight(
                request.getWeight() != null ? java.math.BigDecimal.valueOf(request.getWeight()) : null
        );
        exerciseLogRepository.save(exerciseLog);
    }
}