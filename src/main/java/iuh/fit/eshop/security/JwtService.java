package iuh.fit.eshop.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class JwtService  {
    public static final String COOKIE_NAME="ACCESS_TOKEN";
    private final SecretKey key;
    private final long expiration;
    public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-ms}") long expiration) {
        this.key=Keys.hmacShaKeyFor(decodeSecret(secret));
        this.expiration=expiration;
    }

    private static byte[] decodeSecret(String configuredSecret) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT_SECRET is required and must be a Base64-encoded secret of at least 32 bytes");
        }
        String secret = configuredSecret.trim();
        if (secret.length() >= 2 &&
                ((secret.startsWith("\"") && secret.endsWith("\""))
                        || (secret.startsWith("'") && secret.endsWith("'")))) {
            secret = secret.substring(1, secret.length() - 1).trim();
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length < 32) {
                throw new IllegalArgumentException("decoded secret is shorter than 32 bytes");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "app.jwt.secret/JWT_SECRET must be valid standard Base64 and decode to at least 32 bytes. "
                            + "Generate one with: [Convert]::ToBase64String((1..32 | %% { Get-Random -Maximum 256 }))",
                    ex);
        }
    }
    public String generateToken(UserDetails user) {
        List<String> roles=user.getAuthorities().stream().map(a->a.getAuthority()).collect(Collectors.toList());
        return Jwts.builder().subject(user.getUsername()).claim("roles",roles).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expiration)).signWith(key).compact();
    }
    public String username(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean valid(String token,UserDetails user) {
        try {
            return username(token).equals(user.getUsername())&&Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getExpiration().after(new Date());
        } catch(JwtException|IllegalArgumentException e) {
            return false;
        }
    }
    public long getExpirationSeconds() {
        return expiration/1000;
    }
}
