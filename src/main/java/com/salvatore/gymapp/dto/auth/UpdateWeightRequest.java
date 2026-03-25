package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateWeightRequest {

    @NotNull(message = "Il peso è obbligatorio")
    private BigDecimal weight;
}