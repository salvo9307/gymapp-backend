package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.CreateGymRequest;
import com.salvatore.gymapp.dto.auth.CreateGymWithManagerRequest;
import com.salvatore.gymapp.dto.auth.ResetUserPasswordRequest;
import com.salvatore.gymapp.dto.auth.UpdateGymStatusRequest;
import com.salvatore.gymapp.entity.Gym;
import com.salvatore.gymapp.entity.Role;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.exception.ConflictException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.GymRepository;
import com.salvatore.gymapp.repository.RoleRepository;
import com.salvatore.gymapp.repository.UserRepository;
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

    public Gym createGym(CreateGymRequest request) {
        Gym gym = new Gym();
        gym.setName(request.getName().trim());
        gym.setCity(request.getCity() != null ? request.getCity().trim() : null);
        gym.setActive(true);

        return gymRepository.save(gym);
    }

    @Transactional
    public Long createGymWithManager(CreateGymWithManagerRequest request) {
        if (userRepository.existsByEmail(request.getManagerEmail().trim())) {
            throw new ConflictException("Email manager già presente");
        }

        Role managerRole = roleRepository.findByName("MANAGER")
                .orElseThrow(() -> new NotFoundException("Ruolo MANAGER non trovato"));

        Gym gym = new Gym();
        gym.setName(request.getGymName().trim());
        gym.setCity(request.getCity() != null ? request.getCity().trim() : null);
        gym.setActive(true);

        Gym savedGym = gymRepository.save(gym);

        User manager = new User();
        manager.setFirstName(request.getManagerFirstName().trim());
        manager.setLastName(request.getManagerLastName().trim());
        manager.setEmail(request.getManagerEmail().trim());
        manager.setPasswordHash(passwordEncoder.encode(request.getManagerPassword()));
        manager.setRole(managerRole);
        manager.setGym(savedGym);
        manager.setActive(true);

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
        userRepository.save(manager);
    }
}