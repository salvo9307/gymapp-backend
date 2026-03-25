package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetUserPasswordRequest {

    @NotBlank(message = "La nuova password è obbligatoria")
    private String newPassword;
}