package com.tech.BookStore.auth;

import com.tech.BookStore.role.RoleRepo;
import com.tech.BookStore.security.JwtService;
import com.tech.BookStore.token.Token;
import com.tech.BookStore.token.TokenRepo;
import com.tech.BookStore.user.User;
import com.tech.BookStore.user.UserRepo;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    private final RoleRepo repo;
    private final UserRepo userRepo;
    private final TokenRepo tokenRepo;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;



    public void register(RegistrationRequest request)
    {
        var userRole = repo.findByName("USER")
                .orElseThrow(()-> new IllegalStateException("Role user is not initialized"));
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepo.save(user);
    }

    public void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);

        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplate.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );


    }
    private String generateAndSaveActivationToken(User user)
    {
        String generateToken = generateActivationCode(6);
        var token  = Token.builder()
                .token(generateToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepo.save(token);
        return generateToken;

    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for(int i =0 ; i <length ;i++) {
            int randomText = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomText));
        }
        return codeBuilder.toString();
    }

    public AuthenticationResponse authenticate(AuthenticateRequest request)
    {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var claims = new HashMap<String,Object>();
        var user = ((User)auth.getPrincipal());

        claims.put("fullName",user.fullName());
        var jwtToken = jwtService.generateToken(claims,user);


        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    @Transactional
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepo.findByToken(token).orElseThrow(()->new RuntimeException("Invalid token"));
        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt()))
        {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired.A new token has been send to the account");
        }

        var user = userRepo.findById(savedToken.getUser().getId()).orElseThrow(()-> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepo.save(user);
        savedToken.setValidateAt(LocalDateTime.now());
        tokenRepo.save(savedToken);
    }
}
