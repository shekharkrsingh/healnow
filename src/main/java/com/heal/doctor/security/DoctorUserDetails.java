package com.heal.doctor.security;

import com.heal.doctor.models.DoctorEntity;
import com.heal.doctor.models.enums.RolesEnum;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class DoctorUserDetails implements UserDetails {

    private final DoctorEntity doctor;

    public DoctorUserDetails(DoctorEntity doctor) {
        this.doctor = doctor;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        RolesEnum role = doctor.getRolesEnum();
        if (role == null) {
            role = RolesEnum.DOCTOR; // Default to DOCTOR if role is null
        }
        String authority = "ROLE_" + role.name();
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return doctor.getPassword();
    }

    @Override
    public String getUsername() {
        return doctor.getEmail();
    }

    public String getDoctorId() {
        return doctor.getDoctorId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
