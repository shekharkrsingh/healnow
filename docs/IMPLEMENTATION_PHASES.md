# Implementation Phases: Video Consultation Feature

## Overview

This document breaks down the implementation into **8 distinct phases**, each with clear objectives, tasks, and acceptance criteria. Phases are designed to be completed sequentially with minimal dependencies.

**Total Timeline**: 14-22 days  
**Team Size**: 2-3 developers (1 backend, 1 frontend, 1 tester)

---

## Phase 1: Foundation - Models & Enums

**Duration**: 1-2 days  
**Priority**: Critical (blocks all other phases)  
**Assigned To**: Backend Developer

### Objectives
- Create new enums for consultation types and appointment modes
- Add new fields to existing entities
- Ensure backward compatibility

### Tasks

#### Task 1.1: Create New Enums (30 min)

**Files to Create**:
- `src/main/java/com/heal/doctor/models/enums/ConsultationType.java`
- `src/main/java/com/heal/doctor/models/enums/AppointmentMode.java`
- `src/main/java/com/heal/doctor/models/enums/AppointmentCallStatus.java`

**Instructions**:
```java
// 1. Create ConsultationType.java
package com.heal.doctor.models.enums;

public enum ConsultationType {
    IN_PERSON_VISIT,
    VIDEO_CALL
}

// 2. Create AppointmentMode.java
public enum AppointmentMode {
    IN_PERSON_ONLY,
    VIDEO_CALL_ONLY,
    BOTH
}

// 3. Create AppointmentCallStatus.java
public enum AppointmentCallStatus {
    SCHEDULED,
    PATIENT_WAITING,
    DOCTOR_WAITING,
    IN_PROGRESS,
    BRIEF_DISCONNECTION,
    INCOMPLETE_CONSULTATION,
    TECHNICAL_FAILURE,
    COMPLETED,
    PATIENT_NO_SHOW,
    DOCTOR_NO_SHOW
}
```

**Validation**:
- [ ] All enums compile without errors
- [ ] No naming conflicts with existing code

---

#### Task 1.2: Modify AppointmentStatus Enum (15 min)

**File to Modify**:
- `src/main/java/com/heal/doctor/models/enums/AppointmentStatus.java`

**Instructions**:
```java
// Add NO_SHOW status
public enum AppointmentStatus {
    ACCEPTED,
    BOOKED,
    CANCELLED,
    NO_SHOW  // ADD THIS
}
```

**Validation**:
- [ ] Enum compiles
- [ ] Existing code using AppointmentStatus still works

---

#### Task 1.3: Create DoctorSchedulingConfig Class (45 min)

**File to Create**:
- `src/main/java/com/heal/doctor/models/DoctorSchedulingConfig.java`

**Instructions**:
```java
package com.heal.doctor.models;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import java.util.List;

@Data
public class DoctorSchedulingConfig {
    // Basic scheduling
    private Boolean strictNoOverlapping = false;
    
    @Min(5) @Max(480)
    private Integer treatmentDurationMinutes;
    
    @Min(0) @Max(60)
    private Integer cooldownMinutes = 0;
    
    private Boolean allowWeekendBooking = true;
    private List<LocalDate> blockedDates;
    
    // No-show detection
    @Min(5) @Max(30)
    private Integer noShowGracePeriodMinutes = 15;
    
    // Call tracking
    @Min(2) @Max(10)
    private Integer minimumCallDurationMinutes = 5;
    
    @Min(1) @Max(10)
    private Integer reconnectionWindowMinutes = 5;
    
    // Auto-availability
    private Boolean autoReleaseNoShowSlots = true;
}
```

**Validation**:
- [ ] Class compiles
- [ ] All validation annotations present
- [ ] Lombok @Data generates getters/setters

---

#### Task 1.4: Modify DoctorEntity (30 min)

**File to Modify**:
- `src/main/java/com/heal/doctor/models/DoctorEntity.java`

**Instructions**:
Add these fields to DoctorEntity class:

```java
import com.heal.doctor.models.enums.AppointmentMode;
import jakarta.validation.Valid;

// Add after existing fields

@Builder.Default
private AppointmentMode appointmentMode = AppointmentMode.IN_PERSON_ONLY;

@Valid
private DoctorSchedulingConfig schedulingConfig;

@Builder.Default
private Boolean acceptsVideoConsultations = false;
```

