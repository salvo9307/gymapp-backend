package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutTemplateSummaryResponse {
    private Long id;
    private String title;
}