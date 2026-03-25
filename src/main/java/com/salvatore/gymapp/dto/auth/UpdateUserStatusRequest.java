package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Il campo active è obbligatorio")
    private Boolean active;
}