**Validation**:
- [ ] DoctorEntity compiles
- [ ] Builder pattern still works
- [ ] Defaults are set correctly

---

#### Task 1.5: Modify AppointmentEntity (45 min)

**File to Modify**:
- `src/main/java/com/heal/doctor/models/AppointmentEntity.java`

**Instructions**:
Add these fields to AppointmentEntity class:

```java
import com.heal.doctor.models.enums.ConsultationType;
import com.heal.doctor.models.enums.AppointmentCallStatus;
import java.util.Date;

// Add after existing fields

// Consultation method
private ConsultationType consultationType;

// Duration tracking
private Integer estimatedDurationMinutes;

// Video call tracking
private Date patientJoinedAt;
private Date patientLeftAt;
private Date doctorJoinedAt;
private Date doctorLeftAt;
private Date actualCallStartTime;
private Date actualCallEndTime;
private Integer actualCallDurationSeconds;
private AppointmentCallStatus callStatus;
private String disconnectReason;

// Video call link (future use)
private String videoCallLink;
```

**Validation**:
- [ ] AppointmentEntity compiles
- [ ] All new fields have proper types
- [ ] No conflicts with existing fields

---

#### Task 1.6: Create DTOs (1 hour)

**Files to Create**:
- `UpdateAppointmentModeDTO.java`
- `DoctorSchedulingConfigDTO.java`
- `AppointmentSlotDTO.java`

**Instructions**:

```java
// 1. UpdateAppointmentModeDTO.java
@Data
public class UpdateAppointmentModeDTO {
    @NotNull
    private AppointmentMode appointmentMode;
    
    @Valid
    private DoctorSchedulingConfig schedulingConfig;
}

// 2. DoctorSchedulingConfigDTO.java
@Data
public class DoctorSchedulingConfigDTO {
    // Same fields as DoctorSchedulingConfig
    // Used for API responses
}

// 3. AppointmentSlotDTO.java
@Data
public class AppointmentSlotDTO {
    private Date startTime;
    private Date endTime;
    private Boolean available;
    private Integer durationMinutes;
}
```

**Validation**:
- [ ] All DTOs compile
- [ ] Validation annotations present
- [ ] Mapper methods can be written

---

#### Task 1.7: Database Migration Script (30 min)

**File to Create**:
- `src/main/resources/db/migration/V1__add_video_consultation_fields.js`

**Instructions**:
```javascript
// MongoDB migration script

// 1. Add fields to doctors
db.doctors.updateMany({}, {
    $set: {
        appointmentMode: "IN_PERSON_ONLY",
        acceptsVideoConsultations: false,
        schedulingConfig: {
            strictNoOverlapping: false,
            treatmentDurationMinutes: 30,
            cooldownMinutes: 5,
            allowWeekendBooking: true,
            blockedDates: [],
            noShowGracePeriodMinutes: 15,
            minimumCallDurationMinutes: 5,
            reconnectionWindowMinutes: 5,
            autoReleaseNoShowSlots: true
        }
    }
});

// 2. Add fields to appointments
db.appointments.updateMany(
    { consultationType: { $exists: false } },
    { 
        $set: { 
            consultationType: "IN_PERSON_VISIT",
            estimatedDurationMinutes: 30,
            callStatus: "SCHEDULED"
        }
    }
);

// 3. Create indexes
db.appointments.createIndex({
    "doctorId": 1,
    "appointmentDateTime": 1,
    "status": 1
});

db.appointments.createIndex({
    "doctorId": 1,
    "callStatus": 1,
    "appointmentDateTime": 1
});
```

**Validation**:
- [ ] Script runs without errors
- [ ] All existing doctors have new fields
- [ ] All existing appointments have consultationType
- [ ] Indexes are created

---

### Phase 1 Acceptance Criteria
- [ ] All new enums created and compile
- [ ] DoctorEntity has 3 new fields with defaults
- [ ] AppointmentEntity has 13 new fields
- [ ] Database migration runs successfully
- [ ] Existing appointments default to IN_PERSON_VISIT
- [ ] Existing doctors default to IN_PERSON_ONLY mode
- [ ] All DTOs created
- [ ] No breaking changes to existing code

---

## Phase 2: Slot Generation Service

**Duration**: 2-3 days  
**Priority**: High (required for booking)  
**Assigned To**: Backend Developer

