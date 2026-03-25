package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.*;
import com.salvatore.gymapp.entity.Gym;
import com.salvatore.gymapp.entity.Role;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.entity.WorkoutPlan;
import com.salvatore.gymapp.exception.BadRequestException;
import com.salvatore.gymapp.exception.ConflictException;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.GymRepository;
import com.salvatore.gymapp.repository.RoleRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.repository.WorkoutPlanRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final SubscriptionService subscriptionService;


    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email già presente");
        }

        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Ruolo non valido"));

        Gym gym = null;
        if (request.getGymId() != null) {
            gym = gymRepository.findById(request.getGymId())
                    .orElseThrow(() -> new NotFoundException("Palestra non trovata"));
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setGym(gym);
        user.setActive(true);

        return userRepository.save(user);
    }

    public List<UserSummaryResponse> getUsersForManager(CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        if (manager.getGym() == null) {
            throw new RuntimeException("Manager senza palestra");
        }

        return userRepository.findByGymIdAndRole_NameOrderByLastNameAscFirstNameAsc(
                        manager.getGym().getId(),
                        "USER"
                )
                .stream()
                .map(user -> {
                    WorkoutPlan latestPlan = workoutPlanRepository
                            .findByUserIdAndActiveTrue(user.getId())
                            .orElse(null);

                    LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());
                    String subscriptionStatus = getSubscriptionStatus(endDate);
                    boolean isActive = "ACTIVE".equals(subscriptionStatus) || "EXPIRING".equals(subscriptionStatus);

                    return new UserSummaryResponse(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getGym() != null ? user.getGym().getId() : null,
                            isActive, // 🔥 NON user.isActive()
                            latestPlan != null,
                            latestPlan != null ? latestPlan.getId() : null,
                            latestPlan != null ? latestPlan.getTitle() : null,
                            endDate,
                            subscriptionStatus
                    );
                })
                .toList();
    }


    public UserDetailResponse getUserDetailForManager(Long userId, CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (manager.getGym() == null || user.getGym() == null) {
            throw new RuntimeException("Palestra non associata");
        }

        if (!manager.getGym().getId().equals(user.getGym().getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        WorkoutPlan latestPlan = workoutPlanRepository
                .findByUserIdAndActiveTrue(user.getId())
                .orElse(null);

        boolean isActive = subscriptionService.hasValidSubscription(user.getId());

        return new UserDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().getName(),
                user.getGym().getId(),
                isActive, // 🔥 qui cambia tutto
                latestPlan != null,
                latestPlan != null ? latestPlan.getId() : null,
                latestPlan != null ? latestPlan.getTitle() : null,
                subscriptionService.getSubscriptionEndDate(user.getId()) // 🔥 NEW
        );
    }


    public PagedResponse<UserSummaryResponse> searchUsersForManager(CustomUserPrincipal currentUser,
                                                                    String search,
                                                                    int page,
                                                                    int size) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        if (manager.getGym() == null) {
            throw new ForbiddenException("Manager senza palestra");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<User> usersPage = userRepository.searchGymUsers(
                manager.getGym().getId(),
                search == null ? "" : search,
                pageable
        );

        List<UserSummaryResponse> content = usersPage.getContent()
                .stream()
                .map(user -> {
                    WorkoutPlan latestPlan = workoutPlanRepository
                            .findByUserIdAndActiveTrue(user.getId())
                            .orElse(null);
                    LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());
                    String subscriptionStatus = getSubscriptionStatus(endDate);
                    boolean isActive = "ACTIVE".equals(subscriptionStatus) || "EXPIRING".equals(subscriptionStatus);

                    return new UserSummaryResponse(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getGym() != null ? user.getGym().getId() : null,
                            user.isActive(),
                            latestPlan != null,
                            latestPlan != null ? latestPlan.getId() : null,
                            latestPlan != null ? latestPlan.getTitle() : null,
                            endDate,
                            subscriptionStatus
                    );
                })
                .toList();

        return new PagedResponse<>(
                content,
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages()
        );
    }

    public UserDetailResponse createUserForManager(CreateManagedUserRequest request, CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        if (manager.getGym() == null) {
            throw new ForbiddenException("Manager senza palestra associata");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email già presente");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Ruolo USER non trovato"));

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setGym(manager.getGym());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        return new UserDetailResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole().getName(),
                savedUser.getGym().getId(),
                false, // 🔥 nessun abbonamento
                false,
                null,
                null,
                null // subscriptionEndDate
        );
    }

    public void resetUserPasswordForManager(Long userId,
                                            ResetUserPasswordRequest request,
                                            CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        if (manager.getGym() == null) {
            throw new ForbiddenException("Manager senza palestra associata");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (user.getGym() == null || !user.getGym().getId().equals(manager.getGym().getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        if (!"USER".equals(user.getRole().getName())) {
            throw new ForbiddenException("Puoi resettare solo password di utenti finali");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }


    public void updateUserStatusForManager(Long userId,
                                           UpdateUserStatusRequest request,
                                           CustomUserPrincipal currentUser) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (targetUser.getRole() == null || !"USER".equals(targetUser.getRole().getName())) {
            throw new ForbiddenException("Puoi modificare lo stato solo degli utenti USER");
        }

        String role = currentUser.getRole();

        if ("ADMIN".equals(role)) {
            targetUser.setActive(request.getActive());
            userRepository.save(targetUser);
            return;
        }

        if ("MANAGER".equals(role)) {
            User manager = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Manager non trovato"));

            if (manager.getGym() == null || targetUser.getGym() == null) {
                throw new ForbiddenException("Palestra non associata");
            }

            if (!manager.getGym().getId().equals(targetUser.getGym().getId())) {
                throw new ForbiddenException("Non autorizzato");
            }

            targetUser.setActive(request.getActive());
            userRepository.save(targetUser);
            return;
        }

        throw new ForbiddenException("Non autorizzato");
    }

    private String getSubscriptionStatus(LocalDate endDate) {
        if (endDate == null) {
            return "NONE";
        }

        LocalDate today = LocalDate.now();

        if (endDate.isBefore(today.minusDays(2))) {
            return "EXPIRED";
        }

        if (!endDate.isAfter(today.plusDays(5))) {
            return "EXPIRING";
        }

        return "ACTIVE";
    }
}