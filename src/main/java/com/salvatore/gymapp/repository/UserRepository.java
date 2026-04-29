package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.gym.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u left join fetch u.gym where u.id = :id")
    Optional<User> findByIdWithGym(@Param("id") Long id);

    Optional<User> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);

    @Query("select u from User u left join fetch u.role left join fetch u.gym where u.emailHash = :emailHash")
    Optional<User> findByEmailHashWithRoleAndGym(@Param("emailHash") String emailHash);

    boolean existsByEmailEnc(String emailEnc);

    List<User> findAllByGymIdAndRole_Name(Long gymId, String roleName);

    List<User> findByGymIdAndRole_NameOrderByLastNameAscFirstNameAsc(Long gymId, String roleName);

    long countByGymIdAndRole_Name(Long gymId, String roleName);

    long countByGymIdAndRole_NameAndIsActiveTrue(Long gymId, String roleName);

    long countByRole_Name(String roleName);

    Optional<User> findFirstByGymIdAndRole_Name(Long gymId, String roleName);

    @Query("""
        select u
        from User u
        where u.gym.id = :gymId
          and u.role.name = 'USER'
          and (
               lower(u.firstName) like lower(concat('%', :search, '%'))
            or lower(u.lastName) like lower(concat('%', :search, '%'))
            or lower(u.emailBackup) like lower(concat('%', :search, '%'))
          )
    """)
    Page<User> searchGymUsers(@Param("gymId") Long gymId,
                              @Param("search") String search,
                              Pageable pageable);
}