### Objectives
- Implement slot generation algorithm
- Respect doctor's schedule and configuration
- Enforce boundary rules
- Handle edge cases

### Tasks

#### Task 2.1: Create SlotGenerationService Interface (15 min)

**File to Create**:
- `src/main/java/com/heal/doctor/services/ISlotGenerationService.java`

**Instructions**:
```java
package com.heal.doctor.services;

import com.heal.doctor.dto.AppointmentSlotDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Date;

public interface ISlotGenerationService {
    List<AppointmentSlotDTO> generateAvailableSlots(
        String doctorId, 
        LocalDate date
    );
    
    boolean isSlotAvailable(
        String doctorId, 
        Date startTime, 
        Date endTime
    );
}
```

---

#### Task 2.2: Implement SlotGenerationServiceImpl (4-6 hours)

**File to Create**:
- `src/main/java/com/heal/doctor/services/impl/SlotGenerationServiceImpl.java`

**Instructions**:

**Step 1**: Create service class skeleton
```java
@Service
public class SlotGenerationServiceImpl implements ISlotGenerationService {
    
    private final IDoctorService doctorService;
    private final IAppointmentService appointmentService;
    
    public SlotGenerationServiceImpl(
        IDoctorService doctorService,
        IAppointmentService appointmentService
    ) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
    }
}
```

**Step 2**: Implement generateAvailableSlots() method

```java
@Override
public List<AppointmentSlotDTO> generateAvailableSlots(
    String doctorId, 
    LocalDate date
) {
    // 1. Fetch doctor
    DoctorProfileDTO doctor = doctorService.getDoctorById(doctorId);
    
    // 2. Validate date is in available days
    if (!isAvailableDay(doctor, date)) {
        return Collections.emptyList();
    }
    
    // 3. Check if date is blocked
    if (isBlockedDate(doctor, date)) {
        return Collections.emptyList();
    }
    
    // 4. Check weekend restriction
    if (isWeekend(date) && !doctor.getSchedulingConfig().getAllowWeekendBooking()) {
        return Collections.emptyList();
    }
    
    // 5. Generate slots
    List<AppointmentSlotDTO> slots = new ArrayList<>();
    for (TimeSlot timeSlot : doctor.getAvailableTimeSlots()) {
        slots.addAll(generateSlotsForTimeRange(
            timeSlot, 
            doctor.getSchedulingConfig(), 
            date
        ));
    }
    
    // 6. Mark availability based on existing appointments
    markSlotAvailability(slots, doctor.getDoctorId(), date);
    
    // 7. Filter out past slots for same-day booking
    if(date.equals(LocalDate.now())) {
        slots = filterPastSlots(slots);
    }
    
    return slots;
}
```

**Step 3**: Implement helper methods

```java
private List<AppointmentSlotDTO> generateSlotsForTimeRange(
    TimeSlot timeSlot, 
    DoctorSchedulingConfig config, 
    LocalDate date
) {
    List<AppointmentSlotDTO> slots = new ArrayList<>();
    
    LocalTime startTime = parseTime(timeSlot.getStartTime());
    LocalTime endTime = parseTime(timeSlot.getEndTime());
    
    int treatmentDuration = config.getTreatmentDurationMinutes();
    int cooldown = config.getCooldownMinutes();
    int totalDuration = treatmentDuration + cooldown;
    
    LocalTime currentTime = startTime;
    
    // CRITICAL: Enforce boundary - slot must end within time range
    while (currentTime.plusMinutes(treatmentDuration).isBefore(endTime) ||
           currentTime.plusMinutes(treatmentDuration).equals(endTime)) {
        
        AppointmentSlotDTO slot = new AppointmentSlotDTO();
        slot.setStartTime(toDate(date, currentTime));
        slot.setEndTime(toDate(date, currentTime.plusMinutes(treatmentDuration)));
        slot.setDurationMinutes(treatmentDuration);
        slot.setAvailable(true); // Will be updated later
        
        slots.add(slot);
        currentTime = currentTime.plusMinutes(totalDuration);
    }
    
    return slots;
}

private void markSlotAvailability(
    List<AppointmentSlotDTO> slots, 
    String doctorId, 
    LocalDate date
) {
    // Fetch all appointments for this doctor on this date
    List<AppointmentDTO> appointments = appointmentService
        .getAppointmentsByDoctorAndDate(doctorId, date);
    
    for (AppointmentSlotDTO slot : slots) {
        boolean hasConflict = appointments.stream()
            .anyMatch(appt -> isOverlapping(slot, appt));
        
        slot.setAvailable(!hasConflict);
    }
}

private boolean isOverlapping(AppointmentSlotDTO slot, AppointmentDTO appointment) {
    Date slotStart = slot.getStartTime();
    Date slotEnd = slot.getEndTime();
    Date apptStart = appointment.getAppointmentDateTime();
    Date apptEnd = addMinutes(apptStart, appointment.getEstimatedDurationMinutes());
    
    // Check if ranges overlap
    return slotStart.before(apptEnd) && slotEnd.after(apptStart);
}
```

