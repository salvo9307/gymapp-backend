package com.salvatore.gymapp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Setter
@Getter
@Table(name = "gym_subscriptions")
public class GymSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active = true;
}
