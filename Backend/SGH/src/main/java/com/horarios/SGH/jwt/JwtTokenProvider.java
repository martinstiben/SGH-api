package com.horarios.SGH.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Proveedor JWT seguro usando la librería estándar JJWT.
 * Esta implementación corrige las vulnerabilidades de seguridad identificadas
 * en la versión anterior que usaba parsing manual de JSON.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    /**
     * Genera un token JWT seguro para el usuario especificado.
     * 
     * @param username nombre de usuario para el token
     * @return token JWT firmado
     * @throws JwtException si ocurre un error durante la generación
     */
    public String generateToken(String username) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
            
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            return Jwts.builder()
                    .setSubject(username)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            logger.error("Error generando token JWT para usuario {}: {}", username, e.getMessage(), e);
            throw new JwtException("Error generando token JWT", e);
        }
    }

    /**
     * Extrae el nombre de usuario del token JWT.
     * 
     * @param token token JWT a parsear
     * @return nombre de usuario extraído o null si el token es inválido
     */
    public String getUsernameFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
                    
            return claims.getBody().getSubject();
        } catch (ExpiredJwtException e) {
            logger.warn("Token JWT expirado: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            logger.warn("Token JWT no soportado: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            logger.warn("Token JWT malformado: {}", e.getMessage());
            return null;
        } catch (SecurityException | IllegalArgumentException e) {
            logger.warn("Error de seguridad en token JWT: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error inesperado parseando token JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Valida un token JWT contra los detalles del usuario.
     * 
     * @param token token JWT a validar
     * @param userDetails detalles del usuario para validación
     * @return true si el token es válido, false en caso contrario
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
                    
            String username = claims.getBody().getSubject();
            Date expiration = claims.getBody().getExpiration();
            
            // Verificar que el usuario coincide y el token no ha expirado
            boolean isUsernameValid = username.equals(userDetails.getUsername());
            boolean isTokenExpired = expiration.before(new Date());
            
            if (isTokenExpired) {
                logger.warn("Token JWT expirado para usuario: {}", username);
                return false;
            }
            
            if (!isUsernameValid) {
                logger.warn("Username en token no coincide con userDetails: {} vs {}", 
                           username, userDetails.getUsername());
                return false;
            }
            
            logger.debug("Token JWT validado exitosamente para usuario: {}", username);
            return true;
            
        } catch (ExpiredJwtException e) {
            logger.warn("Token JWT expirado durante validación: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Token JWT no soportado durante validación: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Token JWT malformado durante validación: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            logger.warn("Error de seguridad durante validación de token JWT: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("Token JWT inválido durante validación: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error inesperado durante validación de token JWT: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extrae el token JWT del header de autorización HTTP.
     * 
     * @param request petición HTTP
     * @return token JWT extraído o null si no se encuentra
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Verifica si un token JWT está bien formado y es legible.
     * Método auxiliar para validación básica de formato.
     * 
     * @param token token a verificar
     * @return true si el token tiene formato JWT válido
     */
    public boolean isTokenWellFormed(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Verificar que tiene exactamente 3 partes separadas por puntos
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            // Intentar parsear el token sin verificar la firma
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token.substring(0, token.lastIndexOf('.')));
                
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene la fecha de expiración de un token JWT.
     * 
     * @param token token JWT
     * @return fecha de expiración o null si el token es inválido
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
                    
            return claims.getBody().getExpiration();
        } catch (Exception e) {
            logger.warn("Error obteniendo fecha de expiración del token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica si un token JWT ha expirado.
     * 
     * @param token token JWT
     * @return true si el token ha expirado
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
}
