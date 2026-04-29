package com.salvatore.gymapp.entity.workout;

import com.salvatore.gymapp.entity.exercise.Exercise;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workout_template_exercises")
@Getter
@Setter
public class WorkoutTemplateExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_day_id")
    private WorkoutTemplateDay templateDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    private Integer exerciseOrder;

    private Integer sets;

    private String reps;

    private Integer restSeconds;
}