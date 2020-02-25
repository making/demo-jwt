package am.ik.lab.jwt.token;

import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

@RestController
public class PseudoOauthController {

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

    public PseudoOauthController(JwtService jwtService, AuthenticationManager authenticationManager) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(path = "oauth/token", params = "grant_type=password")
    public ResponseEntity<?> token(@RequestParam("username") String username, @RequestParam("password") String password) {
        final Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);
        try {
            final Authentication authenticated = this.authenticationManager.authenticate(authentication);
            final UserDetails userDetails = (UserDetails) authenticated.getPrincipal();
            final String issuer = ServletUriComponentsBuilder.fromCurrentRequest().build().toString();
            final Instant issuedAt = Instant.now();
            final Instant expiresAt = issuedAt.plus(1, ChronoUnit.HOURS);
            final Set<String> scope = Set.of("message:read", "message:write");
            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .expirationTime(Date.from(expiresAt))
                    .subject(userDetails.getUsername())
                    .issueTime(Date.from(issuedAt))
                    .claim("scope", scope)
                    .claim("preferred_username", userDetails.getUsername())
                    .claim("authorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(toSet()))
                    .build();
            final String tokenValue = this.jwtService.sign(claimsSet).serialize();
            return ResponseEntity.ok(Map.of("access_token", tokenValue,
                    "token_type", BEARER.getValue(),
                    "expires_in", Duration.between(issuedAt, expiresAt).getSeconds(),
                    "scope", scope));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized",
                            "error_description", e.getMessage()));
        }
    }
}
