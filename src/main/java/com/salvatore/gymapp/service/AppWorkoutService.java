package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.app.AppUpdateWeightRequest;
import com.salvatore.gymapp.dto.app.AppWorkoutDayResponse;
import com.salvatore.gymapp.dto.app.AppWorkoutExerciseResponse;
import com.salvatore.gymapp.dto.app.AppWorkoutPlanResponse;
import com.salvatore.gymapp.entity.exercise.ExerciseLog;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.entity.workout.WorkoutDay;
import com.salvatore.gymapp.entity.workout.WorkoutDayExercise;
import com.salvatore.gymapp.entity.workout.WorkoutPlan;
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

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppWorkoutService {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final WorkoutDayRepository workoutDayRepository;
    private final WorkoutDayExerciseRepository workoutDayExerciseRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final SubscriptionService subscriptionService;
    private final GymSubscriptionService gymSubscriptionService;

    public AppWorkoutPlanResponse getMyWorkoutPlan(CustomUserPrincipal currentUser) {
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
                .orElseThrow(() -> new NotFoundException("Scheda attiva non trovata"));

        List<WorkoutDay> workoutDays = workoutDayRepository
                .findByWorkoutPlanIdOrderByDayOrderAsc(workoutPlan.getId());

        List<AppWorkoutDayResponse> days = workoutDays.stream()
                .map(day -> {
                    List<WorkoutDayExercise> dayExercises = workoutDayExerciseRepository
                            .findByWorkoutDayIdOrderByExerciseOrderAsc(day.getId());

                    List<AppWorkoutExerciseResponse> exercises = dayExercises.stream()
                            .map(dayExercise -> {
                                Double lastWeight = exerciseLogRepository
                                        .findByUserIdAndWorkoutDayExerciseId(user.getId(), dayExercise.getId())
                                        .map(log -> log.getWeight() != null ? log.getWeight().doubleValue() : null)
                                        .orElse(null);

                                return new AppWorkoutExerciseResponse(
                                        dayExercise.getId(),
                                        dayExercise.getExercise().getId(),
                                        dayExercise.getExercise().getName(),
                                        dayExercise.getExerciseOrder(),
                                        dayExercise.getSets(),
                                        dayExercise.getReps(),
                                        lastWeight
                                );
                            })
                            .toList();

                    return new AppWorkoutDayResponse(
                            day.getId(),
                            day.getDayOrder(),
                            day.getTitle(),
                            exercises
                    );
                })
                .toList();

        return new AppWorkoutPlanResponse(
                workoutPlan.getId(),
                workoutPlan.getTitle(),
                days
        );
    }

    public void updateMyWeight(Long workoutDayExerciseId,
                               AppUpdateWeightRequest request,
                               CustomUserPrincipal currentUser) {
        if (!"USER".equals(currentUser.getRole())) {
            throw new ForbiddenException("Solo gli utenti finali possono aggiornare i propri pesi");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        WorkoutPlan activePlan = workoutPlanRepository.findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> new NotFoundException("Scheda attiva non trovata"));

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
                request.getWeight() != null ? BigDecimal.valueOf(request.getWeight()) : null
        );

        exerciseLogRepository.save(exerciseLog);
    }
}