**Validation**:
- [ ] Service generates correct number of slots
- [ ] Slots respect time boundaries (no overflow)
- [ ] Overlap detection works correctly
- [ ] Weekend filter works
- [ ] Blocked dates are respected
- [ ] Same-day booking filters past slots

---

#### Task 2.3: Write Unit Tests (2 hours)

**File to Create**:
- `src/test/java/com/heal/doctor/services/SlotGenerationServiceTest.java`

**Test Cases**:
```java
@Test
void testGenerateSlots_Normal() {
    // Given: Doctor with 9-5 schedule, 30 min slots
    // When: Generate for valid date
    // Then: Should return 16 slots (8 hours * 2)
}

@Test
void testGenerateSlots_RespectsBoundary() {
    // Given: Slot 9:00-9:30, treatment 30 min
    // When: Generate slots
    // Then: Last slot should be 9:00, not 9:10
}

@Test
void testGenerateSlots_WeekendDisabled() {
    // Given: Weekend booking disabled
    // When: Generate for Saturday
    // Then: Should return empty list
}

@Test
void testMarkAvailability_WithExistingAppointment() {
    // Given: Appointment at 9:00
    // When: Mark slot availability
    // Then: 9:00 slot should be unavailable
}

@Test
void testFilterPastSlots_SameDay() {
    // Given: Current time 10:00 AM
    // When: Generate slots for today
    // Then: Should not include 9:00 AM slot
}
```

---

### Phase 2 Acceptance Criteria
- [ ] SlotGenerationService generates slots correctly
- [ ] Boundary enforcement works (no overflow)
- [ ] Overlap detection is accurate
- [ ] Weekend filter functional
- [ ] Blocked dates handled
- [ ] Same-day past slot filtering works
- [ ] All unit tests pass (>80% coverage)

---

## Phase 3: Enhanced Booking Validation

**Duration**: 2-3 days  
**Priority**: Critical  
**Assigned To**: Backend Developer

### Objectives
- Add consultation type validation
- Implement strict no-overlap checking
- Preserve estimated duration
- Handle all edge cases

### Tasks

#### Task 3.1: Add Validation Helper Methods (1 hour)

**File to Modify**:
- `AppointmentServiceImpl.java`

**Instructions**:

Add these private methods:

```java
private void validateConsultationType(
    DoctorProfileDTO doctor, 
    ConsultationType consultationType
) {
    if (consultationType == ConsultationType.VIDEO_CALL) {
        if (!doctor.getAcceptsVideoConsultations()) {
            throw new ValidationException(
                "Doctor does not offer video consultations"
            );
        }
        
        if (doctor.getAppointmentMode() == AppointmentMode.IN_PERSON_ONLY) {
            throw new ValidationException(
                "Doctor only accepts in-person visits"
            );
        }
    }
    
    if (consultationType == ConsultationType.IN_PERSON_VISIT) {
        if (doctor.getAppointmentMode() == AppointmentMode.VIDEO_CALL_ONLY) {
            throw new ValidationException(
                "Doctor only offers video consultations"
            );
        }
    }
}

private void validateStrictNoOverlap(
    DoctorProfileDTO doctor,
    Date appointmentDateTime,
    Integer durationMinutes,
    Boolean isEmergency
) {
    if (isEmergency) {
        return; // Emergency appointments bypass strict rules
    }
    
    DoctorSchedulingConfig config = doctor.getSchedulingConfig();
    if (config == null || !config.getStrictNoOverlapping()) {
        return; // Strict mode not enabled
    }
    
    Date slotEndTime = addMinutes(appointmentDateTime, durationMinutes);
    
    boolean hasConflict = appointmentRepository.existsByDoctorIdAndTimeOverlap(
        doctor.getDoctorId(),
        appointmentDateTime,
        slotEndTime,
        AppointmentStatus.ACCEPTED
    );
    
    if (hasConflict) {
        throw new ConflictException("Time slot not available");
    }
}

private void validateDateRestrictions(
    DoctorProfileDTO doctor,
    Date appointmentDateTime
) {
    DoctorSchedulingConfig config = doctor.getSchedulingConfig();
    if (config == null) return;
    
    LocalDate appointmentDate = toLocalDate(appointmentDateTime);
    
    // Weekend check
    if (!config.getAllowWeekendBooking() && isWeekend(appointmentDate)) {
        throw new ValidationException("Weekend bookings not allowed");
    }
    
    // Blocked date check
    if (config.getBlockedDates() != null && 
        config.getBlockedDates().contains(appointmentDate)) {
        throw new ValidationException("Selected date is blocked by doctor");
    }
}
```

