package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGymStatusRequest {

    @NotNull
    private Boolean active;
}