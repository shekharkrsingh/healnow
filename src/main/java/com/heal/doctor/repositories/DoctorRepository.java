package com.heal.doctor.repositories;

import com.heal.doctor.models.DoctorEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import com.heal.doctor.models.enums.VerificationStatus;

public interface DoctorRepository extends MongoRepository<DoctorEntity, String> {
    Optional<DoctorEntity> findByDoctorId(String doctorId);

    List<DoctorEntity> findByLicenseExpiryDateBeforeAndVerificationStatus(Date date, VerificationStatus status);

    List<DoctorEntity> findByLicenseExpiryDateBetweenAndVerificationStatus(Date start, Date end, VerificationStatus status);

    Boolean existsByDoctorId(String doctorId);

    @Query("{ $and: [ " +
           "{ 'clinicAddress': { $regex: ?0, $options: 'i' } }, " +
           "{ $expr: { " +
           "  $regexMatch: { " +
           "    input: { $concat: [ " +
           "      { $ifNull: ['$firstName', ''] }, ' ', " +
           "      { $ifNull: ['$lastName', ''] }, ' ', " +
           "      { $ifNull: ['$specialization', ''] }, ' ', " +
           "      { $ifNull: ['$clinicName', ''] } " +
           "    ] }, " +
           "    regex: ?1, " +
           "    options: 'i' " +
           "  } " +
           "} } " +
           "] }")
    List<DoctorEntity> findByLocationAndQuery(String location, String query);
}
