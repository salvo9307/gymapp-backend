package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGymRequest {

    @NotBlank(message = "Nome obbligatorio")
    private String name;

    private String city;
}