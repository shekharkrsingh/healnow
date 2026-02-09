# System Architecture: Video Consultation & Appointment Management

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [System Components](#system-components)
3. [Service Layer Architecture](#service-layer-architecture)
4. [Data Layer](#data-layer)
5. [API Layer](#api-layer)
6. [Real-Time Communication](#real-time-communication)
7. [Integration Architecture](#integration-architecture)
8. [Security Architecture](#security-architecture)
9. [Technology Stack](#technology-stack)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
├─────────────────────┬───────────────────────────────────────┤
│  Doctor Dashboard   │     Patient Booking Portal            │
│  - Settings UI      │     - Slot Selection                  │
│  - Schedule Mgmt    │     - Consultation Type               │
│  - Call Interface   │     - Video Call UI                   │
└─────────────────────┴───────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                       │
├──────────────────────┬──────────────────────────────────────┤
│   REST Controllers   │      WebSocket Handlers              │
│   - Doctor APIs      │      - Call Events                   │
│   - Appointment APIs │      - Slot Updates                  │
│   - Slot APIs        │      - Notifications                 │
└──────────────────────┴──────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
├──────────────┬──────────────┬──────────────┬────────────────┤
│ Appointment  │ Slot         │ Call         │ Notification   │
│ Service      │ Generation   │ Tracking     │ Service        │
│              │ Service      │ Service      │                │
│ Doctor       │ Validation   │ No-Show      │ Email/SMS      │
│ Config Svc   │ Service      │ Detection    │ Service        │
└──────────────┴──────────────┴──────────────┴────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Data Access Layer                         │
├──────────────────────┬──────────────────────────────────────┤
│   Repositories       │         Cache Layer                  │
│   - Appointment      │         - Slot Cache                 │
│   - Doctor           │         - Config Cache               │
│   - Statistics       │                                      │
└──────────────────────┴──────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│              MongoDB (Appointments, Doctors)                 │
└─────────────────────────────────────────────────────────────┘
```

### Architecture Principles

1. **Separation of Concerns**: Clear boundaries between presentation, business logic, and data
2. **Single Responsibility**: Each service handles one specific domain
3. **Dependency Injection**: Loose coupling through Spring IoC
4. **Asynchronous Processing**: Non-blocking operations for notifications and analytics
5. **Event-Driven**: WebSocket-based real-time updates
6. **Stateless Services**: Enable horizontal scaling

---

## System Components

### Core Components

#### 1. Appointment Management Component

**Responsibilities**:
- Booking, updating, canceling appointments
- Consultation type validation
- Slot conflict detection
- Emergency appointment handling

**Key Classes**:
- `AppointmentServiceImpl` - Core business logic
- `AppointmentController` - REST endpoints
- `AppointmentRepository` - Data access
- `AppointmentEntity` - Domain model

**Interactions**:
```
AppointmentController
    ↓
AppointmentService
    ↓ validates against
DoctorService (get config)
    ↓ checks availability
SlotGenerationService
    ↓ persists
AppointmentRepository
    ↓ broadcasts
NotificationService
```

#### 2. Slot Generation Component

**Responsibilities**:
- Generate available time slots
- Respect doctor's schedule and config
- Mark slots as available/booked
- Handle boundary enforcement

**Key Classes**:
- `SlotGenerationService` - Algorithm implementation
- `AppointmentSlotController` - Public API
- `AppointmentSlotDTO` - Slot representation

**Algorithm Flow**:
```
1. Fetch doctor configuration
   ├─ availableDays (MONDAY-FRIDAY)
   ├─ availableTimeSlots (9:00 AM - 5:00 PM)
   └─ schedulingConfig (duration, cooldown)

2. For requested date:
   ├─ Check if day is in availableDays
   ├─ Check if date not in blockedDates
   └─ Check if not weekend (if disabled)

3. For each time slot range:
   ├─ Parse start and end times
   ├─ Generate slots with duration + cooldown
   ├─ Enforce boundary: start + duration ≤ end
   └─ Add to slot list

4. Fetch existing appointments for date:
   ├─ Query by doctorId and date range
   └─ Use estimatedDurationMinutes

5. Mark slot availability:
   ├─ For each slot, check overlap with appointments
   └─ Set available = true/false

6. Return slot list
```

#### 3. Call Tracking Component

**Responsibilities**:
- Track join/leave events
- Monitor call duration
- Detect no-shows
- Handle brief disconnections
- Manage reconnection window

**Key Classes**:
- `CallTrackingService` - Event handling
- `VideoCallWebSocketHandler` - WebSocket events
- `CallMonitoringScheduler` - Background jobs

**State Machine**:
```
SCHEDULED
    ↓ (patient joins)
PATIENT_WAITING
    ↓ (doctor joins)
IN_PROGRESS
    ↓ (either leaves)
    ├─ duration < 2 min → BRIEF_DISCONNECTION → reconnect offer
    ├─ duration 2-5 min → INCOMPLETE_CONSULTATION → doctor prompt
    └─ duration > 5 min → COMPLETED
```

#### 4. No-Show Detection Component

**Responsibilities**:
- Automatic no-show detection
- Grace period management
- Slot auto-release
- Notification triggers

**Key Classes**:
- `NoShowDetectionScheduler` - Scheduled job (every 5 min)
- `SlotManagementService` - Slot release logic

**Detection Logic**:
```
Every 5 minutes:
1. Calculate cutoff: now - gracePeriodMinutes
2. Find scheduled video calls before cutoff
3. For each appointment:
   IF neither joined:
      → Status = NO_SHOW
      → CallStatus = PATIENT_NO_SHOW
      → Release slot (if configured)
   ELSE IF patient joined, doctor didn't:
      → CallStatus = DOCTOR_NO_SHOW
      → Offer reschedule
   ELSE IF doctor joined, patient didn't:
      → CallStatus = PATIENT_NO_SHOW
      → Release slot
```

#### 5. Doctor Configuration Component

**Responsibilities**:
- Manage appointment mode
- Update scheduling config
- Validate configuration changes
- Preserve existing appointments

**Key Classes**:
- `DoctorConfigurationService` - Configuration management
- `DoctorController` - Configuration endpoints

**Validation**:
```
On configuration update:
1. IF appointmentMode = VIDEO_CALL_ONLY or BOTH:
   ├─ REQUIRE strictNoOverlapping = true
   ├─ REQUIRE treatmentDurationMinutes set
   └─ VALIDATE cooldownMinutes range

2. Count existing appointments:
   ├─ Query appointments > now
   └─ Display warning if count > 0

3. Update doctor.schedulingConfig

4. Broadcast config change event
```

---

## Service Layer Architecture

### Service Hierarchy

```
┌─────────────────────────────────────────┐
│         IAppointmentService             │
│  - bookAppointment()                    │
│  - selfBookAppointment()                │
│  - updateAppointmentDetails()           │
│  - cancelAppointment()                  │
└─────────────────────────────────────────┘
                 ↓ implements
┌─────────────────────────────────────────┐
│      AppointmentServiceImpl             │
│  + validation logic                     │
│  + conflict detection                   │
│  + consultation type check              │
│  + emergency bypass                     │
└─────────────────────────────────────────┘
         ↓ depends on
┌──────────────────┬──────────────────────┐
│ ISlotGeneration  │ IDoctorService       │
│ Service          │                      │
│ - generateSlots  │ - getDoctorById()    │
│ - isAvailable    │ - getConfig()        │
└──────────────────┴──────────────────────┘
```

### Service Interactions

#### Booking Flow

```
[Patient Request] → BookAppointment
                        ↓
           1. Validate Input (contact, name, date)
                        ↓
           2. Fetch Doctor → DoctorService.getDoctorById()
                        ↓
           3. Check Mode Compatibility
              IF consultationType = VIDEO_CALL:
                 REQUIRE doctor.acceptsVideoConsultations
                 REQUIRE mode = VIDEO_CALL_ONLY or BOTH
                        ↓
           4. Set Estimated Duration
              appointment.estimatedDurationMinutes = 
                doctor.schedulingConfig.treatmentDurationMinutes
                        ↓
           5. Check Strict No-Overlap (if enabled)
              IF schedulingConfig.strictNoOverlapping:
                 ├─ Calculate slot end time
                 ├─ Query overlapping appointments
                 └─ Throw error if conflict
                        ↓
           6. Check Weekend/Blocked Dates
              IF !allowWeekendBooking && isWeekend:
                 Throw error
              IF date in blockedDates:
                 Throw error
                        ↓
           7. Save Appointment → AppointmentRepository
                        ↓
           8. Broadcast Update → WebSocket
                        ↓
           9. Send Notifications → NotificationService
                        ↓
          10. Return AppointmentDTO
```

#### Slot Generation Flow

```
[Request: doctorId, date] → GenerateSlots
                                ↓
           1. Fetch Doctor Config
              ├─ availableDays
              ├─ availableTimeSlots
              └─ schedulingConfig
                                ↓
           2. Validate Date
              IF date not in availableDays:
                 Return empty list
              IF date in blockedDates:
                 Return empty list
                                ↓
           3. Generate Slot Times
              FOR each timeSlot in availableTimeSlots:
                 startTime = parseTime(slot.startTime)
                 endTime = parseTime(slot.endTime)
                 slotDuration = treatment + cooldown
                 
                 WHILE start + treatment ≤ end:
                    slots.add(start, start + treatment)
                    start += slotDuration
                                ↓
           4. Fetch Appointments for Date
              appointments = repository.findByDoctorAndDate()
                                ↓
           5. Mark Availability
              FOR each slot:
                 hasConflict = checkOverlap(slot, appointments)
                 slot.available = !hasConflict
                                ↓
           6. Filter Past Slots (same-day booking)
              IF date = today:
                 Remove slots where start < now
                                ↓
           7. Return Slot List
```

#### Call Monitoring Flow

```
[Scheduled Job: Every 1 minute] → MonitorActiveCalls
                                       ↓
           1. Find In-Progress Calls
              calls = repository.findByCallStatus(IN_PROGRESS)
                                       ↓
           2. For Each Call:
              elapsed = minutesSince(call.actualCallStartTime)
              scheduled = call.estimatedDurationMinutes
                                       ↓
           3. Check Time Warnings
              IF elapsed = scheduled - 2:
                 Send "2 minutes remaining" to doctor
                                       ↓
           4. Check Overtime
              IF elapsed > scheduled:
                 overtime = elapsed - scheduled
                 Send "Running X min over" to doctor
                 
                 Find next appointment
                 IF next exists:
                    Notify next patient of delay
                                       ↓
           5. Update Statistics
              Track overtime metrics for reports
```

---

## Data Layer

### Database Schema

#### Appointments Collection

```javascript
{
  _id: ObjectId,
  appointmentId: String (unique),
  doctorId: String (indexed),
  patientName: String,
  contact: String,
  appointmentDateTime: Date (indexed),
  
  // Existing fields
  status: "ACCEPTED" | "BOOKED" | "CANCELLED" | "NO_SHOW",
  appointmentType: "IN_PERSON" | "ONLINE",
  treated: Boolean,
  availableAtClinic: Boolean,
  paymentStatus: Boolean,
  isEmergency: Boolean,
  
  // NEW fields
  consultationType: "IN_PERSON_VISIT" | "VIDEO_CALL",
  estimatedDurationMinutes: Number,
  
  // Call tracking
  patientJoinedAt: Date,
  patientLeftAt: Date,
  doctorJoinedAt: Date,
  doctorLeftAt: Date,
  actualCallStartTime: Date,
  actualCallEndTime: Date,
  actualCallDurationSeconds: Number,
  callStatus: String, // SCHEDULED, IN_PROGRESS, COMPLETED, etc.
  disconnectReason: String,
  videoCallLink: String,
  
  createdAt: Date,
  updatedAt: Date
}
```

**Indexes**:
```javascript
// Existing
{ doctorId: 1, appointmentDateTime: 1 }

// NEW - for overlap detection
{ doctorId: 1, appointmentDateTime: 1, status: 1 }

// NEW - for call monitoring
{ doctorId: 1, callStatus: 1, appointmentDateTime: 1 }

// NEW - for no-show detection
{ consultationType: 1, appointmentDateTime: 1, callStatus: 1 }
```

#### Doctors Collection

```javascript
{
  _id: ObjectId,
  doctorId: String (unique),
  firstName: String,
  lastName: String,
  specialization: String,
  
  // Existing schedule
  availableDays: ["MONDAY", "TUESDAY", ...],
  availableTimeSlots: [
    { startTime: "9:00 AM", endTime: "12:00 PM" },
    { startTime: "2:00 PM", endTime: "5:00 PM" }
  ],
  
  // NEW fields
  appointmentMode: "IN_PERSON_ONLY" | "VIDEO_CALL_ONLY" | "BOTH",
  acceptsVideoConsultations: Boolean,
  
  schedulingConfig: {
    strictNoOverlapping: Boolean,
    treatmentDurationMinutes: Number,
    cooldownMinutes: Number,
    allowWeekendBooking: Boolean,
    blockedDates: [Date],
    
    noShowGracePeriodMinutes: Number,
    minimumCallDurationMinutes: Number,
    reconnectionWindowMinutes: Number,
    autoReleaseNoShowSlots: Boolean
  },
  
  createdAt: Date,
  updatedAt: Date
}
```

### Repository Patterns

#### Custom Query Methods

```java
public interface AppointmentRepository extends MongoRepository<AppointmentEntity, String> {
    
    // Time overlap detection
    @Query("{ 'doctorId': ?0, " +
           "'appointmentDateTime': { $lt: ?2 }, " +
           "'$expr': { $gt: [{ $add: ['$appointmentDateTime', " +
           "{ $multiply: ['$estimatedDurationMinutes', 60000] }] }, ?1 ] }, " +
           "'status': ?3 }")
    boolean existsByDoctorIdAndTimeOverlap(
        String doctorId, Date startTime, Date endTime, AppointmentStatus status
    );
    
    // No-show detection
    List<AppointmentEntity> findByConsultationTypeAndAppointmentDateTimeBeforeAndCallStatus(
        ConsultationType type, Date beforeTime, AppointmentCallStatus status
    );
    
    // Call monitoring
    List<AppointmentEntity> findByCallStatus(AppointmentCallStatus status);
    
    // Statistics
    Long countByDoctorIdAndConsultationTypeAndAppointmentDateTimeBetween(
        String doctorId, ConsultationType type, Date start, Date end
    );
}
```

### Caching Strategy

```java
@Cacheable(value = "doctorConfig", key = "#doctorId")
public DoctorEntity getDoctorById(String doctorId) {
    // Cache doctor configuration for 5 minutes
}

@Cacheable(value = "availableSlots", key = "#doctorId + '-' + #date")
public List<AppointmentSlotDTO> getAvailableSlots(String doctorId, LocalDate date) {
    // Cache generated slots for 2 minutes
}

@CacheEvict(value = {"doctorConfig", "availableSlots"}, allEntries = true)
public void updateSchedulingConfig(String doctorId, DoctorSchedulingConfig config) {
    // Invalidate cache on config update
}
```

---

## API Layer

### REST API Architecture

#### Controller Structure

```
/api/v1/
├── doctors/
│   ├── {doctorId}/appointment-mode [PUT] - Update mode
│   ├── {doctorId}/scheduling-config [PUT, GET] - Manage config
│   └── {doctorId} [GET] - Get doctor profile
│
├── appointments/
│   ├── /book [POST] - Book appointment (doctor/collaborator)
│   ├── /self-book [POST] - Patient self-booking
│   ├── /{appointmentId} [GET, PUT, DELETE]
│   ├── /by-doctor [GET] - List by date
│   ├── /search [POST] - Advanced search
│   └── /public/doctors/{doctorId}/slots [GET] - Available slots
│
└── video-calls/
    ├── /{appointmentId}/join [POST] - Join call
    ├── /{appointmentId}/leave [POST] - Leave call
    └── /{appointmentId}/status [GET] - Call status
```

#### Security Annotations

```java
// Only doctor owner can update mode
@PreAuthorize("hasRole('DOCTOR') and @roleUtils.isOwner(#doctorId)")
public DoctorDTO updateAppointmentMode(String doctorId, ...) { }

// Doctor, admin, collaborator can book
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'COLLABORATOR')")
public AppointmentDTO bookAppointment(...) { }

// Public endpoint for slot viewing
@GetMapping("/public/doctors/{doctorId}/slots")
public List<AppointmentSlotDTO> getAvailableSlots(...) { }
```

---

## Real-Time Communication

### WebSocket Architecture

#### Topics

```
/topic/appointments/{doctorId}
  - Appointment created
  - Appointment updated
  - Appointment cancelled
  - Slot availability changed

/topic/video-calls/{appointmentId}
  - User joined
  - User left
  - Call status changed
  
/topic/notifications/{userId}
  - General notifications
  - Call reminders
  - Running late alerts
```

#### Message Format

```typescript
interface WebSocketMessage {
  type: 'APPOINTMENT' | 'CALL_STATUS' | 'SLOT_AVAILABLE' | 'NOTIFICATION';
  timestamp: string;
  payload: any;
}

// Example: Appointment update
{
  type: 'APPOINTMENT',
  timestamp: '2026-02-10T09:35:00Z',
  payload: {
    appointmentId: '...',
    status: 'ACCEPTED',
    consultationType: 'VIDEO_CALL',
    callStatus: 'PATIENT_WAITING'
  }
}
```

#### Event Flow

```
Patient joins video call
         ↓
WebSocket: SEND /app/video-call/join
         ↓
CallTrackingService.onUserJoined()
         ↓
Update appointment.patientJoinedAt
Update callStatus = PATIENT_WAITING
         ↓
Broadcast to /topic/appointments/{doctorId}
         ↓
Doctor dashboard updates in real-time
```

---

## Integration Architecture

### External Integrations

#### Email Service Integration

```
AppointmentService
    ↓ (async)
EmailService.sendAppointmentConfirmation()
    ↓
TemplateEngine (Thymeleaf)
    ↓ renders
email-template.html
    ↓
SMTP Server (configured in application.yml)
```

#### SMS/Notification Service

```
NoShowDetectionScheduler
    ↓ detects no-show
NotificationService.sendNoShowAlert()
    ↓ (async)
SMSService / PushNotificationService
```

#### Video Call Platform (Future)

```
Appointment created with VIDEO_CALL
    ↓
VideoCallIntegrationService.generateMeetingLink()
    ↓ calls
Zoom/Google Meet/WebRTC API
    ↓ returns
videoCallLink
    ↓
Store in appointment.videoCallLink
    ↓
Include in confirmation email
```

### Internal Integrations

#### Reports Integration

```
DoctorReportsService.generateReport()
    ↓
AppointmentService.getAppointmentsByDateRange()
    ↓ filters/groups by
consultationType, callStatus, treated
    ↓
TemplateEngine renders PDF
    ↓ includes
Video vs In-Person breakdown
No-show statistics
Average call duration
```

#### Statistics Integration

```
DoctorStatisticsService.fetchStatistics()
    ↓ aggregates
AppointmentRepository.countByConsultationType()
AppointmentRepository.averageCallDuration()
AppointmentRepository.noShowRate()
    ↓ returns
DoctorStatisticsDTO with video metrics
```

---

## Security Architecture

### Authentication Flow

```
Client Request
    ↓
JWT Token in Authorization header
    ↓
JwtAuthenticationFilter
    ↓ validates
JwtTokenProvider.validateToken()
    ↓ extracts
username, roles
    ↓
SecurityContext.setAuthentication()
    ↓
@PreAuthorize checks role
    ↓
Controller method executes
```

### Authorization Matrix

| Operation | DOCTOR (Owner) | COLLABORATOR | ADMIN | PATIENT |
|-----------|----------------|--------------|-------|---------|
| Update appointment mode | ✅ | ❌ | ❌ | ❌ |
| View scheduling config | ✅ | ✅ (read-only) | ✅ | ❌ |
| Book appointment | ✅ | ✅ | ✅ | ❌ |
| Self-book appointment | ❌ | ❌ | ❌ | ✅ (public) |
| View available slots | ✅ | ✅ | ✅ | ✅ (public) |
| Update appointment | ✅ | ✅ | ✅ | ❌ |
| Generate reports | ✅ | ✅ | ✅ | ❌ |
| View statistics | ✅ | ✅ | ✅ | ❌ |

### Data Protection

```java
// Sensitive data encryption
@Encrypted
private String contact;

// Audit logging
@Audited
public AppointmentDTO bookAppointment(...) {
    // Logs user, timestamp, action
}

// Rate limiting
@RateLimited(maxRequests = 100, perSeconds = 60)
public List<AppointmentSlotDTO> getAvailableSlots(...) { }
```

---

## Technology Stack

### Backend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | Spring Boot 2.x/3.x | Application framework |
| Language | Java 11+ | Primary language |
| Database | MongoDB | Document storage |
| Caching | Spring Cache + Redis | Performance optimization |
| WebSocket | STOMP over WebSocket | Real-time communication |
| Security | Spring Security + JWT | Authentication/Authorization |
| Validation | Jakarta Validation | Input validation |
| Scheduling | Spring @Scheduled | Background jobs |
| Async | Spring @Async | Non-blocking operations |
| Email | JavaMailSender | Email notifications |
| PDF | iText / Flying Saucer | Report generation |
| Template | Thymeleaf | Email/PDF templates |

### Frontend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | React / Vue / Angular | UI framework |
| WebSocket Client | SockJS + STOMP | Real-time updates |
| State Management | Redux / Vuex / NgRx | Application state |
| HTTP Client | Axios | API communication |
| Date/Time | Moment.js / Day.js | Date handling |
| UI Components | Material-UI / Ant Design | Component library |

### DevOps

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Build | Maven / Gradle | Dependency management |
| Testing | JUnit + Mockito | Unit/integration tests |
| API Docs | Swagger / OpenAPI | API documentation |
| Monitoring | Spring Actuator | Health checks |
| Logging | SLF4J + Logback | Application logging |

---

## Component Interaction Diagram

### Complete Booking Flow

```
┌─────────┐                                    ┌──────────┐
│ Patient │                                    │  Doctor  │
│   UI    │                                    │  Dashboard│
└────┬────┘                                    └────┬─────┘
     │                                              │
     │ 1. Select consultation type                 │
     │    & request available slots                │
     │───────────────────────────────────────────► │
     │                                              │
     │          GET /slots?date=2026-02-10         │
     │ ◄───────────────────────────────────────────┤
     │                                              │
     ▼                                              │
┌─────────────────┐                                │
│SlotGeneration   │                                │
│Service          │                                │
│ - Load config   │                                │
│ - Generate slots│                                │
│ - Check existing│                                │
└────┬────────────┘                                │
     │                                              │
     │ Return available slots                      │
     │───────────────────────────────────────────► │
     │                                              │
     │ 2. Patient selects slot & books             │
     │───────────────────────────────────────────► │
     │                                              │
     │         POST /appointments/self-book        │
     │ ◄───────────────────────────────────────────┤
     │                                              │
     ▼                                              │
┌─────────────────┐                                │
│Appointment      │                                │
│Service          │                                │
│                 │                                │
│ ├─ Validate     │                                │
│ ├─ Check mode   │                                │
│ ├─ Check conflict                                │
│ ├─ Save         │                                │
│ └─ Notify       │                                │
└────┬────────────┘                                │
     │                                              │
     │ 3. Broadcast via WebSocket                  │
     │─────────────────────────────────────────────┤
     │                                              │
     │ 4. Dashboard updates in real-time           │
     │                                             ▼
     │                                    ┌─────────────┐
     │                                    │ WebSocket   │
     │                                    │ /topic/...  │
     │                                    └─────────────┘
     │                                              │
     │ 5. Send confirmation email (async)          │
     ▼                                              │
┌─────────────────┐                                │
│Email Service    │                                │
│ - Render template                                │
│ - Send via SMTP │                                │
└─────────────────┘                                │
```

---

**End of System Architecture Document**

For implementation details and phases, refer to separate documentation files.
