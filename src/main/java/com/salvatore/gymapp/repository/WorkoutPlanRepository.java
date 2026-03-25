package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    Optional<WorkoutPlan> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<WorkoutPlan> findByUserId(Long userId);

    Optional<WorkoutPlan> findByUserIdAndActiveTrue(Long userId);

    long countByUserGymIdAndActiveTrue(Long gymId);

    @Query("""
    select count(distinct wp.user.id)
    from WorkoutPlan wp
    where wp.user.gym.id = :gymId
      and wp.user.role.name = 'USER'
      and wp.active = true
""")
    long countDistinctUsersWithActivePlanByGymId(@Param("gymId") Long gymId);

    @Modifying
    @Query("update WorkoutPlan wp set wp.active = false where wp.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") Long userId);

    Optional<WorkoutPlan> findById(Long id);


}