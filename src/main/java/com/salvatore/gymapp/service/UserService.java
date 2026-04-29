package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.CreateManagedUserRequest;
import com.salvatore.gymapp.dto.auth.CreateUserRequest;
import com.salvatore.gymapp.dto.auth.PagedResponse;
import com.salvatore.gymapp.dto.auth.ResetUserPasswordRequest;
import com.salvatore.gymapp.dto.auth.UpdateUserStatusRequest;
import com.salvatore.gymapp.dto.auth.UserDetailResponse;
import com.salvatore.gymapp.dto.auth.UserSummaryResponse;
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
import com.salvatore.gymapp.util.EmailHashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final SubscriptionService subscriptionService;
    private final CryptoService cryptoService;

    public User createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailHash(EmailHashUtils.sha256(email))) {
            throw new ConflictException("Email già presente");
        }

        Role role = roleRepository.findByName(request.getRole().trim().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Ruolo non valido"));

        Gym gym = null;
        if (request.getGymId() != null) {
            gym = gymRepository.findById(request.getGymId())
                    .orElseThrow(() -> new NotFoundException("Palestra non trovata"));
        }

        User user = new User();
        user.setFirstName(normalizeText(request.getFirstName()));
        user.setLastName(normalizeText(request.getLastName()));
        user.setEmailHash(EmailHashUtils.sha256(email));
        user.setEmailEnc(cryptoService.encrypt(email));
        user.setEmailBackup(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setGym(gym);
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(null);

        return userRepository.save(user);
    }

    public List<UserSummaryResponse> getUsersForManager(CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        if (manager.getGym() == null) {
            throw new ForbiddenException("Manager senza palestra");
        }

        return userRepository.findByGymIdAndRole_NameOrderByLastNameAscFirstNameAsc(
                        manager.getGym().getId(),
                        "USER"
                )
                .stream()
                .map(this::mapToUserSummaryResponse)
                .toList();
    }

    public UserDetailResponse getUserDetailForManager(Long userId, CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Manager non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (manager.getGym() == null || user.getGym() == null) {
            throw new ForbiddenException("Palestra non associata");
        }

        if (!manager.getGym().getId().equals(user.getGym().getId())) {
            throw new ForbiddenException("Non autorizzato");
        }

        WorkoutPlan latestPlan = workoutPlanRepository.findByUserIdAndActiveTrue(user.getId()).orElse(null);
        LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());
        String subscriptionStatus = getSubscriptionStatus(endDate);
        boolean effectiveActive = isEffectivelyActive(user, subscriptionStatus);

        return new UserDetailResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                decryptEmailSafely(user),
                user.getRole().getName(),
                user.getGym().getId(),
                effectiveActive,
                latestPlan != null,
                latestPlan != null ? latestPlan.getId() : null,
                latestPlan != null ? latestPlan.getTitle() : null,
                endDate
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
                search == null ? "" : search.trim(),
                pageable
        );

        List<UserSummaryResponse> content = usersPage.getContent()
                .stream()
                .map(this::mapToUserSummaryResponse)
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
        log.info("[CREATE_USER_MANAGER] START principalId={}, role={}, gymIdFromToken={}, email={}",
                currentUser.getId(),
                currentUser.getRole(),
                currentUser.getGymId(),
                safeEmailForLog(request.getEmail())
        );

        try {
            String email = normalizeEmail(request.getEmail());
            String emailHash = EmailHashUtils.sha256(email);

            log.info("[CREATE_USER_MANAGER] Email normalizzata={}, hash={}", safeEmailForLog(email), emailHash);

            User manager = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Manager non trovato"));

            log.info("[CREATE_USER_MANAGER] Manager trovato id={}, emailBackup={}, gymPresent={}",
                    manager.getId(),
                    safeEmailForLog(manager.getEmailBackup()),
                    manager.getGym() != null
            );

            if (manager.getGym() == null) {
                throw new ForbiddenException("Manager senza palestra associata");
            }

            Gym gym = manager.getGym();

            log.info("[CREATE_USER_MANAGER] Gym id={}, name={}, maxUsers={}",
                    gym.getId(),
                    gym.getName(),
                    gym.getMaxUsers()
            );

            Integer maxUsers = gym.getMaxUsers();
            long activeUsersCount = 0;

            log.info("[CREATE_USER_MANAGER] Recupero utenti palestra gymId={}", gym.getId());

            List<User> gymUsers = userRepository.findAllByGymIdAndRole_Name(gym.getId(), "USER");

            log.info("[CREATE_USER_MANAGER] Utenti USER trovati nella palestra={}", gymUsers.size());

            for (User existingUser : gymUsers) {
                LocalDate endDate = subscriptionService.getSubscriptionEndDate(existingUser.getId());
                String subscriptionStatus = getSubscriptionStatus(endDate);

                if (isEffectivelyActive(existingUser, subscriptionStatus)) {
                    activeUsersCount++;
                }
            }

            log.info("[CREATE_USER_MANAGER] Conteggio utenti attivi effettivi={}, maxUsers={}",
                    activeUsersCount,
                    maxUsers
            );

            if (maxUsers != null && activeUsersCount >= maxUsers) {
                throw new BadRequestException("Limite massimo utenti attivi raggiunto per questa palestra");
            }

            log.info("[CREATE_USER_MANAGER] Controllo email duplicata hash={}", emailHash);

            if (userRepository.existsByEmailHash(emailHash)) {
                throw new ConflictException("Email già presente");
            }

            log.info("[CREATE_USER_MANAGER] Recupero ruolo USER");

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new NotFoundException("Ruolo USER non trovato"));

            log.info("[CREATE_USER_MANAGER] Ruolo USER trovato id={}, name={}", userRole.getId(), userRole.getName());

            log.info("[CREATE_USER_MANAGER] Encrypt email");

            String encryptedEmail = cryptoService.encrypt(email);

            log.info("[CREATE_USER_MANAGER] Encode password");

            String encodedPassword = passwordEncoder.encode(request.getPassword());

            User user = new User();
            user.setFirstName(normalizeText(request.getFirstName()));
            user.setLastName(normalizeText(request.getLastName()));
            user.setEmailHash(emailHash);
            user.setEmailEnc(encryptedEmail);
            user.setEmailBackup(email);
            user.setPasswordHash(encodedPassword);
            user.setRole(userRole);
            user.setGym(gym);
            user.setActive(true);
            user.setMustChangePassword(true);
            user.setPasswordChangedAt(null);

            log.info("[CREATE_USER_MANAGER] Salvataggio utente firstName={}, lastName={}, gymId={}, role={}",
                    user.getFirstName(),
                    user.getLastName(),
                    gym.getId(),
                    userRole.getName()
            );

            User savedUser = userRepository.save(user);

            log.info("[CREATE_USER_MANAGER] Utente salvato id={}", savedUser.getId());

            return new UserDetailResponse(
                    savedUser.getId(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    decryptEmailSafely(savedUser),
                    savedUser.getRole().getName(),
                    savedUser.getGym().getId(),
                    false,
                    false,
                    null,
                    null,
                    null
            );

        } catch (DataAccessException ex) {
            log.error("[CREATE_USER_MANAGER] ERRORE DB durante creazione utente. principalId={}, email={}",
                    currentUser.getId(),
                    safeEmailForLog(request.getEmail()),
                    ex
            );
            throw ex;
        } catch (Exception ex) {
            log.error("[CREATE_USER_MANAGER] ERRORE GENERICO durante creazione utente. principalId={}, email={}",
                    currentUser.getId(),
                    safeEmailForLog(request.getEmail()),
                    ex
            );
            throw ex;
        }
    }

    @Transactional
    public void deleteUserForManager(Long userId, CustomUserPrincipal currentUser) {
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
            throw new ForbiddenException("Puoi eliminare solo utenti USER");
        }

        userRepository.delete(user);
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
            throw new ForbiddenException("Puoi resettare solo la password degli utenti USER");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(true);
        user.setPasswordChangedAt(null);

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

    private UserSummaryResponse mapToUserSummaryResponse(User user) {
        WorkoutPlan latestPlan = workoutPlanRepository.findByUserIdAndActiveTrue(user.getId()).orElse(null);

        LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());
        String subscriptionStatus = getSubscriptionStatus(endDate);
        boolean effectiveActive = isEffectivelyActive(user, subscriptionStatus);

        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                decryptEmailSafely(user),
                user.getGym() != null ? user.getGym().getId() : null,
                effectiveActive,
                latestPlan != null,
                latestPlan != null ? latestPlan.getId() : null,
                latestPlan != null ? latestPlan.getTitle() : null,
                endDate,
                subscriptionStatus
        );
    }

    private boolean isEffectivelyActive(User user, String subscriptionStatus) {
        return user.isActive() && ("ACTIVE".equals(subscriptionStatus) || "EXPIRING".equals(subscriptionStatus));
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

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email obbligatoria");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Campo obbligatorio");
        }
        return value.trim();
    }

    private String decryptEmailSafely(User user) {
        if (user.getEmailEnc() != null && !user.getEmailEnc().isBlank()) {
            try {
                return cryptoService.decrypt(user.getEmailEnc());
            } catch (Exception ex) {
                log.warn("[DECRYPT_EMAIL] Impossibile decriptare email userId={}, uso fallback emailBackup",
                        user.getId(),
                        ex
                );
            }
        }
        return user.getEmailBackup();
    }

    private String safeEmailForLog(String email) {
        if (email == null || email.isBlank()) {
            return "null";
        }

        String normalized = email.trim().toLowerCase();
        int atIndex = normalized.indexOf("@");

        if (atIndex <= 1) {
            return "***";
        }

        return normalized.charAt(0) + "***" + normalized.substring(atIndex);
    }
}