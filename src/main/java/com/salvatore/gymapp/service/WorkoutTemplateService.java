package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.*;
import com.salvatore.gymapp.entity.exercise.Exercise;
import com.salvatore.gymapp.entity.gym.Gym;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.entity.workout.WorkoutTemplate;
import com.salvatore.gymapp.entity.workout.WorkoutTemplateDay;
import com.salvatore.gymapp.entity.workout.WorkoutTemplateExercise;
import com.salvatore.gymapp.exception.BadRequestException;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.*;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class WorkoutTemplateService {

    private final WorkoutTemplateRepository templateRepository;
    private final WorkoutTemplateDayRepository dayRepository;
    private final WorkoutTemplateExerciseRepository templateExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final WorkoutService workoutService;

    @Transactional(readOnly = true)
    public List<WorkoutTemplateSummaryResponse> getTemplates(CustomUserPrincipal currentUser) {
        Gym gym = getCurrentManagerGym(currentUser);

        return templateRepository.findByGymIdOrderByTitleAsc(gym.getId())
                .stream()
                .map(template -> new WorkoutTemplateSummaryResponse(
                        template.getId(),
                        template.getTitle()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutTemplateResponse getTemplate(Long templateId, CustomUserPrincipal currentUser) {
        Gym gym = getCurrentManagerGym(currentUser);
        WorkoutTemplate template = getTemplateForGym(templateId, gym.getId());

        return buildTemplateResponse(template);
    }

    @Transactional
    public Long createTemplate(CreateWorkoutPlanRequest request, CustomUserPrincipal currentUser) {
        validateRequest(request);

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));

        Gym gym = getCurrentManagerGym(currentUser);

        WorkoutTemplate template = new WorkoutTemplate();
        template.setTitle(request.getTitle().trim());
        template.setGym(gym);
        template.setCreatedBy(creator);
        template = templateRepository.save(template);

        saveDaysAndExercises(template, request, gym);

        return template.getId();
    }

    @Transactional
    public Long updateTemplate(Long templateId,
                               CreateWorkoutPlanRequest request,
                               CustomUserPrincipal currentUser) {
        validateRequest(request);

        Gym gym = getCurrentManagerGym(currentUser);
        WorkoutTemplate template = getTemplateForGym(templateId, gym.getId());

        template.setTitle(request.getTitle().trim());
        template = templateRepository.save(template);

        List<WorkoutTemplateDay> oldDays = dayRepository.findByTemplateIdOrderByDayOrderAsc(template.getId());

        for (WorkoutTemplateDay oldDay : oldDays) {
            templateExerciseRepository.deleteByTemplateDayId(oldDay.getId());
        }

        dayRepository.deleteAll(oldDays);
        dayRepository.flush();

        saveDaysAndExercises(template, request, gym);

        return template.getId();
    }

    @Transactional
    public void deleteTemplate(Long templateId, CustomUserPrincipal currentUser) {
        Gym gym = getCurrentManagerGym(currentUser);
        WorkoutTemplate template = getTemplateForGym(templateId, gym.getId());

        List<WorkoutTemplateDay> days = dayRepository.findByTemplateIdOrderByDayOrderAsc(template.getId());

        for (WorkoutTemplateDay day : days) {
            templateExerciseRepository.deleteByTemplateDayId(day.getId());
        }

        dayRepository.deleteAll(days);
        templateRepository.delete(template);
    }

    @Transactional
    public Long applyTemplate(Long templateId, Long userId, CustomUserPrincipal currentUser) {
        Gym gym = getCurrentManagerGym(currentUser);
        WorkoutTemplate template = getTemplateForGym(templateId, gym.getId());

        CreateWorkoutPlanRequest request = new CreateWorkoutPlanRequest();
        request.setUserId(userId);
        request.setTitle(template.getTitle());

        List<WorkoutDayRequest> dayRequests = new ArrayList<>();

        List<WorkoutTemplateDay> days = dayRepository.findByTemplateIdOrderByDayOrderAsc(template.getId());

        for (WorkoutTemplateDay day : days) {
            WorkoutDayRequest dayRequest = new WorkoutDayRequest();
            dayRequest.setDayOrder(day.getDayOrder());
            dayRequest.setTitle(day.getTitle());

            List<WorkoutExerciseRequest> exerciseRequests = new ArrayList<>();

            List<WorkoutTemplateExercise> exercises =
                    templateExerciseRepository.findByTemplateDayIdOrderByExerciseOrderAsc(day.getId());

            for (WorkoutTemplateExercise templateExercise : exercises) {
                WorkoutExerciseRequest exerciseRequest = new WorkoutExerciseRequest();
                exerciseRequest.setExerciseId(templateExercise.getExercise().getId());
                exerciseRequest.setExerciseOrder(templateExercise.getExerciseOrder());
                exerciseRequest.setSets(templateExercise.getSets());
                exerciseRequest.setReps(templateExercise.getReps());
                exerciseRequest.setRestSeconds(templateExercise.getRestSeconds());

                exerciseRequests.add(exerciseRequest);
            }

            dayRequest.setExercises(exerciseRequests);
            dayRequests.add(dayRequest);
        }

        request.setDays(dayRequests);

        return workoutService.createWorkoutPlan(request, currentUser).getId();
    }

    private void saveDaysAndExercises(WorkoutTemplate template,
                                      CreateWorkoutPlanRequest request,
                                      Gym gym) {
        for (WorkoutDayRequest dayRequest : request.getDays()) {
            WorkoutTemplateDay day = new WorkoutTemplateDay();
            day.setTemplate(template);
            day.setDayOrder(dayRequest.getDayOrder());
            day.setTitle(dayRequest.getTitle().trim());
            day = dayRepository.save(day);

            for (WorkoutExerciseRequest exerciseRequest : dayRequest.getExercises()) {
                Exercise exercise = exerciseRepository.findById(exerciseRequest.getExerciseId())
                        .orElseThrow(() -> new NotFoundException("Esercizio non trovato"));

                if (exercise.getGym() == null || !exercise.getGym().getId().equals(gym.getId())) {
                    throw new ForbiddenException("Non puoi usare un esercizio di un'altra palestra");
                }

                WorkoutTemplateExercise templateExercise = new WorkoutTemplateExercise();
                templateExercise.setTemplateDay(day);
                templateExercise.setExercise(exercise);
                templateExercise.setExerciseOrder(exerciseRequest.getExerciseOrder());
                templateExercise.setSets(exerciseRequest.getSets());
                templateExercise.setReps(exerciseRequest.getReps());
                templateExercise.setRestSeconds(exerciseRequest.getRestSeconds());

                templateExerciseRepository.save(templateExercise);
            }
        }
    }

    private WorkoutTemplateResponse buildTemplateResponse(WorkoutTemplate template) {
        List<WorkoutTemplateDay> days = dayRepository.findByTemplateIdOrderByDayOrderAsc(template.getId());
        List<WorkoutTemplateDayResponse> dayResponses = new ArrayList<>();

        for (WorkoutTemplateDay day : days) {
            List<WorkoutTemplateExercise> exercises =
                    templateExerciseRepository.findByTemplateDayIdOrderByExerciseOrderAsc(day.getId());

            List<WorkoutTemplateExerciseResponse> exerciseResponses = new ArrayList<>();

            for (WorkoutTemplateExercise exercise : exercises) {
                exerciseResponses.add(new WorkoutTemplateExerciseResponse(
                        exercise.getId(),
                        exercise.getExercise().getId(),
                        exercise.getExercise().getName(),
                        exercise.getExerciseOrder(),
                        exercise.getSets(),
                        exercise.getReps(),
                        exercise.getRestSeconds()
                ));
            }

            dayResponses.add(new WorkoutTemplateDayResponse(
                    day.getId(),
                    day.getDayOrder(),
                    day.getTitle(),
                    exerciseResponses
            ));
        }

        return new WorkoutTemplateResponse(
                template.getId(),
                template.getTitle(),
                dayResponses
        );
    }

    private WorkoutTemplate getTemplateForGym(Long templateId, Long gymId) {
        WorkoutTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Template non trovato"));

        if (template.getGym() == null || !template.getGym().getId().equals(gymId)) {
            throw new ForbiddenException("Template non autorizzato");
        }

        return template;
    }

    private Gym getCurrentManagerGym(CustomUserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));

        if (user.getGym() == null) {
            throw new ForbiddenException("Utente senza palestra associata");
        }

        Long gymId = user.getGym().getId();

        return gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Palestra non trovata"));
    }

    private void validateRequest(CreateWorkoutPlanRequest request) {
        if (request == null) {
            throw new BadRequestException("Richiesta non valida");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Titolo template obbligatorio");
        }

        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BadRequestException("Il template deve contenere almeno un giorno");
        }

        for (WorkoutDayRequest day : request.getDays()) {
            if (day.getTitle() == null || day.getTitle().isBlank()) {
                throw new BadRequestException("Ogni giornata deve avere un titolo");
            }

            if (day.getExercises() == null || day.getExercises().isEmpty()) {
                throw new BadRequestException("Ogni giornata deve contenere almeno un esercizio");
            }

            for (WorkoutExerciseRequest exercise : day.getExercises()) {
                if (exercise.getExerciseId() == null) {
                    throw new BadRequestException("Esercizio obbligatorio");
                }

                if (exercise.getSets() == null) {
                    throw new BadRequestException("Serie obbligatorie");
                }

                if (exercise.getReps() == null || exercise.getReps().isBlank()) {
                    throw new BadRequestException("Ripetizioni obbligatorie");
                }
            }
        }
    }
}