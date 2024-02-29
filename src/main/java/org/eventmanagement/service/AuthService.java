package org.eventmanagement.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eventmanagement.converter.ObjectConverter;
import org.eventmanagement.dto.LoginRequest;
import org.eventmanagement.dto.TokenResponse;
import org.eventmanagement.dto.UserDetailsImpl;
import org.eventmanagement.dto.UserRegistration;
import org.eventmanagement.exception.UserAlreadyExistException;
import org.eventmanagement.model.User;
import org.eventmanagement.repository.UserRepository;
import org.eventmanagement.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private ObjectConverter converter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    public Optional<UserRegistration> signUp(UserRegistration userRegistrationRequest) throws Exception {
        checkForDuplicateUserRecord(userRegistrationRequest);
        userRegistrationRequest.setPassword(encoder.encode(userRegistrationRequest.getPassword()));
        User user = (User) converter.convert(userRegistrationRequest, User.class);
        User savedUser = this.userRepository.save(user);
        UserRegistration savedUserDto = (UserRegistration) this.converter.convert(savedUser, UserRegistration.class);
        return Optional.of(savedUserDto);
    }

    private void checkForDuplicateUserRecord(UserRegistration userRegistrationRequest) throws Exception {
        Optional<User> user = this.userRepository.findByEmail(userRegistrationRequest.getEmail());
        if (user.isPresent()) {
            throw new Exception(new UserAlreadyExistException("User already exist with this email id."));
        }


    }


    public Optional<TokenResponse> signIn(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return Optional.of(new TokenResponse(jwt, userDetails.getRole()));
    }
}
