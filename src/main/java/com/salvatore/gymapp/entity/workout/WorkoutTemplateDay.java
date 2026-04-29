package com.salvatore.gymapp.entity.workout;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workout_template_days")
@Getter
@Setter
public class WorkoutTemplateDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private WorkoutTemplate template;

    private Integer dayOrder;

    private String title;
}