package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.exercise.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Optional<Exercise> findByGymIdAndNameIgnoreCase(Long gymId, String name);

    boolean existsByGymIdAndNameIgnoreCase(Long gymId, String name);

    List<Exercise> findByGymIdOrderByNameAsc(Long gymId);

    List<Exercise> findByGymIdAndNameContainingIgnoreCaseOrderByNameAsc(Long gymId, String name);

    long countByGymId(Long gymId);
}