package com.carpeso.carpeso_backend.service;

import com.carpeso.carpeso_backend.dto.AuthResponse;
import com.carpeso.carpeso_backend.dto.LoginRequest;
import com.carpeso.carpeso_backend.dto.RegisterRequest;
import com.carpeso.carpeso_backend.model.User;
import com.carpeso.carpeso_backend.repository.UserRepository;
import com.carpeso.carpeso_backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        userRepository.save(user);
        auditLogService.log(
                "USER_REGISTERED",
                user.getUsername(),
                "User",
                String.valueOf(user.getId()),
                "New user registered: " + user.getUsername(),
                "system"
        );
        String token = jwtUtil.generateToken(user.getUsername(),
                user.getRole().name());
        return new AuthResponse(token, user.getRole().name(),
                user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found!"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }
        String token = jwtUtil.generateToken(user.getUsername(),
                user.getRole().name());
        return new AuthResponse(token, user.getRole().name(),
                user.getUsername());
    }
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}