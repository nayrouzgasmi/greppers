package tn.esprit.healthcare.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.healthcare.Entities.ERole;
import tn.esprit.healthcare.Entities.Role;
import tn.esprit.healthcare.Entities.User;
import tn.esprit.healthcare.Repositories.RoleRepository;
import tn.esprit.healthcare.Repositories.UserRepository;
import tn.esprit.healthcare.Security.Jwt.JwtUtils;
import tn.esprit.healthcare.Security.Payload.Request.LoginRequest;
import tn.esprit.healthcare.Security.Payload.Request.SignupRequest;
import tn.esprit.healthcare.Security.Payload.Response.JwtResponse;
import tn.esprit.healthcare.Security.Payload.Response.MessageResponse;
import tn.esprit.healthcare.Services.UserDetailsImpl;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @GetMapping("/username")
    public String getUserEmail(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return "Your user name is : " +userDetails.getUsername()+"\nYour email is : " +userDetails.getEmail()+"\nYour role is : "
                +userDetails.getAuthorities()+"\nYour phone number is : "+ userDetails.getPhone_number();
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getPhone_number(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                        signUpRequest.getPhone_number());

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Please choose role!"));
        }
        if (strRoles.contains("Patient")){
            Role patient = roleRepository.findByName(ERole.Patient)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(patient);
            user.setRoles(roles);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("New Patient registered successfully!"));

        }
        if (strRoles.contains("Admin")){
            Role patient = roleRepository.findByName(ERole.Admin)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(patient);
            user.setRoles(roles);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("New Admin registered successfully!"));

        }
        if (strRoles.contains("Doctor")){
            Role patient = roleRepository.findByName(ERole.Doctor)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(patient);
            user.setRoles(roles);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("New Doctor registered successfully!"));

        }
        else {

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error:Select Valid role!"));
        }}



}


