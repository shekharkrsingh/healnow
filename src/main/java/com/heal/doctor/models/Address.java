package com.heal.doctor.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    private String street;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-]+$", message = "City can only contain letters, spaces, and hyphens")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100, message = "State must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-]+$", message = "State can only contain letters, spaces, and hyphens")
    private String state;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 100, message = "Country must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-]+$", message = "Country can only contain letters, spaces, and hyphens")
    private String country;

    @NotBlank(message = "Pincode is required")
    @Size(min = 5, max = 10, message = "Pincode must be between 5 and 10 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Pincode can only contain alphanumeric characters")
    private String pincode;
}
