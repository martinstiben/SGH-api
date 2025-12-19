package com.horarios.SGH.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del almacenamiento de tokens revocados.
 * Usa ConcurrentHashMap para thread-safety.
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class InMemoryTokenStorage implements TokenStorage {

    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    @Override
    public void addToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            revokedTokens.add(token);
        }
    }

    @Override
    public boolean containsToken(String token) {
        return token != null && revokedTokens.contains(token);
    }

    @Override
    public int size() {
        return revokedTokens.size();
    }
}