---

#### Task 3.2: Modify bookAppointment() Method (2 hours)

**File to Modify**:
- `AppointmentServiceImpl.java`

**Instructions**:

Update the method to add new validations:

```java
@Override
public AppointmentDTO bookAppointment(AppointmentRequestDTO requestDTO) {
    // Existing validations...
    
    // NEW: Fetch doctor
    DoctorProfileDTO doctor = doctorService.getDoctorById(requestDTO.getDoctorId());
    
    // NEW: Validate consultation type
    ConsultationType consultationType = requestDTO.getConsultationType();
    if (consultationType == null) {
        consultationType = ConsultationType.IN_PERSON_VISIT; // Default
    }
    validateConsultationType(doctor, consultationType);
    
    // NEW: Set estimated duration
    Integer estimatedDuration = doctor.getSchedulingConfig() != null ?
        doctor.getSchedulingConfig().getTreatmentDurationMinutes() : 30;
    
    // NEW: Validate strict no-overlap
    validateStrictNoOverlap(
        doctor,
        requestDTO.getAppointmentDateTime(),
        estimatedDuration,
        requestDTO.getIsEmergency()
    );
    
    // NEW: Validate date restrictions
    validateDateRestrictions(doctor, requestDTO.getAppointmentDateTime());
    
    // Build appointment entity
    AppointmentEntity appointment = AppointmentEntity.builder()
        // ... existing fields ...
        .consultationType(consultationType)
        .estimatedDurationMinutes(estimatedDuration)
        .callStatus(AppointmentCallStatus.SCHEDULED)
        .build();
    
    // Save and return...
}
```

---

#### Task 3.3: Add Repository Method for Overlap Check (1 hour)

**File to Modify**:
- `AppointmentRepository.java`

**Instructions**:

Add custom query method:

```java
@Query("{ 'doctorId': ?0, " +
       "'appointmentDateTime': { $lt: ?2 }, " +
       "'$expr': { " +
       "  $gt: [ " +
       "    { $add: ['$appointmentDateTime', " +
       "            { $multiply: ['$estimatedDurationMinutes', 60000] }] }, " +
       "    ?1 " +
       "  ] " +
       "}, " +
       "'status': ?3 }")
boolean existsByDoctorIdAndTimeOverlap(
    String doctorId,
    Date startTime,
    Date endTime,
    AppointmentStatus status
);

// For rescheduling (exclude current appointment)
@Query("{ 'doctorId': ?0, " +
       "'_id': { $ne: ?3 }, " +
       "'appointmentDateTime': { $lt: ?2 }, " +
       "'$expr': { " +
       "  $gt: [ " +
       "    { $add: ['$appointmentDateTime', " +
       "            { $multiply: ['$estimatedDurationMinutes', 60000] }] }, " +
       "    ?1 " +
       "  ] " +
       "}, " +
       "'status': ?4 }")
boolean existsByDoctorIdAndTimeOverlapExcluding(
    String doctorId,
    Date startTime,
    Date endTime,
    String excludeAppointmentId,
    AppointmentStatus status
);
```

---

#### Task 3.4: Write Integration Tests (2 hours)

**File to Create**:
- `src/test/java/com/heal/doctor/services/AppointmentBookingValidationTest.java`

