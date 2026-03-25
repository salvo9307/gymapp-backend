package com.salvatore.gymapp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordHashGenerator {

    @Bean
    public CommandLineRunner generateHashes(PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("admin123 -> " + passwordEncoder.encode("admin123"));
            System.out.println("manager123 -> " + passwordEncoder.encode("manager123"));
            System.out.println("user123 -> " + passwordEncoder.encode("user123"));
        };
    }
}