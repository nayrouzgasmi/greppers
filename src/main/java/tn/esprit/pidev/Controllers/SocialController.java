package tn.esprit.pidev.Controllers;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pidev.Entities.ERole;
import tn.esprit.pidev.Entities.Role;
import tn.esprit.pidev.Security.Jwt.JwtUtils;
import tn.esprit.pidev.Security.Payload.Request.LoginRequest;
import tn.esprit.pidev.Security.Payload.Request.TokenDto;
import tn.esprit.pidev.Security.Payload.Response.JwtResponse;
import tn.esprit.pidev.Services.UserDetailsServiceImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping("/social")
// http://localhost:8080/social
public class SocialController {

    @Value("google.id")
    private String idClient;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    private UserDetailsServiceImpl userService;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public SocialController(UserDetailsServiceImpl userService, PasswordEncoder passwordEncoder) {


        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    //http://localhost:8080/social/google
    @PostMapping("/google")
    public JwtResponse loginWithGoogle(@RequestBody TokenDto tokenDto) throws IOException {
        NetHttpTransport transport = new NetHttpTransport();
        JacksonFactory factory = JacksonFactory.getDefaultInstance();
        GoogleIdTokenVerifier.Builder ver =
                new GoogleIdTokenVerifier.Builder(transport,factory)
                        .setAudience(Collections.singleton(idClient));
        GoogleIdToken googleIdToken = GoogleIdToken.parse(ver.getJsonFactory(),tokenDto.getToken());
        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        return login(payload.getEmail());
    }

    //http://localhost:8080/social/facebook
    @PostMapping("/facebook")
    public JwtResponse loginWithFacebook(@RequestBody TokenDto tokenDto){
        Facebook facebook = new FacebookTemplate(tokenDto.getToken());
        String [] data = {"email","name","picture"};
        User userFacebook = facebook.fetchObject("me",User.class,data);
        return login(userFacebook.getEmail());

    }

    private JwtResponse login(String email){
        boolean result = userService.ifEmailExist(email); // t   // f
        if(!result){
            tn.esprit.pidev.Entities.User user = new tn.esprit.pidev.Entities.User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("kasdjhfkadhsY776ggTyUU65khaskdjfhYuHAwjñlji"));
          //  user.setActive(1);
           // List<Role> authorities = authoritiesService.getAuthorities();
            Set<Role> roles = user.getRoles();
            Role newRole = new Role();
          //  newRole.setName(ERole.Client);
           // newRole.setUser(user);
          //  roles.add(newRole); // add the new role to the set
            //user.setRoles(roles); // update the roles set in the user object
            //  user.getUserRoles().add(authorities.get(0));
            userService.addUser(user);
        }
        LoginRequest jwtLogin = new LoginRequest();
        jwtLogin.setUsername(email);
        jwtLogin.setPassword("kasdjhfkadhsY776ggTyUU65khaskdjfhYuHAwjñlji");

        return login(jwtLogin);
    }

    public JwtResponse login(LoginRequest jwtLogin) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(jwtLogin.getUsername(),
                        jwtLogin.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new JwtResponse(jwtLogin.getUsername(),jwt);
    }
}
