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
public class WorkoutDayRequest {

    @NotNull(message = "Il dayOrder è obbligatorio")
    private Integer dayOrder;

    @NotBlank(message = "Il titolo del giorno è obbligatorio")
    private String title;

    @Valid
    @NotEmpty(message = "Il giorno deve contenere almeno un esercizio")
    private List<WorkoutExerciseRequest> exercises;
}