**Test Cases**:
```java
@Test
void testBookAppointment_ConsultationTypeValidation() {
    // Given: Doctor with IN_PERSON_ONLY mode
    // When: Try to book VIDEO_CALL
    // Then: Should throw ValidationException
}

@Test
void testBookAppointment_StrictNoOverlap() {
    // Given: Appointment exists at 9:00
    // When: Try to book at 9:15 (overlaps)
    // Then: Should throw ConflictException
}

@Test
void testBookAppointment_EmergencyBypass() {
    // Given: Appointment exists at 9:00
    // When: Book emergency at 9:15
    // Then: Should succeed (bypass strict rules)
}

@Test
void testBookAppointment_PreservesEstimatedDuration() {
    // Given: Doctor with 30 min duration
    // When: Book appointment
    // Then: estimatedDurationMinutes should be 30
}
```

---

### Phase 3 Acceptance Criteria
- [ ] Consultation type validation works
- [ ] Strict no-overlap checking prevents conflicts
- [ ] Estimated duration is preserved at booking
- [ ] Emergency appointments bypass rules
- [ ] Weekend validation works
- [ ] Blocked dates are enforced
- [ ] Repository overlap query is accurate
- [ ] All integration tests pass

---

## Phase 4: Doctor Configuration APIs

**Duration**: 1-2 days  
**Priority**: Medium  
**Assigned To**: Backend Developer

### Objectives
- Create endpoints for appointment mode configuration
- Implement scheduling config management
- Add proper authorization
- Validate configuration changes

### Tasks

#### Task 4.1: Create Configuration Service (2 hours)

**File to Create**:
- `IDoctorConfigurationService.java`
- `DoctorConfigurationServiceImpl.java`

**Instructions**:

```java
// Interface
public interface IDoctorConfigurationService {
    DoctorDTO updateAppointmentMode(
        String doctorId, 
        UpdateAppointmentModeDTO dto
    );
    
    DoctorDTO updateSchedulingConfig(
        String doctorId,
        DoctorSchedulingConfigDTO config
    );
    
    DoctorSchedulingConfigDTO getSchedulingConfig(String doctorId);
    
    Integer countUpcomingAppointments(String doctorId);
}

// Implementation
@Service
public class DoctorConfigurationServiceImpl 
    implements IDoctorConfigurationService {
    
    @Override
    public DoctorDTO updateAppointmentMode(
        String doctorId, 
        UpdateAppointmentModeDTO dto
    ) {
        // 1. Fetch doctor
        DoctorEntity doctor = doctorRepository.findByDoctorId(doctorId)
            .orElseThrow(() -> new NotFoundException("Doctor not found"));
        
        // 2. Validate video mode requires strict no-overlap
        if (dto.getAppointmentMode() == AppointmentMode.VIDEO_CALL_ONLY ||
            dto.getAppointmentMode() == AppointmentMode.BOTH) {
            
            if (dto.getSchedulingConfig() == null ||
                !dto.getSchedulingConfig().getStrictNoOverlapping()) {
                throw new ValidationException(
                    "Video consultations require strict no-overlapping mode"
                );
            }
            
            if (dto.getSchedulingConfig().getTreatmentDurationMinutes() == null) {
                throw new ValidationException(
                    "Treatment duration is required for video consultations"
                );
            }
        }
        
        // 3. Update fields
        doctor.setAppointmentMode(dto.getAppointmentMode());
        doctor.setSchedulingConfig(dto.getSchedulingConfig());
        doctor.setAcceptsVideoConsultations(
            dto.getAppointmentMode() != AppointmentMode.IN_PERSON_ONLY
        );
        
        // 4. Save
        doctor = doctorRepository.save(doctor);
        
        // 5. Clear cache
        cacheEvict(doctorId);
        
        // 6. Return DTO
        return mapper.toDTO(doctor);
    }
}
```

---

#### Task 4.2: Add Controller Endpoints (1 hour)

**File to Modify**:
- `DoctorController.java`

**Instructions**:

