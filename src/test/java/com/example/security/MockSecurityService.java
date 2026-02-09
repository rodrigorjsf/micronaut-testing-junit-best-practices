package com.example.security;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock security service that always allows access.
 * <p>
 * Activated only in the TEST environment when {@code mockSecurityService=true}.
 * Most tests use this mock by default. Tests that need the real security service
 * override {@code mockSecurityServiceEnabled()} to return {@code false}.
 * </p>
 */
@Primary
@Singleton
@Requires(env = Environment.TEST)
@Requires(property = "mockSecurityService", value = StringUtils.TRUE)
public class MockSecurityService implements SecurityService {

    private static final Logger LOG = LoggerFactory.getLogger(MockSecurityService.class);

    @Override
    public boolean canUserAccess(@Nullable String username) {
        LOG.debug(" ================ Using mock security service ================ ");
        return true;
    }
}
