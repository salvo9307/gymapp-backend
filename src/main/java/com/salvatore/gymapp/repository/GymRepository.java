package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymRepository extends JpaRepository<Gym, Long> {
}