```java
@PutMapping("/{doctorId}/appointment-mode")
@PreAuthorize("hasRole('DOCTOR') and @roleUtils.isOwner(#doctorId)")
public ResponseEntity<ApiResponse<DoctorDTO>> updateAppointmentMode(
    @PathVariable String doctorId,
    @Valid @RequestBody UpdateAppointmentModeDTO dto
) {
    // Count upcoming appointments for warning
    Integer upcomingCount = configurationService
        .countUpcomingAppointments(doctorId);
    
    DoctorDTO doctor = configurationService
        .updateAppointmentMode(doctorId, dto);
    
    String message = upcomingCount > 0 ?
        "Configuration updated. " + upcomingCount + 
        " existing appointments will keep their original duration." :
        "Configuration updated successfully";
    
    return ResponseEntity.ok(ApiResponse.<DoctorDTO>builder()
        .success(true)
        .message(message)
        .data(doctor)
        .build());
}

@GetMapping("/{doctorId}/scheduling-config")
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'COLLABORATOR')")
public ResponseEntity<ApiResponse<DoctorSchedulingConfigDTO>> getSchedulingConfig(
    @PathVariable String doctorId
) {
    DoctorSchedulingConfigDTO config = configurationService
        .getSchedulingConfig(doctorId);
    
    return ResponseEntity.ok(ApiResponse.<DoctorSchedulingConfigDTO>builder()
        .success(true)
        .message("Configuration retrieved")
        .data(config)
        .build());
}
```

---

### Phase 4 Acceptance Criteria
- [ ] Update appointment mode endpoint works
- [ ] Get scheduling config endpoint works
- [ ] Authorization enforced (only doctor owner can update)
- [ ] Video mode requires strict no-overlap
- [ ] Configuration changes validated
- [ ] Cache invalidated on update
- [ ] Warning shown for existing appointments

---

## Phase 5: Call Tracking & No-Show Detection

**Duration**: 2 days  
**Priority**: Medium  
**Assigned To**: Backend Developer

### Objectives
- Track video call join/leave events
- Implement no-show detection
- Handle brief disconnections
- Auto-release slots

### Tasks

#### Task 5.1: Create Call Tracking Service (3 hours)

**File to Create**:
- `ICallTrackingService.java`
- `CallTrackingServiceImpl.java`

**Instructions**:

```java
public interface ICallTrackingService {
    void onUserJoined(String appointmentId, String userType);
    void onUserLeft(String appointmentId, String userType, String reason);
    void checkCallDuration(String appointmentId);
}

@Service
public class CallTrackingServiceImpl implements ICallTrackingService {
    
    @Override
    public void onUserJoined(String appointmentId, String userType) {
        AppointmentEntity appointment = appointmentRepository
            .findByAppointmentId(appointmentId)
            .orElseThrow();
        
        Date now = new Date();
        
        if ("PATIENT".equals(userType)) {
            appointment.setPatientJoinedAt(now);
            
            if (appointment.getDoctorJoinedAt() != null) {
                // Both now connected
                appointment.setActualCallStartTime(now);
                appointment.setCallStatus(AppointmentCallStatus.IN_PROGRESS);
            } else {
                appointment.setCallStatus(AppointmentCallStatus.PATIENT_WAITING);
            }
        } else {
            appointment.setDoctorJoinedAt(now);
            
            if (appointment.getPatientJoinedAt() != null) {
                appointment.setActualCallStartTime(now);
                appointment.setCallStatus(AppointmentCallStatus.IN_PROGRESS);
            } else {
                appointment.setCallStatus(AppointmentCallStatus.DOCTOR_WAITING);
            }
        }
        
        appointmentRepository.save(appointment);
        broadcastUpdate(appointment);
    }
    
    @Override
    public void onUserLeft(String appointmentId, String userType, String reason) {
        AppointmentEntity appointment = appointmentRepository
            .findByAppointmentId(appointmentId)
            .orElseThrow();
        
        Date now = new Date();
        
        if ("PATIENT".equals(userType)) {
            appointment.setPatientLeftAt(now);
        } else {
            appointment.setDoctorLeftAt(now);
        }
        
        appointment.setDisconnectReason(reason);
        appointment.setActualCallEndTime(now);
        
        // Calculate duration
        if (appointment.getActualCallStartTime() != null) {
            int duration = secondsBetween(
                appointment.getActualCallStartTime(), 
                now
            );
            appointment.setActualCallDurationSeconds(duration);
            
            // Determine status based on duration
            DoctorSchedulingConfig config = getConfig(appointment.getDoctorId());
            int minDuration = config.getMinimumCallDurationMinutes() * 60;
            
            if (duration < 120) {
                // Brief disconnection
                appointment.setCallStatus(AppointmentCallStatus.BRIEF_DISCONNECTION);
                scheduleReconnectionWindow(appointment, config);
            } else if (duration < minDuration) {
                // Incomplete
                appointment.setCallStatus(AppointmentCallStatus.INCOMPLETE_CONSULTATION);
                notifyDoctorForStatus(appointment);
            } else {
                // Complete
                appointment.setCallStatus(AppointmentCallStatus.COMPLETED);
                appointment.setTreated(true);
            }
        }
        
        appointmentRepository.save(appointment);
        broadcastUpdate(appointment);
    }
}
```

