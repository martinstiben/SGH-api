package com.horarios.SGH.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para manejo seguro de tokens JWT (JSON Web Tokens).
 * Proporciona funcionalidades completas para generar, validar y extraer información de tokens JWT.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de gestionar tokens JWT
 * - DIP: No depende de implementaciones concretas
 *
 * Funcionalidades:
 * - Generación de tokens con claims personalizados
 * - Validación de tokens contra UserDetails
 * - Extracción de claims específicos
 * - Verificación de expiración
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     * El subject típicamente contiene el username o email del usuario.
     *
     * @param token El token JWT del cual extraer el username
     * @return El nombre de usuario contenido en el token
     * @throws io.jsonwebtoken.ExpiredJwtException si el token ha expirado
     * @throws io.jsonwebtoken.MalformedJwtException si el token está malformado
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae una reclamación específica del token JWT usando un resolver funcional.
     * Permite extraer cualquier claim de forma type-safe.
     *
     * @param token El token JWT del cual extraer la reclamación
     * @param claimsResolver Función que especifica qué reclamación extraer
     * @param <T> Tipo de la reclamación a extraer
     * @return La reclamación extraída del tipo especificado
     * @throws io.jsonwebtoken.ExpiredJwtException si el token ha expirado
     * @throws io.jsonwebtoken.MalformedJwtException si el token está malformado
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Genera un token JWT para un usuario sin claims adicionales.
     * Utiliza el username del UserDetails como subject.
     *
     * @param userDetails Los detalles del usuario para el cual generar el token
     * @return El token JWT generado como String
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Genera un token JWT con reclamaciones adicionales personalizadas.
     * Incluye issuedAt, expiration y firma digital.
     *
     * @param extraClaims Map con reclamaciones adicionales a incluir en el token
     * @param userDetails Los detalles del usuario para el cual generar el token
     * @return El token JWT generado como String
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * Verifica si un token JWT es válido para un usuario específico.
     * Comprueba que el username coincida y que el token no haya expirado.
     *
     * @param token El token JWT a validar
     * @param userDetails Los detalles del usuario contra los que validar
     * @return true si el token es válido y pertenece al usuario, false en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica si un token JWT ha expirado comparando con la fecha actual.
     *
     * @param token El token JWT a verificar
     * @return true si el token ha expirado, false si aún es válido
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token El token JWT del cual extraer la fecha de expiración
     * @return La fecha de expiración como objeto Date
     * @throws io.jsonwebtoken.ExpiredJwtException si el token ha expirado
     * @throws io.jsonwebtoken.MalformedJwtException si el token está malformado
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae todas las reclamaciones del token JWT después de verificar la firma.
     * Este método realiza la validación criptográfica del token.
     *
     * @param token El token JWT a parsear
     * @return Objeto Claims con todas las reclamaciones del token
     * @throws io.jsonwebtoken.ExpiredJwtException si el token ha expirado
     * @throws io.jsonwebtoken.MalformedJwtException si el token está malformado
     * @throws io.jsonwebtoken.SignatureException si la firma es inválida
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}