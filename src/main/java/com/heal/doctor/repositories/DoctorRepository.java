package com.heal.doctor.repositories;

import com.heal.doctor.models.DoctorEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends MongoRepository<DoctorEntity, String> {
    Optional<DoctorEntity> findByDoctorId(String doctorId);

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
