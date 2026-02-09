package com.example.security;

import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very naive security service.
 */
@Singleton
public class SecurityServiceImpl implements SecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityServiceImpl.class);

    @Override
    public boolean canUserAccess(@Nullable String username) {
        LOG.debug(" ================ Using REAL security service ================ ");
        // Only admin can access
        return username != null && username.equals("admin");
    }
}
