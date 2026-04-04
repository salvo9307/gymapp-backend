package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGymMaxUsersRequest {

    @Min(value = 0, message = "Il numero massimo utenti non può essere negativo")
    private Integer maxUsers;
}