package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.CreateGymRequest;
import com.salvatore.gymapp.dto.auth.CreateGymWithManagerRequest;
import com.salvatore.gymapp.dto.auth.ResetUserPasswordRequest;
import com.salvatore.gymapp.dto.auth.UpdateGymMaxUsersRequest;
import com.salvatore.gymapp.dto.auth.UpdateGymStatusRequest;
import com.salvatore.gymapp.entity.Gym;
import com.salvatore.gymapp.entity.Role;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.exception.BadRequestException;
import com.salvatore.gymapp.exception.ConflictException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.GymRepository;
import com.salvatore.gymapp.repository.RoleRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.util.EmailHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CryptoService cryptoService;

    public Gym createGym(CreateGymRequest request) {
        Gym gym = new Gym();
        gym.setName(normalizeRequiredText(request.getName(), "Nome palestra obbligatorio"));
        gym.setCity(normalizeOptionalText(request.getCity()));
        gym.setMaxUsers(request.getMaxUsers());
        gym.setActive(true);

        return gymRepository.save(gym);
    }

    @Transactional
    public Long createGymWithManager(CreateGymWithManagerRequest request) {
        String managerEmail = normalizeEmail(request.getManagerEmail());

        if (userRepository.existsByEmailHash(EmailHashUtils.sha256(managerEmail))) {
            throw new ConflictException("Email manager già presente");
        }

        Role managerRole = roleRepository.findByName("MANAGER")
                .orElseThrow(() -> new NotFoundException("Ruolo MANAGER non trovato"));

        Gym gym = new Gym();
        gym.setName(normalizeRequiredText(request.getGymName(), "Nome palestra obbligatorio"));
        gym.setCity(normalizeOptionalText(request.getCity()));
        gym.setMaxUsers(request.getMaxUsers());
        gym.setActive(true);

        Gym savedGym = gymRepository.save(gym);

        User manager = new User();
        manager.setFirstName(normalizeRequiredText(request.getManagerFirstName(), "Nome manager obbligatorio"));
        manager.setLastName(normalizeRequiredText(request.getManagerLastName(), "Cognome manager obbligatorio"));
        manager.setEmailHash(EmailHashUtils.sha256(managerEmail));
        manager.setEmailEnc(cryptoService.encrypt(managerEmail));
        manager.setEmailBackup(managerEmail);
        manager.setPasswordHash(passwordEncoder.encode(request.getManagerPassword()));
        manager.setRole(managerRole);
        manager.setGym(savedGym);
        manager.setActive(true);
        manager.setMustChangePassword(true);
        manager.setPasswordChangedAt(null);

        userRepository.save(manager);

        return savedGym.getId();
    }

    public void updateGymStatus(Long gymId, UpdateGymStatusRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Palestra non trovata"));

        gym.setActive(request.getActive());
        gymRepository.save(gym);
    }

    public void resetGymManagerPassword(Long gymId, ResetUserPasswordRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Palestra non trovata"));

        User manager = userRepository.findFirstByGymIdAndRole_Name(gym.getId(), "MANAGER")
                .orElseThrow(() -> new NotFoundException("Manager della palestra non trovato"));

        manager.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        manager.setMustChangePassword(true);
        manager.setPasswordChangedAt(null);

        userRepository.save(manager);
    }

    public void updateGymMaxUsers(Long gymId, UpdateGymMaxUsersRequest request) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new NotFoundException("Palestra non trovata"));

        if (request.getMaxUsers() != null && request.getMaxUsers() < 0) {
            throw new BadRequestException("Il numero massimo utenti non può essere negativo");
        }

        gym.setMaxUsers(request.getMaxUsers());
        gymRepository.save(gym);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email obbligatoria");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}