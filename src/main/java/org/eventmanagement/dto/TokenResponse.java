package org.eventmanagement.dto;

import java.io.Serializable;
import java.util.List;

import org.eventmanagement.enums.Roles;

public class TokenResponse implements Serializable {
    private String token;

    private Roles role;

    public TokenResponse(String token, Roles roles) {
        this.token = token;
        this.role = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }
}
