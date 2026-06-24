package com.heal.doctor.security;

import com.heal.doctor.models.CollaboratorProfileEntity;
import com.heal.doctor.models.UserEntity;
import com.heal.doctor.models.enums.RolesEnum;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CollaboratorUserDetails implements UserDetails {

    private final UserEntity user;
    private final CollaboratorProfileEntity profile;

    public CollaboratorUserDetails(UserEntity user, CollaboratorProfileEntity profile) {
        this.user = user;
        this.profile = profile;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        RolesEnum role = user.getRolesEnum();
        // Default to COLLABORATOR role if role is null (for backward compatibility)
        if (role == null) {
            role = RolesEnum.COLLABORATOR;
        }
        // Convert RolesEnum to Spring Security authority format (ROLE_COLLABORATOR)
        String authority = "ROLE_" + role.name();
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    public String getUserId() {
        return user.getUserId();
    }

    public String getDoctorId() {
        return profile.getEffectiveDoctorId();
    }

    public String getActiveDoctorId() {
        return getDoctorId();
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
        return user.getIsActive() != null && user.getIsActive();
    }
}
