package com.salvatore.gymapp.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RenewSubscriptionRequest {

    @Min(1)
    @Max(12)
    private int months;

    @NotNull
    private LocalDate startDate;
}