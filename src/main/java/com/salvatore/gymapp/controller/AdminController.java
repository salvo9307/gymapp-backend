package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.AdminDashboardResponse;
import com.salvatore.gymapp.dto.auth.CreateUserRequest;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.service.AdminDashboardService;
import com.salvatore.gymapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    private final AdminDashboardService adminDashboardService;

    @PostMapping("/users")
    public Long createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return user.getId();
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }


}