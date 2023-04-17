package tn.esprit.pidev.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pidev.Entities.Code;
import tn.esprit.pidev.Entities.ERole;
import tn.esprit.pidev.Entities.Role;
import tn.esprit.pidev.Entities.User;
import tn.esprit.pidev.Repositories.RoleRepository;
import tn.esprit.pidev.Repositories.UserRepository;
import tn.esprit.pidev.Security.Jwt.JwtUtils;
import tn.esprit.pidev.Security.Payload.Request.*;
import tn.esprit.pidev.Security.Payload.Response.JwtResponse;
import tn.esprit.pidev.Security.Payload.Response.MessageResponse;
import tn.esprit.pidev.Services.EmailServiceImpl;
import tn.esprit.pidev.Services.UserCode;
import tn.esprit.pidev.Services.UserDetailsImpl;
import tn.esprit.pidev.Services.UserDetailsServiceImpl;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(value = "http://localhost:4200")
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
    @Autowired
    EmailServiceImpl emailService;
    @Autowired
    UserDetailsServiceImpl userDetailsService;

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

        if (strRoles.contains("Client")){
            Role client = roleRepository.findByName(ERole.Client)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(client);
            user.setRoles(roles);
            String myCode = UserCode.getCode();
            Mail mail = new Mail(signUpRequest.getEmail(),myCode);
            emailService.sendCodeByMail(mail);
            user.setActive(0);
            Code code=new Code();
            code.setCode(myCode);
            user.setCode(code);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("New Doctor registered successfully!"));

        }
        if (strRoles.contains("Marchant")){
            Role patient = roleRepository.findByName(ERole.Marchant)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(patient);
            user.setRoles(roles);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("New Patient registered successfully!"));

        }
        if (strRoles.contains("Admin")){
            Role admin = roleRepository.findByName(ERole.Admin)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(admin);
            user.setRoles(roles);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("New Admin registered successfully!"));

        }

        else {

            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error:Select Valid role!"));
        }}

    @PostMapping("/active")
    public UserActive getActiveUser(@RequestBody LoginRequest jwtLogin){
        String enPassword = userDetailsService.getPasswordByEmail(jwtLogin.getEmail());  // from DB
        boolean result = encoder.matches(jwtLogin.getPassword(),enPassword); // Sure
        UserActive userActive = new UserActive();
        if (result){
            int act = userDetailsService.getUserActive(jwtLogin.getEmail());
            if(act == 0){
                String code = UserCode.getCode();
                Mail mail = new Mail(jwtLogin.getEmail(),code);
                emailService.sendCodeByMail(mail);
                User user = userDetailsService.getUserByMail(jwtLogin.getEmail());
                user.getCode().setCode(code);
                userDetailsService.editUser(user);
            }
            userActive.setActive(act);
        } else {
            userActive.setActive(-1);
        }
        return userActive;
    }

    // http://localhost:8080/activated
    @PostMapping("/activated")
    public ResponseEntity<?> activeAccount(@RequestBody ActiveAccount activeAccount){
        User user = userDetailsService.getUserByMail(activeAccount.getMail());

        if(user.getCode().getCode().equals(activeAccount.getCode())){
            user.setActive(1);
            userDetailsService.editUser(user);
            return ResponseEntity
                    .ok(new MessageResponse("Account Active!"));        }
        else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Account not active!"));
        }
    }
    // http://localhost:8080/resetPassword
    @PostMapping("/resetPassword")
    public ResponseEntity<?>  resetPassword(@RequestBody NewPassword newPassword){
        User user = this.userDetailsService.getUserByMail(newPassword.getEmail());
        if(user != null){
            if(user.getCode().getCode().equals(newPassword.getCode())){
                user.setPassword(encoder.encode(newPassword.getPassword()));
                userDetailsService.addUser(user);
                return ResponseEntity

                        .ok(new MessageResponse("Password changed!"));
            } else {

                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error while changing password!"));
            }
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Email does not exist!"));
        }

    }

}


