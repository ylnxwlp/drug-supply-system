package com.supply.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.util.Assert;

public abstract class DrugAbstractUserDetailsAuthenticationProvider implements AuthenticationProvider, InitializingBean, MessageSourceAware {
    protected final Log logger = LogFactory.getLog(this.getClass());
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private UserCache userCache = new NullUserCache();
    private boolean forcePrincipalAsString = false;
    protected boolean hideUserNotFoundExceptions = true;
    private UserDetailsChecker preAuthenticationChecks = new DrugAbstractUserDetailsAuthenticationProvider.DefaultPreAuthenticationChecks();
    private UserDetailsChecker postAuthenticationChecks = new DrugAbstractUserDetailsAuthenticationProvider.DefaultPostAuthenticationChecks();
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    public DrugAbstractUserDetailsAuthenticationProvider() {
    }

    protected abstract void additionalAuthenticationChecks(UserDetails userDetails, DrugUsernameFirmNameAuthenticationToken authentication) throws AuthenticationException;

    public final void afterPropertiesSet() throws Exception {
        Assert.notNull(this.userCache, "A user cache must be set");
        Assert.notNull(this.messages, "A message source must be set");
        this.doAfterPropertiesSet();
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(DrugUsernameFirmNameAuthenticationToken.class, authentication, () -> this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.onlySupports", "Only UsernamePasswordAuthenticationToken is supported"));
        String usernameAndFirmName = this.determineUsernameAndFirmName((DrugUsernameFirmNameAuthenticationToken) authentication);
        String[] parts = usernameAndFirmName.split(",");
        if (parts.length != 2) {
            throw new BadCredentialsException("Invalid username and work type format");
        }
        String username = parts[0];
        String firmName = parts[1];
        this.logger.debug("当前登录用户：" + username + "，公司为：" + firmName);
        boolean cacheWasUsed = true;
        UserDetails user = this.userCache.getUserFromCache(usernameAndFirmName);
        if (user == null) {
            cacheWasUsed = false;
            try {
                user = this.retrieveUser(username, firmName, (DrugUsernameFirmNameAuthenticationToken) authentication);
            } catch (UsernameNotFoundException var6) {
                this.logger.debug("Failed to find user '" + username + "'");
                if (!this.hideUserNotFoundExceptions) {
                    throw var6;
                }
                throw new BadCredentialsException(this.messages.getMessage("DrugAbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
            }
            Assert.notNull(user, "retrieveUser returned null - a violation of the interface contract");
        }
        try {
            this.preAuthenticationChecks.check(user);
            this.additionalAuthenticationChecks(user, (DrugUsernameFirmNameAuthenticationToken) authentication);
        } catch (AuthenticationException var7) {
            if (!cacheWasUsed) {
                throw var7;
            }
            cacheWasUsed = false;
            user = this.retrieveUser(username, firmName, (DrugUsernameFirmNameAuthenticationToken) authentication);
            this.preAuthenticationChecks.check(user);
            this.additionalAuthenticationChecks(user, (DrugUsernameFirmNameAuthenticationToken) authentication);
        }

        this.postAuthenticationChecks.check(user);
        if (!cacheWasUsed) {
            this.userCache.putUserInCache(user);
        }

        Object principalToReturn = user;
        if (this.forcePrincipalAsString) {
            principalToReturn = user.getUsername();
        }

        return this.createSuccessAuthentication(principalToReturn, firmName,authentication, user);
    }

    private String determineUsernameAndFirmName(DrugUsernameFirmNameAuthenticationToken authentication) {
        Object principal = authentication.getPrincipal();
        Object info = authentication.getInfo();
        if (principal == null) {
            return "NONE_PROVIDED";
        }
        return principal + "," + (info != null ? info.toString() : "No Info");
    }


    protected Authentication createSuccessAuthentication(Object principal, String firmName, Authentication authentication, UserDetails user) {
        DrugUsernameFirmNameAuthenticationToken result = DrugUsernameFirmNameAuthenticationToken.authenticated(principal, firmName,authentication.getCredentials(), this.authoritiesMapper.mapAuthorities(user.getAuthorities()));
        result.setDetails(authentication.getDetails());
        this.logger.debug("Authenticated user");
        return result;
    }

    protected void doAfterPropertiesSet() throws Exception {
    }

    public UserCache getUserCache() {
        return this.userCache;
    }

    public boolean isForcePrincipalAsString() {
        return this.forcePrincipalAsString;
    }

    public boolean isHideUserNotFoundExceptions() {
        return this.hideUserNotFoundExceptions;
    }

    protected abstract UserDetails retrieveUser(String username, String firmName, DrugUsernameFirmNameAuthenticationToken authentication) throws AuthenticationException;

    public void setForcePrincipalAsString(boolean forcePrincipalAsString) {
        this.forcePrincipalAsString = forcePrincipalAsString;
    }

    public void setHideUserNotFoundExceptions(boolean hideUserNotFoundExceptions) {
        this.hideUserNotFoundExceptions = hideUserNotFoundExceptions;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    public void setUserCache(UserCache userCache) {
        this.userCache = userCache;
    }

    public boolean supports(Class<?> authentication) {
        return DrugUsernameFirmNameAuthenticationToken.class.isAssignableFrom(authentication);
    }

    protected UserDetailsChecker getPreAuthenticationChecks() {
        return this.preAuthenticationChecks;
    }

    public void setPreAuthenticationChecks(UserDetailsChecker preAuthenticationChecks) {
        this.preAuthenticationChecks = preAuthenticationChecks;
    }

    protected UserDetailsChecker getPostAuthenticationChecks() {
        return this.postAuthenticationChecks;
    }

    public void setPostAuthenticationChecks(UserDetailsChecker postAuthenticationChecks) {
        this.postAuthenticationChecks = postAuthenticationChecks;
    }

    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }

    private class DefaultPreAuthenticationChecks implements UserDetailsChecker {
        private DefaultPreAuthenticationChecks() {
        }

        public void check(UserDetails user) {
            if (!user.isAccountNonLocked()) {
                DrugAbstractUserDetailsAuthenticationProvider.this.logger.debug("Failed to authenticate since user account is locked");
                throw new LockedException(DrugAbstractUserDetailsAuthenticationProvider.this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.locked", "User account is locked"));
            } else if (!user.isEnabled()) {
                DrugAbstractUserDetailsAuthenticationProvider.this.logger.debug("Failed to authenticate since user account is disabled");
                throw new DisabledException(DrugAbstractUserDetailsAuthenticationProvider.this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.disabled", "User is disabled"));
            } else if (!user.isAccountNonExpired()) {
                DrugAbstractUserDetailsAuthenticationProvider.this.logger.debug("Failed to authenticate since user account has expired");
                throw new AccountExpiredException(DrugAbstractUserDetailsAuthenticationProvider.this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.expired", "User account has expired"));
            }
        }
    }

    private class DefaultPostAuthenticationChecks implements UserDetailsChecker {
        private DefaultPostAuthenticationChecks() {
        }

        public void check(UserDetails user) {
            if (!user.isCredentialsNonExpired()) {
                DrugAbstractUserDetailsAuthenticationProvider.this.logger.debug("Failed to authenticate since user account credentials have expired");
                throw new CredentialsExpiredException(DrugAbstractUserDetailsAuthenticationProvider.this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.credentialsExpired", "User credentials have expired"));
            }
        }
    }
}
