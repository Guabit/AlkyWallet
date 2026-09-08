package com.alkywallet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import jakarta.annotation.PostConstruct;

/**
 * Servicio utilitario responsable de la generacion, firma, parseo y validacion de tokens JWT.
 */

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostConstruct
    void validateSecret() {
        if (secretKey == null || secretKey.isBlank() || secretKey.length() < 32) {
            throw new IllegalStateException("JWT_SECRET no configurado o demasiado corto (mín. 256 bits)");
        }
    }

    /**
     * Genera un token JWT incluyendo los roles/permisos del usuario.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = Map.of(
                "authorities", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );
        return generateToken(claims, userDetails.getUsername());
    }

    /**
     * Construye y firma el token con claims adicionales, subject y tiempo de expiracion.
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Obtiene la clave criptografica segura HMAC-SHA a partir del secreto en Base64.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parsea el payload del token y maneja posibles excepciones de firma o expiracion.
     */
    private Claims getAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("JWT token invalido o malformado", e);
        }
    }

    //Metodo para devolver cualquier claim del token
    public <T> T getClaim(String token, Function<Claims, T> claimsMapper) {
        final Claims allClaims = getAllClaims(token);
        return claimsMapper.apply(allClaims);
    }

    //Devuelve el username del token
    public String getUsername(String token) {
        return getAllClaims(token).getSubject();
    }

    //Devuelve la fecha de expiracion del token
    public Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    //Verifica si el token ya expiro
    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }

    /**
     * Valida que el token corresponda al usuario y no se encuentre expirado.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}


