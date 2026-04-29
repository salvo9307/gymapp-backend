package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutTemplateResponse {
    private Long id;
    private String title;
    private List<WorkoutTemplateDayResponse> days;
}