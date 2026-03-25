package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGymWithManagerRequest {

    @NotBlank(message = "Il nome palestra è obbligatorio")
    private String gymName;

    private String city;

    @NotBlank(message = "Il nome manager è obbligatorio")
    private String managerFirstName;

    @NotBlank(message = "Il cognome manager è obbligatorio")
    private String managerLastName;

    @NotBlank(message = "L'email manager è obbligatoria")
    @Email(message = "Email manager non valida")
    private String managerEmail;

    @NotBlank(message = "La password manager è obbligatoria")
    private String managerPassword;
}