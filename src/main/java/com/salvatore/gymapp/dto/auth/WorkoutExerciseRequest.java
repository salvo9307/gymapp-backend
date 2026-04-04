package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkoutExerciseRequest {

    @NotNull(message = "L'exerciseId è obbligatorio")
    private Long exerciseId;

    @NotNull(message = "L'exerciseOrder è obbligatorio")
    private Integer exerciseOrder;

    @NotNull(message = "Le serie sono obbligatorie")
    private Integer sets;

    @NotNull(message = "Le reps sono obbligatorie")
    private String reps;

    private Integer restSeconds;
}