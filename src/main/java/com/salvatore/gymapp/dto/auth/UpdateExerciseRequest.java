package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateExerciseRequest {

    @NotBlank(message = "Il nome esercizio è obbligatorio")
    private String name;
}