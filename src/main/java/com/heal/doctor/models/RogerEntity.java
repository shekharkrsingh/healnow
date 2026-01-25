package com.heal.doctor.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "rogers")
public class RogerEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @UniqueElements
    private String rogerId; // Linked to UserEntity.userId

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePicture;
    private String address;

    private Date createdAt;
    private Date updatedAt;
}
