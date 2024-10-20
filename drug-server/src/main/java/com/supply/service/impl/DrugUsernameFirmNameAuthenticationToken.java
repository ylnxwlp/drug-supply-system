package com.supply.service.impl;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Collection;

public class DrugUsernameFirmNameAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 620L;
    private final Object principal;
    private final Object info;
    private Object credentials;

    public DrugUsernameFirmNameAuthenticationToken(Object principal, Object info, Object credentials) {
        super(null);
        this.principal = principal;
        this.info = info;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public DrugUsernameFirmNameAuthenticationToken(Object principal, Object info, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.info = info;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    public static DrugUsernameFirmNameAuthenticationToken unauthenticated(Object principal, Object info, Object credentials) {
        return new DrugUsernameFirmNameAuthenticationToken(principal, info, credentials);
    }

    public static DrugUsernameFirmNameAuthenticationToken authenticated(Object principal, Object info, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        return new DrugUsernameFirmNameAuthenticationToken(principal, info, credentials, authorities);
    }

    public Object getCredentials() {
        return this.credentials;
    }

    public Object getPrincipal() {
        return this.principal;
    }

    public Object getInfo() {
        return this.info;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated, "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
