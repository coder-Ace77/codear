package com.codear.user.service;

import com.codear.user.dto.LoginDTO;
import com.codear.user.dto.RegisterDTO;
import com.codear.user.entity.User;
import com.codear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor 
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User registerUser(RegisterDTO registerDTO) {
        
        userRepository.findByEmail(registerDTO.getEmail())
            .ifPresent(user -> {
                throw new RuntimeException("Email already in use: " + user.getEmail());
            });

        userRepository.findByUsername(registerDTO.getUsername())
            .ifPresent(user -> {
                throw new RuntimeException("Username already taken: " + user.getUsername());
            });

        User newUser = new User();
        newUser.setUsername(registerDTO.getUsername());
        newUser.setEmail(registerDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        newUser.setRole("USER"); // Or "ROLE_USER" if you use Spring Security roles
        newUser.setDailyStreak(0);
        newUser.setProblemSolvedEasy(0);
        newUser.setProblemSolvedMedium(0);
        newUser.setProblemSolvedHard(0);
        newUser.setProblemSolvedTotal(0);
        return userRepository.save(newUser);
    }

    public String loginUser(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + loginDTO.getEmail()));
        System.out.println(user);
        if (passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            System.out.println("Password matched");
            return jwtService.generateToken(user.getId());
        } else {
            System.out.println("INVALID PASSWORD"+loginDTO);
            throw new RuntimeException("Invalid credentials");
        }
    }

    public User getUserByToken(String authHeader){
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String jwt = authHeader.substring(7);
        Long userId = jwtService.extractUserId(jwt.strip());
        return userRepository.findById(userId).orElse(null);
    }
}
