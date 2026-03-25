package com.salvatore.gymapp.dto.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateWorkoutPlanRequest {

    @NotNull(message = "L'userId è obbligatorio")
    private Long userId;

    @NotBlank(message = "Il titolo è obbligatorio")
    private String title;

    @Valid
    @NotEmpty(message = "La scheda deve contenere almeno un giorno")
    private List<WorkoutDayRequest> days;
}