---

#### Task 5.2: Create No-Show Detection Scheduler (2 hours)

**File to Create**:
- `NoShowDetectionScheduler.java`

**Instructions**:

```java
@Component
public class NoShowDetectionScheduler {
    
    private final AppointmentRepository appointmentRepository;
    private final ISlotManagementService slotManagementService;
    private final INotificationService notificationService;
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void detectNoShows() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find all scheduled video calls
        List<AppointmentEntity> scheduled = appointmentRepository
            .findByConsultationTypeAndCallStatus(
                ConsultationType.VIDEO_CALL,
                AppointmentCallStatus.SCHEDULED
            );
        
        for (AppointmentEntity appt : scheduled) {
            DoctorSchedulingConfig config = getConfig(appt.getDoctorId());
            int gracePeriod = config.getNoShowGracePeriodMinutes();
            
            LocalDateTime deadline = toLocalDateTime(appt.getAppointmentDateTime())
                .plusMinutes(gracePeriod);
            
            if (now.isAfter(deadline)) {
                handleNoShow(appt, config);
            }
        }
    }
    
    private void handleNoShow(AppointmentEntity appt, DoctorSchedulingConfig config) {
        if (appt.getPatientJoinedAt() == null && appt.getDoctorJoinedAt() == null) {
            // Neither joined
            appt.setStatus(AppointmentStatus.NO_SHOW);
            appt.setCallStatus(AppointmentCallStatus.PATIENT_NO_SHOW);
            
            if (config.getAutoReleaseNoShowSlots()) {
                slotManagementService.releaseSlot(appt);
            }
        } else if (appt.getPatientJoinedAt() != null && 
                   appt.getDoctorJoinedAt() == null) {
            // Doctor no-show
            appt.setCallStatus(AppointmentCallStatus.DOCTOR_NO_SHOW);
            notificationService.offerReschedule(appt);
        } else if (appt.getDoctorJoinedAt() != null && 
                   appt.getPatientJoinedAt() == null) {
            // Patient no-show
            appt.setStatus(AppointmentStatus.NO_SHOW);
            appt.setCallStatus(AppointmentCallStatus.PATIENT_NO_SHOW);
            
            if (config.getAutoReleaseNoShowSlots()) {
                slotManagementService.releaseSlot(appt);
            }
        }
        
        appointmentRepository.save(appt);
    }
}
```

---

### Phase 5 Acceptance Criteria
- [ ] Join/leave events tracked correctly
- [ ] Call duration calculated accurately
- [ ] Brief disconnection detected
- [ ] No-show detection runs every 5 min
- [ ] Slots auto-released when configured
- [ ] Reconnection windows work
- [ ] Notifications sent appropriately

---

## Phase 6-8: Summary

Due to length constraints, Phases 6-8 are summarized:

### Phase 6: Frontend Implementation (3-4 days)
- Doctor settings UI for appointment mode
- Patient booking flow with slot selection
- Video call interface
- Real-time updates via WebSocket

### Phase 7: Integration & Reports (1-2 days)
- Update report generation
- Enhance statistics
- WebSocket broadcasting
- Email templates

### Phase 8: Testing & Bug Fixes (2-3 days)
- Unit tests (>80% coverage)
- Integration tests
- Manual testing scenarios
- Performance testing
- Bug fixes

---

## Timeline

| Phase | Duration | Dependencies | Start After |
|-------|----------|--------------|-------------|
| Phase 1 | 1-2 days | None | Day 1 |
| Phase 2 | 2-3 days | Phase 1 | Day 3 |
| Phase 3 | 2-3 days | Phase 1, 2 | Day 6 |
| Phase 4 | 1-2 days | Phase 1 | Day 9 |
| Phase 5 | 2 days | Phase 1, 3 | Day 11 |
| Phase 6 | 3-4 days | Phase 1-5 | Day 13 |
| Phase 7 | 1-2 days | Phase 1-6 | Day 17 |
| Phase 8 | 2-3 days | All | Day 19 |

**Total**: 14-22 days

---

**End of Implementation Phases**
