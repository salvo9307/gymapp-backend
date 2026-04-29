package com.salvatore.gymapp.entity.workout;

import com.salvatore.gymapp.entity.exercise.Exercise;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workout_day_exercises")
@Getter
@Setter
public class WorkoutDayExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_day_id", nullable = false)
    private WorkoutDay workoutDay;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "exercise_order", nullable = false)
    private Integer exerciseOrder;

    @Column
    private Integer sets;

    @Column(length = 50)
    private String reps;

    @Column(name = "rest_seconds")
    private Integer restSeconds;
}