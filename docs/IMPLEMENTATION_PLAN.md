# Implementation Plan: Video Consultation & Strict No-Overlapping Appointments

## Table of Contents
1. [Overview](#overview)
2. [Terminology](#terminology)
3. [Requirements](#requirements)
4. [Data Models](#data-models)
5. [Business Rules](#business-rules)
6. [Validation Rules](#validation-rules)
7. [Critical Edge Cases](#critical-edge-cases)
8. [API Specifications](#api-specifications)
9. [Frontend Requirements](#frontend-requirements)
10. [Database Changes](#database-changes)

---

## Overview

This implementation adds support for video consultation appointments alongside traditional in-person visits, with intelligent slot management to prevent scheduling conflicts when doctors can only attend one patient at a time via video call.

**Key Capabilities:**
- Doctors can offer in-person visits only, video consultations only, or both
- Strict no-overlapping scheduling for video appointments (one patient at a time)
- Flexible scheduling for in-person visits (multiple patients can wait)
- Intelligent time slot generation based on treatment duration
- Comprehensive call tracking and no-show detection
- Automatic slot management with reconnection support

---

## Terminology

### Important Distinction

**Booking Source** (Existing - `AppointmentType` enum):
- `IN_PERSON` = Appointment booked directly by doctor
- `ONLINE` = Appointment self-booked by patient from website
- **This tracks WHO booked the appointment**

**Consultation Method** (NEW - `ConsultationType` enum):
- `IN_PERSON_VISIT` = Patient physically visits clinic/hospital
- `VIDEO_CALL` = Doctor treats patient via online video consultation
- **This tracks HOW the consultation is delivered**

### Example Combinations
- Doctor books appointment (`IN_PERSON`) for video call (`VIDEO_CALL`)
- Patient self-books (`ONLINE`) for in-person visit (`IN_PERSON_VISIT`)
- Patient self-books (`ONLINE`) for video call (`VIDEO_CALL`)

These are independent fields serving different purposes.

---

## Requirements

### Functional Requirements

#### FR1: Doctor Configuration
- Doctors must be able to select appointment mode:
  - **In-Person Only**: Traditional clinic visits
  - **Video Consultations Only**: Remote treatments only
  - **Both**: Mixed mode offering both types

#### FR2: Strict No-Overlapping
- When doctor enables video consultations, strict no-overlapping MUST be enforced
- Doctor must configure:
  - Average treatment duration (5-480 minutes)
  - Cooldown time between appointments (0-60 minutes)
  - Weekend booking preference
  - Blocked dates (holidays, vacations)

#### FR3: Slot Generation
- System must auto-generate bookable time slots based on:
  - Doctor's available days (e.g., Monday-Friday)
  - Doctor's time ranges (e.g., 9:00 AM - 12:00 PM)
  - Treatment duration + cooldown period
  - Existing bookings
- Slots must respect time boundaries (no overflow)

#### FR4: Booking Validation
- Prevent double-booking when strict mode enabled
- Validate consultation type matches doctor's mode
- Check weekend/holiday restrictions
- Enforce slot boundaries

#### FR5: Call Tracking
- Track when patient joins/leaves call
- Track when doctor joins/leaves call
- Calculate actual call duration
- Detect no-shows automatically
- Support reconnection for brief disconnections

#### FR6: No-Show Management
- Auto-detect no-shows after grace period (configurable, default 15 min)
- Distinguish between patient no-show and doctor no-show
- Auto-release slots when configured
- Notify relevant parties

#### FR7: Existing Appointment Protection
- Configuration changes (duration, cooldown) only affect NEW bookings
- Existing appointments retain original duration
- No forced rescheduling when doctor updates settings

### Non-Functional Requirements

#### NFR1: Performance
- Slot generation must complete within 2 seconds for 30-day range
- Concurrent booking prevention via database-level locking
- Cache frequently accessed slot data

#### NFR2: Backward Compatibility
- Existing appointments default to `IN_PERSON_VISIT` consultation type
- Existing doctors default to `IN_PERSON_ONLY` mode
- All current features continue working (reports, statistics, notifications)

#### NFR3: Real-Time Updates
- WebSocket broadcasts for slot availability changes
- Live notifications for running over time
- Patient delay notifications

---

## Data Models

### New Enums

#### ConsultationType
```java
public enum ConsultationType {
    IN_PERSON_VISIT,    // Patient visits clinic
    VIDEO_CALL          // Online video consultation
}
```

#### AppointmentMode
```java
public enum AppointmentMode {
    IN_PERSON_ONLY,     // Only accepts in-person visits (default)
    VIDEO_CALL_ONLY,    // Only accepts video consultations
    BOTH                // Accepts both types
}
```

#### AppointmentCallStatus
```java
public enum AppointmentCallStatus {
    SCHEDULED,              // Appointment not started yet
    PATIENT_WAITING,        // Patient joined, waiting for doctor
    DOCTOR_WAITING,         // Doctor joined, waiting for patient
    IN_PROGRESS,            // Both connected, call active
    BRIEF_DISCONNECTION,    // Call less than 2 minutes (likely technical)
    INCOMPLETE_CONSULTATION,// Call 2-5 minutes (needs doctor input)
    TECHNICAL_FAILURE,      // Connection failed
    COMPLETED,              // Successful completion
    PATIENT_NO_SHOW,        // Patient didn't join by grace period
    DOCTOR_NO_SHOW          // Doctor didn't join (patient was waiting)
}
```

#### AppointmentStatus (Modified)
```java
public enum AppointmentStatus {
    ACCEPTED,
    BOOKED,
    CANCELLED,
    NO_SHOW     // NEW: Patient/doctor didn't attend
}
```

### New Classes

#### DoctorSchedulingConfig
Embedded configuration object for scheduling settings.

```java
@Data
public class DoctorSchedulingConfig {
    // Basic scheduling
    private Boolean strictNoOverlapping = false;
    
    @Min(5) @Max(480) 
    private Integer treatmentDurationMinutes;
    
    @Min(0) @Max(60) 
    private Integer cooldownMinutes = 0;
    
    private Boolean allowWeekendBooking = true;
    
    private List<LocalDate> blockedDates; // Holidays, vacations
    
    // No-show detection
    @Min(5) @Max(30) 
    private Integer noShowGracePeriodMinutes = 15;
    
    // Call duration tracking
    @Min(2) @Max(10)
    private Integer minimumCallDurationMinutes = 5;
    
    @Min(1) @Max(10)
    private Integer reconnectionWindowMinutes = 5;
    
    // Auto-availability
    private Boolean autoReleaseNoShowSlots = true;
}
```

### Modified Entities

#### DoctorEntity (Add fields)
```java
// Appointment mode configuration
@Builder.Default
private AppointmentMode appointmentMode = AppointmentMode.IN_PERSON_ONLY;

// Scheduling configuration (required if video consultations enabled)
@Valid
private DoctorSchedulingConfig schedulingConfig;

// Quick flag for video consultation support
@Builder.Default
private Boolean acceptsVideoConsultations = false;
```

**Constraint**: If `appointmentMode` is `VIDEO_CALL_ONLY` or `BOTH`, then `schedulingConfig.strictNoOverlapping` must be `true`.

#### AppointmentEntity (Add fields)
```java
// Consultation delivery method
private ConsultationType consultationType;

// Duration captured at booking time (preserved even if doctor changes config)
private Integer estimatedDurationMinutes;

// Video call tracking
private Date patientJoinedAt;
private Date patientLeftAt;
private Date doctorJoinedAt;
private Date doctorLeftAt;
private Date actualCallStartTime;    // When both connected
private Date actualCallEndTime;
private Integer actualCallDurationSeconds;
private AppointmentCallStatus callStatus;
private String disconnectReason;

// Video call link (for future platform integration)
private String videoCallLink;
```

---

## Business Rules

### BR1: Appointment Mode Constraints

**Rule**: Video consultation mode requires strict no-overlapping.

```
IF appointmentMode = VIDEO_CALL_ONLY OR appointmentMode = BOTH
THEN schedulingConfig.strictNoOverlapping MUST = true
AND schedulingConfig.treatmentDurationMinutes MUST be set
```

**Rationale**: Doctor can only attend one video call at a time, unlike in-person where multiple patients can wait.

### BR2: Consultation Type Compatibility

**Rule**: Consultation type must match doctor's appointment mode.

```
IF consultationType = VIDEO_CALL
THEN doctor.appointmentMode MUST be VIDEO_CALL_ONLY OR BOTH

IF consultationType = IN_PERSON_VISIT  
THEN doctor.appointmentMode MUST be IN_PERSON_ONLY OR BOTH
```

**Validation Location**: `AppointmentServiceImpl.bookAppointment()` and `selfBookAppointment()`

###BR3: Slot Boundary Enforcement

**Rule**: Appointments cannot extend beyond configured time slot boundaries.

```
For every generated slot:
  slotStartTime + treatmentDuration ≤ timeSlotEndTime

Example:
  Doctor slot: 9:00 AM - 9:30 AM
  Treatment: 30 minutes
  ✅ Valid: 9:00 AM slot (ends at 9:30 AM)
  ❌ Invalid: 9:10 AM slot (would end at 9:40 AM, 10 min over)
```

**Implementation**: Slot generation algorithm must validate boundaries.

### BR4: Configuration Change Isolation

**Rule**: Configuration changes only affect future bookings.

```
WHEN doctor changes treatmentDurationMinutes:
  - New bookings use new duration
  - Existing appointments keep estimatedDurationMinutes from booking time
  - No automatic rescheduling
  - Display warning showing count of affected future dates
```

### BR5: No-Show Grace Period

**Rule**: Appointments marked as no-show only after grace period expires.

```
IF currentTime > (appointmentDateTime + gracePeriodMinutes)
AND (patientJoinedAt = null OR doctorJoinedAt = null)
THEN mark as NO_SHOW
```

**Default**: 15 minutes grace period (configurable per doctor)

### BR6: Reconnection Window

**Rule**: Brief disconnections allow reconnection without losing slot.

```
IF callDuration < 120 seconds (2 minutes)
THEN:
  - Set callStatus = BRIEF_DISCONNECTION
  - Hold slot for reconnectionWindowMinutes (default 5 min)
  - Offer reconnection to both parties
  - After window expires, release slot
```

### BR7: Emergency Appointment Override

**Rule**: Emergency appointments bypass strict no-overlapping.

```
IF appointment.isEmergency = true
THEN skip slot availability validation
```

**Rationale**: Medical emergencies take priority over scheduling rules.

### BR8: Treatment Status for Video Calls

**Rule**: Video call appointments don't require `availableAtClinic` to mark as treated.

```
IF consultationType = VIDEO_CALL
THEN allow updateTreatedStatus() without availableAtClinic check

IF consultationType = IN_PERSON_VISIT
THEN require availableAtClinic = true before marking treated
```

---

## Validation Rules

### Booking Validation Sequence

#### V1: Basic Input Validation
- Patient name required and valid
- Contact number exactly 10 digits
- Appointment date/time required
- Consultation type required

#### V2: Doctor Compatibility Validation
```java
// Check if doctor offers this consultation type
if (consultationType == VIDEO_CALL && !doctor.getAcceptsVideoConsultations()) {
    throw new ValidationException("Doctor doesn't offer video consultations");
}

// Check mode compatibility
if (consultationType == VIDEO_CALL && 
    doctor.getAppointmentMode() == IN_PERSON_ONLY) {
    throw new ValidationException("Doctor only accepts in-person visits");
}

if (consultationType == IN_PERSON_VISIT && 
    doctor.getAppointmentMode() == VIDEO_CALL_ONLY) {
    throw new ValidationException("Doctor only offers video consultations");
}
```

#### V3: Time Validation
```java
// For scheduled appointments (not walk-ins)
if (!availableAtClinic && appointmentDateTime <= currentTime) {
    throw new ValidationException("Scheduled appointments must be in future");
}

// Weekend validation
if (!schedulingConfig.getAllowWeekendBooking() && isWeekend(appointmentDateTime)) {
    throw new ValidationException("Weekend bookings not allowed");
}

// Blocked date validation
if (schedulingConfig.getBlockedDates().contains(appointmentDate)) {
    throw new ValidationException("Date is blocked by doctor");
}
```

#### V4: Slot Availability Validation
```java
if (doctor.getSchedulingConfig().getStrictNoOverlapping()) {
    // Calculate slot end time using appointment's duration
    Date slotEndTime = addMinutes(
        appointmentDateTime,
        appointment.getEstimatedDurationMinutes()
    );
    
    // Check for conflicts (excluding emergencies)
    if (!appointment.getIsEmergency()) {
        boolean hasConflict = appointmentRepository
            .existsByDoctorIdAndTimeOverlap(
                doctorId, 
                appointmentDateTime, 
                slotEndTime,
                AppointmentStatus.ACCEPTED
            );
        
        if (hasConflict) {
            throw new ConflictException("Time slot not available");
        }
    }
}
```

#### V5: Duplicate Prevention
```java
// Check for existing appointment same day for same patient
boolean exists = appointmentRepository
    .existsByDoctorIdAndPatientNameAndContactAndAppointmentDateTimeBetween(
        doctorId,
        patientName,
        contact,
        startOfDay,
        endOfDay,
        AppointmentStatus.ACCEPTED
    );

if (exists) {
    throw new ConflictException("Appointment already exists for this patient");
}
```

### Rescheduling Validation

When updating appointment date/time:
```java
if (updateDTO.getAppointmentDateTime() != null && 
    doctor.getSchedulingConfig().getStrictNoOverlapping()) {
    
    Date newEndTime = addMinutes(
        updateDTO.getAppointmentDateTime(),
        appointment.getEstimatedDurationMinutes()
    );
    
    // Check conflicts excluding current appointment
    boolean hasConflict = appointmentRepository
        .existsByDoctorIdAndTimeOverlapExcluding(
            doctorId,
            updateDTO.getAppointmentDateTime(),
            newEndTime,
            appointmentId,
            AppointmentStatus.ACCEPTED
        );
    
    if (hasConflict) {
        throw new ConflictException("New time slot not available");
    }
}
```

---

## Critical Edge Cases

### EC1: Slot Boundary Overflow Prevention

**Scenario**: Last appointment of the day might extend beyond configured end time.

**Example**:
- Doctor's slot: 9:00 AM - 9:30 AM
- Treatment duration: 30 minutes
- Question: Can appointment start at 9:10?

**Solution**: NO
- 9:10 + 30 min = 9:40 (extends beyond 9:30)
- Only allow slots where: `startTime + duration ≤ slotEnd`
- Last valid slot: 9:00 AM (ends exactly at 9:30)

**Implementation**:
```java
for (TimeSlot doctorSlot : doctor.getAvailableTimeSlots()) {
    LocalTime slotEnd = parseTime(doctorSlot.getEndTime());
    LocalTime currentStart = parseTime(doctorSlot.getStartTime());
    int slotDuration = treatmentDuration + cooldown;
    
    while (currentStart.plusMinutes(treatmentDuration).isBefore(slotEnd) ||
           currentStart.plusMinutes(treatmentDuration).equals(slotEnd)) {
        
        slots.add(createSlot(currentStart, slotDuration));
        currentStart = currentStart.plusMinutes(slotDuration);
    }
}
```

### EC2: Configuration Change Impact on Existing Bookings

**Scenario**: Doctor changes treatment duration from 30 to 45 minutes.

**Impact**:
- Tomorrow's appointments (already booked): Keep 30-minute duration
- Future bookings: Use new 45-minute duration

**Implementation**:
```java
// At booking time
appointment.setEstimatedDurationMinutes(
    doctor.getSchedulingConfig().getTreatmentDurationMinutes()
);

// When checking conflicts
Date appointmentEnd = addMinutes(
    appointment.getAppointmentDateTime(),
    appointment.getEstimatedDurationMinutes() // Use preserved value
);
```

**UI Warning**:
```
⚠️ Duration change affects NEW bookings only
15 existing appointments over the next 30 days will keep their 30-minute duration.

[Confirm Change]  [Cancel]
```

### EC3: Call Running Over Scheduled Time

**Scenario**: 30-minute video call runs 50 minutes.

**System Behavior**: Alert but don't force-end

**Implementation**:
```java
@Scheduled(fixedRate = 60000) // Every minute
public void monitorActiveCalls() {
    for (Appointment call : findInProgressCalls()) {
        int elapsed = minutesSince(call.getActualCallStartTime());
        int scheduled = call.getEstimatedDurationMinutes();
        
        if (elapsed == scheduled - 2) {
            sendNotification(call.getDoctorId(), 
                "2 minutes remaining for current call");
        }
        
        if (elapsed > scheduled) {
            int overtime = elapsed - scheduled;
            sendAlert(call.getDoctorId(),
                "Running " + overtime + " min over. Next patient waiting.");
            
            // Notify next patient
            Appointment next = findNextAppointment(call.getDoctorId());
            if (next != null) {
                notifyPatient(next, "Doctor running " + overtime + " min late");
            }
        }
    }
}
```

**Doctor Dashboard Display**:
```
🔴 RUNNING LATE: 20 minutes over schedule
   Current: Jane Doe (started 9:00, scheduled 30 min)
   Next: John Smith (9:35 AM) - Waiting
   
   [Send Delay Notification]  [End Current Call]
```

### EC4: No-Show Detection and Slot Release

**Scenario**: Patient doesn't join video call by scheduled time.

**Detection Logic**:
```java
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void detectNoShows() {
    int gracePeriod = schedulingConfig.getNoShowGracePeriodMinutes();
    Date cutoff = addMinutes(now(), -gracePeriod);
    
    List<Appointment> suspected = appointmentRepository
        .findScheduledVideoCallsBefore(cutoff);
    
    for (Appointment appt : suspected) {
        if (appt.getPatientJoinedAt() == null && 
            appt.getDoctorJoinedAt() == null) {
            // Neither joined
            handleDoubleNoShow(appt);
        } else if (appt.getPatientJoinedAt() != null && 
                   appt.getDoctorJoinedAt() == null) {
            // Patient waiting, doctor absent
            handleDoctorNoShow(appt);
        } else if (appt.getDoctorJoinedAt() != null && 
                   appt.getPatientJoinedAt() == null) {
            // Doctor waiting, patient absent
            handlePatientNoShow(appt);
        }
    }
}

private void handlePatientNoShow(Appointment appt) {
    appt.setStatus(NO_SHOW);
    appt.setCallStatus(PATIENT_NO_SHOW);
    
    if (schedulingConfig.getAutoReleaseNoShowSlots()) {
        releaseSlot(appt);
        notifyNextPatientEarlySlot(appt);
    }
    
    appointmentRepository.save(appt);
}
```

### EC5: Brief Disconnection Handling

**Scenario**: Patient joins, disconnects after 1 minute.

**Classification**:
- **< 2 minutes**: Brief disconnection (likely technical)
- **2-5 minutes**: Incomplete consultation (doctor decides)
- **5+ minutes**: Valid consultation (even if shorter than scheduled)

**Implementation**:
```java
public void onUserLeftCall(Appointment appt) {
    int duration = secondsBetween(
        appt.getActualCallStartTime(),
        now()
    );
    appt.setActualCallDurationSeconds(duration);
    
    int minDuration = schedulingConfig.getMinimumCallDurationMinutes() * 60;
    
    if (duration < 120) {
        // Brief disconnection
        appt.setCallStatus(BRIEF_DISCONNECTION);
        
        // Hold slot for reconnection
        int reconnectWindow = schedulingConfig.getReconnectionWindowMinutes();
        scheduleSlotRelease(appt, reconnectWindow);
        
        // Offer reconnection
        sendReconnectionOffer(appt);
        
    } else if (duration < minDuration) {
        // Incomplete consultation
        appt.setCallStatus(INCOMPLETE_CONSULTATION);
        
        // Prompt doctor
        askDoctorCompletionStatus(appt);
        
    } else {
        // Valid consultation
        appt.setCallStatus(COMPLETED);
        appt.setTreated(true);
    }
    
    appointmentRepository.save(appt);
}
```

**Patient UI for Brief Disconnection**:
```
⚠️ Call disconnected after 1 minute

This may have been accidental or a technical issue.

[Rejoin Call] (Available for 4:32)
[Report Technical Issue]
[Reschedule Appointment]
```

### EC6: Concurrent Slot Booking Prevention

**Scenario**: Two patients try to book same slot simultaneously.

**Solution**: Database-level transaction isolation

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public AppointmentDTO bookAppointment(AppointmentRequestDTO requestDTO) {
    // Booking logic with conflict check
    // Second transaction will wait for first to complete
    // Then fail on conflict check
}
```

**Additional Index**:
```javascript
db.appointments.createIndex({
    "doctorId": 1,
    "appointmentDateTime": 1,
    "status": 1
}, { unique: false });
```

### EC7: Timezone Handling for Video Calls

**Scenario**: Patient in different timezone books video call.

**Solution**:
1. Store all times in UTC in database
2. Display times in doctor's configured timezone
3. Show both timezones in confirmation email for video calls

```java
// Booking
appointment.setAppointmentDateTime(convertToUTC(requestedTime, patientTimezone));

// Display
displayTime = convertFromUTC(appointment.getAppointmentDateTime(), doctorTimezone);

// Email
if (appointment.getConsultationType() == VIDEO_CALL) {
    email.addLine("Your time: " + formatInTimezone(time, patientTimezone));
    email.addLine("Doctor time: " + formatInTimezone(time, doctorTimezone));
}
```

### EC8: Collaborator Permission for Configuration

**Scenario**: Should collaborators change doctor's appointment mode?

**Solution**: Read-only for collaborators

```java
@PreAuthorize("hasRole('DOCTOR') and @roleUtils.isOwner(#doctorId)")
public DoctorDTO updateAppointmentMode(String doctorId, UpdateAppointmentModeDTO dto) {
    // Only doctor owner can change mode
}

@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'COLLABORATOR')")
public DoctorSchedulingConfigDTO getSchedulingConfig(String doctorId) {
    // Anyone can view
}
```

---

## API Specifications

### Doctor Configuration Endpoints

#### Update Appointment Mode
```
PUT /api/v1/doctors/{doctorId}/appointment-mode
Authorization: Doctor Owner only

Request Body:
{
  "appointmentMode": "VIDEO_CALL_ONLY" | "IN_PERSON_ONLY" | "BOTH",
  "schedulingConfig": {
    "strictNoOverlapping": true,
    "treatmentDurationMinutes": 30,
    "cooldownMinutes": 5,
    "allowWeekendBooking": true,
    "blockedDates": ["2026-02-15", "2026-02-16"],
    "noShowGracePeriodMinutes": 15,
    "minimumCallDurationMinutes": 5,
    "reconnectionWindowMinutes": 5,
    "autoReleaseNoShowSlots": true
  }
}

Response: DoctorDTO
```

#### Get Scheduling Configuration
```
GET /api/v1/doctors/{doctorId}/scheduling-config
Authorization: Doctor, Admin, Collaborator

Response:
{
  "strictNoOverlapping": true,
  "treatmentDurationMinutes": 30,
  "cooldownMinutes": 5,
  ...
}
```

### Slot Availability Endpoints

#### Get Available Slots
```
GET /api/v1/appointments/public/doctors/{doctorId}/slots?date=2026-02-10
Authorization: Public

Response:
[
  {
    "startTime": "2026-02-10T09:00:00Z",
    "endTime": "2026-02-10T09:30:00Z",
    "available": true,
    "durationMinutes": 30
  },
  {
    "startTime": "2026-02-10T09:35:00Z",
    "endTime": "2026-02-10T10:05:00Z",
    "available": false,
    "durationMinutes": 30
  }
]
```

### Appointment Booking (Modified)

#### Book Appointment
```
POST /api/v1/appointments/book
Authorization: Doctor, Admin, Collaborator

Request Body (add consultationType):
{
  "patientName": "Jane Doe",
  "contact": "9876543210",
  "appointmentDateTime": "2026-02-10T09:00:00Z",
  "consultationType": "VIDEO_CALL",  // NEW
  ...existing fields...
}

Response: AppointmentDTO (includes consultationType, estimatedDurationMinutes, callStatus)
```

### Video Call Tracking WebSocket Events

```
// Patient joins
SEND -> /app/video-call/join
{
  "appointmentId": "...",
  "userType": "PATIENT",
  "timestamp": "2026-02-10T09:00:00Z"
}

// Patient leaves
SEND -> /app/video-call/leave
{
  "appointmentId": "...",
  "userType": "PATIENT",
  "timestamp": "2026-02-10T09:12:00Z",
  "reason": "USER_DISCONNECT" | "TECHNICAL_ERROR"
}

// Subscribe to appointment updates
SUBSCRIBE -> /topic/appointments/{doctorId}
RECEIVE <- {
  "type": "APPOINTMENT_UPDATE",
  "payload": AppointmentDTO
}
```

---

## Frontend Requirements

### Doctor Settings Page

**New Section: "Consultation Mode Configuration"**

**Layout**:
```
┌─────────────────────────────────────┐
│ Consultation Mode                    │
├─────────────────────────────────────┤
│ ○ In-Person Visits Only (Default)   │
│ ○ Video Consultations Only           │
│ ○ Both In-Person and Video           │
└─────────────────────────────────────┘

[If Video or Both selected, show:]

┌─────────────────────────────────────┐
│ Scheduling Configuration             │
├─────────────────────────────────────┤
│ ☑ Strict No-Overlapping (Required)  │
│                                       │
│ Treatment Duration: [30] minutes     │
│ (5-480 min)                          │
│                                       │
│ Cooldown Time: [5] minutes           │
│ (0-60 min) Optional break time       │
│                                       │
│ ☑ Allow Weekend Bookings             │
│                                       │
│ Blocked Dates:                       │
│ [Date Picker - Multi-select]         │
│                                       │
│ Advanced Settings ▼                  │
│   No-Show Grace Period: [15] min    │
│   Minimum Call Duration: [5] min     │
│   Reconnection Window: [5] min       │
│   ☑ Auto-release no-show slots       │
│                                       │
│ [Save Configuration]                 │
└─────────────────────────────────────┘
```

**Validation Messages**:
- Show error if selecting video mode without setting treatment duration
- Display warning when changing duration with existing appointments

### Patient Booking Page

**Step 1: Select Consultation Type**
```
┌─────────────────────────────────────┐
│ Choose Consultation Type             │
├─────────────────────────────────────┤
│ ○ 🏥 In-Person Visit                │
│   Visit doctor at clinic             │
│                                       │
│ ○ 📹 Video Consultation              │
│   Online video call with doctor      │
└─────────────────────────────────────┘
```

**Step 2: Select Date**
```
[Calendar Widget]
- Highlight available days based on doctor's availableDays
- Gray out blocked dates
- Show weekend availability based on config
```

**Step 3: Select Time Slot**

**If Strict Mode Enabled (Video Calls):**
```
┌─────────────────────────────────────┐
│ Available Time Slots - Feb 10       │
├─────────────────────────────────────┤
│ [9:00 AM] ✅  [9:35 AM] (Booked)   │
│ [10:10 AM] ✅  [10:45 AM] ✅        │
│ [11:20 AM] (Booked)                 │
│                                       │
│ Each slot: 30 min + 5 min break     │
└─────────────────────────────────────┘
```

**If Flexible Mode (In-Person):**
```
Time: [HH:MM] [AM/PM]
Note: Multiple patients may have same time slot
```

### Appointment List/Dashboard

**Enhanced Display**:
```
┌─────────────────────────────────────┐
│ 9:00 AM  [📹 VIDEO] Jane Doe        │
│          Contact: 9876543210         │
│          Status: IN_PROGRESS         │
│          ⏱️ 28/30 min (2 min left)  │
│          [End Call] [Extend]         │
├─────────────────────────────────────┤
│ 9:35 AM  [🏥 IN-PERSON] John Smith  │
│          Contact: 9876543211         │
│          Status: PATIENT_WAITING     │
│          🔴 Previous call running    │
│            15 min late               │
└─────────────────────────────────────┘
```

**Running Late Indicator**:
```
🔴 RUNNING LATE
Current call: 20 min overtime
Next patient (9:35 John Smith) notified of delay

[Send Update] [End Current Call]
```

### Video Call Interface Requirements

**Before Call**:
```
┌─────────────────────────────────────┐
│ Video Consultation - 9:00 AM         │
│ Patient: Jane Doe                    │
├─────────────────────────────────────┤
│ Call starts in: 5 minutes           │
│                                       │
│ [Join Waiting Room]                 │
└─────────────────────────────────────┘
```

**Reconnection Prompt** (if brief disconnection):
```
⚠️ Call Disconnected

Connection lost after 1 minute.
This may be a technical issue.

[Rejoin Call] (4:30 remaining)
[Report Issue] [Reschedule]
```

**Call Completion** (if incomplete):
```
Call ended after 3 minutes

Was this:
○ Technical issue - free reschedule
○ Complete consultation
○ Patient emergency
○ Other: ___________

[Submit]
```

---

## Database Changes

### Migration Script

```javascript
// 1. Add new fields to doctors collection
db.doctors.updateMany({}, {
    $set: {
        appointmentMode: "IN_PERSON_ONLY",
        acceptsVideoConsultations: false,
        schedulingConfig: {
            // Basic scheduling
            strictNoOverlapping: false,
            treatmentDurationMinutes: 30,
            cooldownMinutes: 5,
            allowWeekendBooking: true,
            blockedDates: [],
            
            // No-show detection  
            noShowGracePeriodMinutes: 15,
            
            // Call duration tracking
            minimumCallDurationMinutes: 5,
            reconnectionWindowMinutes: 5,
            
            // Auto-availability
            autoReleaseNoShowSlots: true
        }
    }
});

// 2. Add consultation type and tracking to appointments
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

// 3. Create performance indexes
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

### New Repository Methods

#### AppointmentRepository

```java
// Check for time overlap
boolean existsByDoctorIdAndTimeOverlap(
    String doctorId,
    Date startTime,
    Date endTime,
    AppointmentStatus status
);

// Check overlap excluding specific appointment (for rescheduling)
boolean existsByDoctorIdAndTimeOverlapExcluding(
    String doctorId,
    Date startTime,
    Date endTime,
    String excludeAppointmentId,
    AppointmentStatus status
);

// Find scheduled video calls before cutoff (for no-show detection)
List<AppointmentEntity> findScheduledVideoCallsBefore(Date cutoffTime);

// Find in-progress calls (for overtime monitoring)
List<AppointmentEntity> findByCallStatus(AppointmentCallStatus status);

// Count by consultation type
Long countByDoctorIdAndConsultationType(
    String doctorId,
    ConsultationType type
);
```

### Query Implementations

#### Time Overlap Check
```java
@Query("{ 'doctorId': ?0, " +
       "'appointmentDateTime': { $lt: ?2 }, " +
       "'$expr': { " +
       "  $gt: [ " +
       "    { $add: ['$appointmentDateTime', { $multiply: ['$estimatedDurationMinutes', 60000] }] }, " +
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
```

**Logic**: Checks if existing appointment's time range overlaps with requested range.

---

## Integration Points

### Reports Integration

**Enhancement**: Add consultation type breakdown

```java
// In DoctorReportsImpl
Map<String, Object> variables = new HashMap<>();

long inPersonCount = appointments.stream()
    .filter(a -> a.getConsultationType() == IN_PERSON_VISIT)
    .count();
    
long videoCallCount = appointments.stream()
    .filter(a -> a.getConsultationType() == VIDEO_CALL)
    .count();

variables.put("inPersonCount", inPersonCount);
variables.put("videoCallCount", videoCallCount);
variables.put("noShowCount", appointments.stream()
    .filter(a -> a.getStatus() == NO_SHOW)
    .count());
```

**Template Update** (doctor-report-template.html):
```html
<div>
    <h3>Consultation Type Breakdown</h3>
    <p>In-Person Visits: <span th:text="${inPersonCount}"></span></p>
    <p>Video Consultations: <span th:text="${videoCallCount}"></span></p>
    <p>No-Shows: <span th:text="${noShowCount}"></span></p>
</div>
```

### Statistics Integration

**Enhancement**: Track video vs in-person metrics

```java
// In DoctorStatisticsServiceImpl
dto.setTotalVideoCallAppointments(
    statisticsRepository.countByConsultationType(VIDEO_CALL)
);
dto.setTotalInPersonAppointments(
    statisticsRepository.countByConsultationType(IN_PERSON_VISIT)
);
dto.setNoShowRate(
    calculateNoShowRate(doctorId, dateRange)
);
dto.setAverageCallDuration(
    calculateAverageCallDuration(doctorId, dateRange)
);
```

### Notification Integration

**New Notification Types**:

Add to `NotificationType` enum:
```java
VIDEO_CALL_REMINDER,    // 10 min before call
PATIENT_WAITING,        // Patient joined waiting room
RUNNING_OVER_TIME,      // Call exceeding scheduled duration
RECONNECTION_OFFER,     // Brief disconnection recovery
NO_SHOW_DETECTED        // Automatic no-show detection
```

**Implementation**:
```java
// 10 minutes before video call
@Scheduled(fixedRate = 60000)
public void sendVideoCallReminders() {
    Date tenMinutesFromNow = addMinutes(now(), 10);
    
    List<Appointment> upcoming = appointmentRepository
        .findUpcomingVideoCallsAt(tenMinutesFromNow);
    
    for (Appointment appt : upcoming) {
        sendNotification(appt.getDoctorId(), VIDEO_CALL_REMINDER,
            "Video call with " + appt.getPatientName() + " in 10 minutes");
    }
}
```

### WebSocket Integration

Continue using existing `/topic/appointments/{doctorId}` channel.

**Enhanced Payload**:
```java
WebsocketResponseDTO.builder()
    .type(WebSocketResponseType.APPOINTMENT)
    .payload(appointmentDTO) // Now includes consultationType, callStatus
    .build();
```

**New Event Types**:
```java
// When slot becomes available due to no-show
WebsocketResponseDTO.builder()
    .type(WebSocketResponseType.SLOT_AVAILABLE)
    .payload(slotDTO)
    .build();

// When call status changes
WebsocketResponseDTO.builder()
    .type(WebSocketResponseType.CALL_STATUS_UPDATE)
    .payload(callStatusDTO)
    .build();
```

---

## Testing Requirements

### Unit Tests

1. **SlotGenerationService**
   - Generate slots with various configurations
   - Boundary enforcement (last slot doesn't overflow)
   - Weekend/blocked date filtering
   - Different treatment durations and cooldowns

2. **Booking Validation**
   - Consultation type compatibility
   - Strict mode enabled/disabled
   - Emergency bypass
   - Configuration preservation

3. **Call Tracking**
   - Brief disconnection detection (< 2 min)
   - Incomplete consultation (2-5 min)
   - No-show detection after grace period
   - Reconnection window logic

### Integration Tests

1. **Concurrent Booking Prevention**
   - Simulate 2 users booking same slot
   - Verify only one succeeds
   - Check transaction isolation

2. **Configuration Change Impact**
   - Book appointments with 30-min duration
   - Change to 45-min duration
   - Verify existing keep 30 min, new get 45 min

3. **No-Show Auto-Detection**
   - Create appointment in past
   - Run scheduled job
   - Verify status = NO_SHOW
   - Verify slot released if configured

### Manual Test Scenarios

1. **Doctor enables video consultations**
2. **Patient books video appointment**
3. **Verify slot unavailable for others**
4. **Patient joins and leaves after 1 min**
5. **Verify reconnection offered**
6. **Wait 5 min, verify slot released**
7. **Generate report, verify consultation type stats**
8. **Call runs 20 min over, verify delay notifications**

---

## Configuration Reference

### Doctor Scheduling Config Defaults

| Setting | Default | Min | Max | Description |
|---------|---------|-----|-----|-------------|
| strictNoOverlapping | false | - | - | Required true for video mode |
| treatmentDurationMinutes | 30 | 5 | 480 | Per-appointment duration |
| cooldownMinutes | 5 | 0 | 60 | Break time between appointments |
| allowWeekendBooking | true | - | - | Enable Saturday/Sunday slots |
| noShowGracePeriodMinutes | 15 | 5 | 30 | Time before marking no-show |
| minimumCallDurationMinutes | 5 | 2 | 10 | Valid consultation threshold |
| reconnectionWindowMinutes | 5 | 1 | 10 | Brief disconnection recovery time |
| autoReleaseNoShowSlots | true | - | - | Free up slots automatically |

---

**End of Implementation Plan**

For system architecture and implementation phases, refer to separate documentation files.
