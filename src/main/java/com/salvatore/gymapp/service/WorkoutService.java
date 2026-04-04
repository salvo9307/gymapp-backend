package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.CreateWorkoutPlanRequest;
import com.salvatore.gymapp.dto.auth.WorkoutDayRequest;
import com.salvatore.gymapp.dto.auth.WorkoutDayResponse;
import com.salvatore.gymapp.dto.auth.WorkoutExerciseRequest;
import com.salvatore.gymapp.dto.auth.WorkoutExerciseResponse;
import com.salvatore.gymapp.dto.auth.WorkoutPlanResponse;
import com.salvatore.gymapp.entity.Exercise;
import com.salvatore.gymapp.entity.ExerciseLog;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.entity.WorkoutDay;
import com.salvatore.gymapp.entity.WorkoutDayExercise;
import com.salvatore.gymapp.entity.WorkoutPlan;
import com.salvatore.gymapp.exception.BadRequestException;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.ExerciseLogRepository;
import com.salvatore.gymapp.repository.ExerciseRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.repository.WorkoutDayExerciseRepository;
import com.salvatore.gymapp.repository.WorkoutDayRepository;
import com.salvatore.gymapp.repository.WorkoutPlanRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final WorkoutDayRepository workoutDayRepository;
    private final WorkoutDayExerciseRepository workoutDayExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public WorkoutPlan createWorkoutPlan(CreateWorkoutPlanRequest request, CustomUserPrincipal currentUser) {
        validateWorkoutPlanRequest(request);

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));

        checkCanManageUser(currentUser, targetUser);
        ensureTargetUserIsUser(targetUser);

        workoutPlanRepository.deactivateAllByUserId(targetUser.getId());

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUser(targetUser);
        plan.setTitle(request.getTitle().trim());
        plan.setCreatedBy(creator);
        plan.setActive(true);
        plan = workoutPlanRepository.save(plan);

        for (WorkoutDayRequest dayRequest : request.getDays()) {
            WorkoutDay day = new WorkoutDay();
            day.setWorkoutPlan(plan);
            day.setDayOrder(dayRequest.getDayOrder());
            day.setTitle(dayRequest.getTitle());
            day = workoutDayRepository.save(day);

            if (dayRequest.getExercises() == null || dayRequest.getExercises().isEmpty()) {
                continue;
            }

            for (WorkoutExerciseRequest exerciseRequest : dayRequest.getExercises()) {
                Exercise exercise = exerciseRepository.findById(exerciseRequest.getExerciseId())
                        .orElseThrow(() -> new NotFoundException("Esercizio non trovato"));

                checkExerciseBelongsToGym(exercise, targetUser);

                WorkoutDayExercise wde = new WorkoutDayExercise();
                wde.setWorkoutDay(day);
                wde.setExercise(exercise);
                wde.setExerciseOrder(exerciseRequest.getExerciseOrder());
                wde.setSets(exerciseRequest.getSets());
                wde.setReps(exerciseRequest.getReps());
                wde.setRestSeconds(exerciseRequest.getRestSeconds());

                workoutDayExerciseRepository.save(wde);
            }
        }

        return plan;
    }

    @Transactional
    public WorkoutPlanResponse duplicateWorkoutPlan(Long workoutPlanId, CustomUserPrincipal currentUser) {
        WorkoutPlan originalPlan = workoutPlanRepository.findById(workoutPlanId)
                .orElseThrow(() -> new NotFoundException("Scheda non trovata"));

        User targetUser = originalPlan.getUser();
        checkCanManageUser(currentUser, targetUser);
        ensureTargetUserIsUser(targetUser);

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));

        workoutPlanRepository.deactivateAllByUserId(targetUser.getId());

        WorkoutPlan newPlan = new WorkoutPlan();
        newPlan.setUser(targetUser);
        newPlan.setTitle(buildDuplicateTitle(originalPlan.getTitle()));
        newPlan.setCreatedBy(creator);
        newPlan.setActive(true);
        newPlan = workoutPlanRepository.save(newPlan);

        List<WorkoutDay> originalDays = workoutDayRepository.findByWorkoutPlanIdOrderByDayOrderAsc(originalPlan.getId());

        for (WorkoutDay originalDay : originalDays) {
            WorkoutDay newDay = new WorkoutDay();
            newDay.setWorkoutPlan(newPlan);
            newDay.setDayOrder(originalDay.getDayOrder());
            newDay.setTitle(originalDay.getTitle());
            newDay = workoutDayRepository.save(newDay);

            List<WorkoutDayExercise> originalExercises =
                    workoutDayExerciseRepository.findByWorkoutDayIdOrderByExerciseOrderAsc(originalDay.getId());

            for (WorkoutDayExercise originalExercise : originalExercises) {
                checkExerciseBelongsToGym(originalExercise.getExercise(), targetUser);

                WorkoutDayExercise newExercise = new WorkoutDayExercise();
                newExercise.setWorkoutDay(newDay);
                newExercise.setExercise(originalExercise.getExercise());
                newExercise.setExerciseOrder(originalExercise.getExerciseOrder());
                newExercise.setSets(originalExercise.getSets());
                newExercise.setReps(originalExercise.getReps());
                newExercise.setRestSeconds(originalExercise.getRestSeconds());

                workoutDayExerciseRepository.save(newExercise);
            }
        }

        return buildWorkoutPlanResponse(newPlan, targetUser.getId());
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse getMyLatestWorkoutPlan(CustomUserPrincipal currentUser) {
        WorkoutPlan plan = workoutPlanRepository.findByUserIdAndActiveTrue(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Scheda attiva non trovata"));

        return buildWorkoutPlanResponse(plan, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public WorkoutPlanResponse getWorkoutPlanForUser(Long userId, CustomUserPrincipal currentUser) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        checkCanManageUser(currentUser, targetUser);

        WorkoutPlan plan = workoutPlanRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException("Scheda attiva non trovata"));

        return buildWorkoutPlanResponse(plan, userId);
    }

    @Transactional
    public WorkoutPlan updateWorkoutPlan(Long workoutPlanId, CreateWorkoutPlanRequest request, CustomUserPrincipal currentUser) {
        validateWorkoutPlanRequest(request);

        WorkoutPlan plan = workoutPlanRepository.findById(workoutPlanId)
                .orElseThrow(() -> new NotFoundException("Scheda non trovata"));

        User targetUser = plan.getUser();
        checkCanManageUser(currentUser, targetUser);
        ensureTargetUserIsUser(targetUser);

        User updater = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));

        plan.setTitle(request.getTitle().trim());
        plan.setUpdatedBy(updater);
        plan = workoutPlanRepository.save(plan);

        List<WorkoutDay> existingDays = workoutDayRepository.findByWorkoutPlanIdOrderByDayOrderAsc(plan.getId());
        workoutDayRepository.deleteAll(existingDays);

        for (WorkoutDayRequest dayRequest : request.getDays()) {
            WorkoutDay day = new WorkoutDay();
            day.setWorkoutPlan(plan);
            day.setDayOrder(dayRequest.getDayOrder());
            day.setTitle(dayRequest.getTitle());
            day = workoutDayRepository.save(day);

            if (dayRequest.getExercises() == null || dayRequest.getExercises().isEmpty()) {
                continue;
            }

            for (WorkoutExerciseRequest exerciseRequest : dayRequest.getExercises()) {
                Exercise exercise = exerciseRepository.findById(exerciseRequest.getExerciseId())
                        .orElseThrow(() -> new NotFoundException("Esercizio non trovato"));

                checkExerciseBelongsToGym(exercise, targetUser);

                WorkoutDayExercise wde = new WorkoutDayExercise();
                wde.setWorkoutDay(day);
                wde.setExercise(exercise);
                wde.setExerciseOrder(exerciseRequest.getExerciseOrder());
                wde.setSets(exerciseRequest.getSets());
                wde.setReps(exerciseRequest.getReps());
                wde.setRestSeconds(exerciseRequest.getRestSeconds());

                workoutDayExerciseRepository.save(wde);
            }
        }

        return plan;
    }

    @Transactional
    public void updateMyWeight(Long workoutDayExerciseId, BigDecimal weight, CustomUserPrincipal currentUser) {
        WorkoutDayExercise wde = workoutDayExerciseRepository.findById(workoutDayExerciseId)
                .orElseThrow(() -> new NotFoundException("Esercizio scheda non trovato"));

        ExerciseLog log = exerciseLogRepository
                .findByUserIdAndWorkoutDayExerciseId(currentUser.getId(), workoutDayExerciseId)
                .orElseGet(() -> {
                    User user = userRepository.findById(currentUser.getId())
                            .orElseThrow(() -> new NotFoundException("Utente non trovato"));

                    ExerciseLog newLog = new ExerciseLog();
                    newLog.setUser(user);
                    newLog.setWorkoutDayExercise(wde);
                    return newLog;
                });

        log.setWeight(weight);
        exerciseLogRepository.save(log);
    }

    private WorkoutPlanResponse buildWorkoutPlanResponse(WorkoutPlan plan, Long userId) {

        List<WorkoutDay> days =
                workoutDayRepository.findByWorkoutPlanIdOrderByDayOrderAsc(plan.getId());

        List<WorkoutDayResponse> dayResponses = new ArrayList<>();

        for (WorkoutDay day : days) {

            List<WorkoutDayExercise> exercises =
                    workoutDayExerciseRepository
                            .findByWorkoutDayIdOrderByExerciseOrderAsc(day.getId());

            List<WorkoutExerciseResponse> exerciseResponses = new ArrayList<>();

            for (WorkoutDayExercise wde : exercises) {

                BigDecimal weight =
                        exerciseLogRepository
                                .findByUserIdAndWorkoutDayExerciseId(userId, wde.getId())
                                .map(ExerciseLog::getWeight)
                                .orElse(null);

                exerciseResponses.add(
                        new WorkoutExerciseResponse(
                                wde.getId(),
                                wde.getExercise().getName(),
                                wde.getExerciseOrder(),
                                wde.getSets(),
                                wde.getReps(),
                                wde.getRestSeconds(),
                                weight
                        )
                );
            }

            dayResponses.add(
                    new WorkoutDayResponse(
                            day.getId(),
                            day.getDayOrder(),
                            day.getTitle(),
                            exerciseResponses
                    )
            );
        }
        LocalDate subscriptionEndDate =
                subscriptionService.getSubscriptionEndDate(userId);

        return new WorkoutPlanResponse(
                plan.getId(),
                plan.getTitle(),
                dayResponses,
                subscriptionEndDate
        );
    }

    private void checkCanManageUser(CustomUserPrincipal currentUser, User targetUser) {
        String role = currentUser.getRole();

        if ("ADMIN".equals(role)) {
            return;
        }

        if ("MANAGER".equals(role)) {
            User manager = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Manager non trovato"));

            if (manager.getGym() == null || targetUser.getGym() == null) {
                throw new ForbiddenException("Palestra non associata");
            }

            if (!manager.getGym().getId().equals(targetUser.getGym().getId())) {
                throw new ForbiddenException("Non autorizzato");
            }

            return;
        }

        throw new ForbiddenException("Non autorizzato");
    }

    private void checkExerciseBelongsToGym(Exercise exercise, User user) {
        if (user.getGym() == null || exercise.getGym() == null ||
                !exercise.getGym().getId().equals(user.getGym().getId())) {
            throw new ForbiddenException("Non puoi usare un esercizio di un'altra palestra");
        }
    }

    private void ensureTargetUserIsUser(User targetUser) {
        if (targetUser.getRole() == null || !"USER".equals(targetUser.getRole().getName())) {
            throw new BadRequestException("La scheda può essere gestita solo per utenti con ruolo USER");
        }
    }

    private void validateWorkoutPlanRequest(CreateWorkoutPlanRequest request) {
        if (request == null) {
            throw new BadRequestException("Richiesta non valida");
        }

        if (request.getUserId() == null) {
            throw new BadRequestException("UserId obbligatorio");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Titolo obbligatorio");
        }

        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BadRequestException("La scheda deve contenere almeno un giorno");
        }
    }

    private String buildDuplicateTitle(String originalTitle) {
        String suffix = " (copia)";
        String baseTitle = originalTitle == null ? "Scheda" : originalTitle.trim();

        int maxLength = 150;
        if (baseTitle.length() + suffix.length() <= maxLength) {
            return baseTitle + suffix;
        }

        return baseTitle.substring(0, maxLength - suffix.length()) + suffix;
    }
}