# Entity Affiliation Architecture

> **Version:** 1.0  
> **Status:** Draft — Architecture Specification  
> **Last Updated:** 2026-06-30  
> **Applies To:** healnow (Spring Boot API) · fhpotionV2 (Expo/React Native)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Terminology](#2-terminology)
3. [Goals & Non-Goals](#3-goals--non-goals)
4. [Architecture Principles](#4-architecture-principles)
5. [High-Level Architecture](#5-high-level-architecture)
6. [Domain Model](#6-domain-model)
7. [Affiliation Lifecycle](#7-affiliation-lifecycle)
8. [Role-Based Access Control](#8-role-based-access-control)
9. [Context Model](#9-context-model)
10. [Data Sharing Policy](#10-data-sharing-policy)
11. [Availability & Scheduling](#11-availability--scheduling)
12. [Staff Assignment Model](#12-staff-assignment-model)
13. [Service Layer Design](#13-service-layer-design)
14. [API Specification](#14-api-specification)
15. [Frontend Architecture](#15-frontend-architecture)
16. [Notification Architecture](#16-notification-architecture)
17. [Security & Audit](#17-security--audit)
18. [Database Design](#18-database-design)
19. [Event Flows](#19-event-flows)
20. [Migration Strategy](#20-migration-strategy)
21. [Implementation Phases](#21-implementation-phases)
22. [Extension Points](#22-extension-points)
23. [Open Decisions](#23-open-decisions)
24. [Appendix](#24-appendix)

---

## 1. Executive Summary

This document defines the architecture for **Entity Affiliation** — a governed, many-to-many relationship between **Healthcare Entities** (hospitals, clinics, diagnostic centers) and **Doctors**, with admin approval, scoped availability, dual-context staff assignment, separate entity dashboards, and context-aware notifications.

### Problem Statement

Today, clinic information is stored as flat fields on `DoctorEntity` (`clinicName`, `clinicAddress`, etc.). There is no first-class organizational entity, no formal affiliation workflow, and no way for a doctor to work at multiple hospitals with distinct schedules and scoped staff access.

### Solution Overview

Introduce three new core aggregates:

| Aggregate | Purpose |
|-----------|---------|
| `HealthcareEntity` | First-class hospital/clinic organization |
| `EntityAffiliation` | Governed doctor ↔ entity join with lifecycle, availability, and data policy |
| `AffiliationStaffAssignment` | Scoped staff access (doctor-context vs entity-context) |

The design extends — not replaces — existing patterns:

- `DoctorAssociation` → template for affiliation staff assignments
- `InvitationEntity` → template for affiliation invite flows
- `X-Active-Doctor-Id` → extended with `X-Active-Entity-Id` and `X-Active-Affiliation-Id`
- `NotificationService` → extended with context envelope and new recipient types

---

## 2. Terminology

| Term | Definition |
|------|------------|
| **Healthcare Entity** | An organization (hospital, clinic, diagnostic lab) registered on the platform |
| **Affiliation** | A governed link between one doctor and one entity, with its own lifecycle and scoped data |
| **Entity Member** | A user (admin, supervisor, collaborator) belonging to an entity |
| **Doctor-Context Staff** | Collaborator assigned by the doctor; operates with doctor's permissions |
| **Entity-Context Staff** | Collaborator assigned by the entity; scoped to one affiliation only |
| **Peer Acceptance** | The non-initiating party (doctor or entity) accepting an affiliation request |
| **Admin Approval** | Platform admin gate before affiliation becomes `ACTIVE` |
| **Active Context** | The entity, doctor, or affiliation scope a user is currently operating in |
| **Data Sharing Policy** | Explicit contract defining which fields each party exposes |

### Distinction from Existing Concepts

| Existing | New | Relationship |
|----------|-----|--------------|
| `DoctorEntity.clinicName` (flat field) | `HealthcareEntity` | Clinic fields become legacy; entities are first-class |
| `DoctorAssociation` (collaborator ↔ doctor) | `AffiliationStaffAssignment` | Staff can be scoped to doctor globally or to one affiliation |
| `DoctorEntity.availability` (global) | `EntityAffiliation.entityAvailability` | Global schedule remains; entity schedule is per-affiliation |
| `InvitationEntity` (collaborator onboarding) | `AffiliationRequestEntity` | Separate invite flow for affiliation |

---

## 3. Goals & Non-Goals

### Goals

- Enable **bidirectional affiliation initiation** (doctor → entity or entity → doctor)
- Require **mutual acceptance** plus **platform admin approval**
- Support **many-to-many** relationships (doctor ↔ multiple entities, entity ↔ multiple doctors)
- Provide **affiliation-scoped availability** with conflict detection
- Assign **staff with two distinct scopes** (doctor-context vs entity-context)
- Deliver a **separate entity dashboard** with entity-specific roles
- Route **notifications by context** (entity, affiliation, role)
- Maintain **audit trail** for compliance
- Design for **future extension** (billing, departments, patient sharing)

### Non-Goals (v1)

- Cross-entity patient record sharing
- Revenue sharing / billing per affiliation
- Multi-tenant white-label entity portals
- External EHR/HL7 integration
- Automatic migration of all existing clinic fields (phased only)

---

## 4. Architecture Principles

| # | Principle | Implementation |
|---|-----------|----------------|
| P1 | **Mutual consent** | State machine enforces peer accept before admin approval |
| P2 | **Least privilege** | Permissions resolved at request time from role + assignment scope |
| P3 | **Context isolation** | All reads/writes scoped via `EffectiveContext` |
| P4 | **Explicit data sharing** | No implicit full profile access; policy-driven field exposure |
| P5 | **Single responsibility** | One service per aggregate; no god services |
| P6 | **Event-driven side effects** | Notifications, audit, email via domain events — not inline in controllers |
| P7 | **Backward compatibility** | Existing doctor/collaborator flows unchanged until opt-in migration |
| P8 | **Idempotent operations** | Accept/reject/approve safe to retry |
| P9 | **Extensibility via enums + policy objects** | New entity types, roles, permissions added without schema breaks |

---

## 5. High-Level Architecture

### 5.1 System Context

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Client Applications                               │
├─────────────────────┬──────────────────────┬─────────────────────────────┤
│  Doctor App         │  Entity Dashboard    │  Platform Admin Console     │
│  (fhpotionV2)       │  (fhpotionV2 / Web)  │  (existing AdminController) │
│  - Affiliations     │  - Roster            │  - Affiliation approval     │
│  - Entity schedule  │  - Schedule          │  - Entity verification      │
│  - Staff (doctor)   │  - Staff mgmt        │                             │
└─────────────────────┴──────────────────────┴─────────────────────────────┘
                                    │
                          HTTPS / WSS (JWT + Context Headers)
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         healnow API (Spring Boot 3.4)                     │
├──────────────────────────────────────────────────────────────────────────┤
│  Controllers Layer                                                        │
│  EntityController · AffiliationController · EntityMemberController        │
│  AffiliationAvailabilityController · AffiliationStaffController         │
│  ContextController · AdminAffiliationController                         │
├──────────────────────────────────────────────────────────────────────────┤
│  Security Layer                                                           │
│  JwtAuthenticationFilter · ContextResolutionFilter · @PreAuthorize        │
│  EffectiveContext · PermissionEvaluator                                   │
├──────────────────────────────────────────────────────────────────────────┤
│  Service Layer                                                            │
│  EntityService · AffiliationService · AffiliationStateMachine             │
│  AvailabilityConflictService · DataSharingService · StaffAssignmentService│
│  ContextService · EntityNotificationOrchestrator · AuditService           │
├──────────────────────────────────────────────────────────────────────────┤
│  Event Layer (Spring ApplicationEventPublisher)                           │
│  AffiliationRequestedEvent · AffiliationApprovedEvent · ...               │
├──────────────────────────────────────────────────────────────────────────┤
│  Data Layer (MongoDB)                                                     │
│  healthcare_entities · entity_affiliations · affiliation_staff            │
│  affiliation_audit_logs · entity_members                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Bounded Contexts

```
┌─────────────────┐     ┌─────────────────────┐     ┌──────────────────┐
│  Identity &     │     │  Entity Affiliation │     │  Appointments    │
│  Auth Context   │────▶│  Context (NEW)      │────▶│  Context         │
│  (existing)     │     │                     │     │  (extended)      │
└─────────────────┘     └─────────────────────┘     └──────────────────┘
         │                         │                          │
         │                         ▼                          │
         │               ┌─────────────────────┐              │
         └──────────────▶│  Notification       │◀─────────────┘
                         │  Context (extended) │
                         └─────────────────────┘
```

Each bounded context owns its aggregates. Cross-context communication uses domain events and well-defined DTOs — never direct repository access across contexts.

### 5.3 Package Structure (Backend)

```
com.heal.doctor
├── entity/                          # NEW module
│   ├── models/
│   │   ├── HealthcareEntity.java
│   │   ├── EntityMember.java
│   │   ├── EntityAffiliation.java
│   │   ├── AffiliationStaffAssignment.java
│   │   ├── DataSharingPolicy.java
│   │   ├── AffiliationAuditLog.java
│   │   └── enums/
│   │       ├── EntityType.java
│   │       ├── EntityStatus.java
│   │       ├── AffiliationStatus.java
│   │       ├── AffiliationInitiator.java
│   │       ├── EntityMemberRole.java
│   │       ├── StaffAssignmentScope.java
│   │       └── StaffAssigner.java
│   ├── repositories/
│   ├── services/
│   │   ├── IEntityService.java
│   │   ├── IAffiliationService.java
│   │   ├── IAffiliationStateMachine.java
│   │   ├── IAvailabilityConflictService.java
│   │   ├── IDataSharingService.java
│   │   ├── IStaffAssignmentService.java
│   │   ├── IContextService.java
│   │   └── impl/
│   ├── controllers/
│   ├── dto/
│   ├── events/
│   ├── listeners/
│   └── security/
│       ├── EffectiveContext.java
│       ├── ContextResolutionFilter.java
│       └── EntityPermissionEvaluator.java
├── models/                          # existing — unchanged
├── security/                        # extended
└── ...
```

Modular packaging allows future extraction to a separate microservice if scale demands it.

---

## 6. Domain Model

### 6.1 Entity Relationship Diagram

```
┌─────────────────────┐
│   HealthcareEntity  │
│─────────────────────│
│ entityId (PK)       │
│ name, type, status  │
│ registrationDetails │
│ settings            │
│ members[]           │──┐
└─────────────────────┘  │ 1:N (embedded or ref)
                           ▼
                    ┌──────────────┐
                    │ EntityMember │
                    │ userId, role │
                    │ permissions  │
                    └──────────────┘

┌─────────────────────┐         ┌─────────────────────┐
│   HealthcareEntity  │ 1     N │  EntityAffiliation  │ N     1 ┌─────────────┐
│                     │◄───────►│                     │◄───────►│ DoctorEntity│
└─────────────────────┘         │─────────────────────│         │  (existing) │
                                │ affiliationId (PK)  │         └─────────────┘
                                │ entityId, doctorId  │
                                │ status, initiatedBy │
                                │ entityAvailability  │
                                │ dataSharingPolicy   │
                                │ metadata (cached)   │
                                └─────────────────────┘
                                          │ 1
                                          │ N
                                          ▼
                                ┌─────────────────────────┐
                                │ AffiliationStaffAssignment│
                                │ userId, scope, assigner  │
                                │ permissions, active      │
                                └─────────────────────────┘
```

### 6.2 HealthcareEntity

```java
@Document(collection = "healthcare_entities")
public class HealthcareEntity {
    @Id private String id;
    @Indexed(unique = true) private String entityId;

    private String name;
    private EntityType type;           // HOSPITAL, CLINIC, DIAGNOSTIC, PHARMACY
    private EntityStatus status;       // DRAFT, PENDING_VERIFICATION, ACTIVE, SUSPENDED

    // Registration & compliance
    private String registrationNumber;
    private String licenseDocumentUrl;
    private VerificationStatus verificationStatus;

    // Contact & location
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phoneNumber;
    private String email;
    private String logoUrl;

    // Organization
    private List<String> departments;  // extensible: ["Cardiology", "Orthopedics"]
    private List<EntityMember> members;

    // Settings
    private EntitySettings settings;   // timezone, bookingRules, notificationPrefs

    private String createdByUserId;
    private Date createdAt;
    private Date updatedAt;
}
```

### 6.3 EntityMember (Embedded)

```java
public class EntityMember {
    private String userId;
    private EntityMemberRole role;     // ENTITY_ADMIN, SUPERVISOR, ENTITY_COLLABORATOR
    private List<String> permissions;  // granular override; defaults from role
    private List<String> departmentIds; // optional scope restriction
    private Date joinedAt;
    private boolean active;
}
```

### 6.4 EntityAffiliation

```java
@Document(collection = "entity_affiliations")
@CompoundIndexes({
    @CompoundIndex(name = "entity_doctor_unique", def = "{'entityId': 1, 'doctorId': 1}", unique = true),
    @CompoundIndex(name = "entity_status_idx", def = "{'entityId': 1, 'status': 1}"),
    @CompoundIndex(name = "doctor_status_idx", def = "{'doctorId': 1, 'status': 1}")
})
public class EntityAffiliation {
    @Id private String id;
    @Indexed(unique = true) private String affiliationId;

    private String entityId;
    private String doctorId;

    private AffiliationInitiator initiatedBy;  // DOCTOR, ENTITY
    private String initiatedByUserId;
    private AffiliationStatus status;

    // Affiliation-scoped schedule (reuses DayAvailability / TimeSlot)
    private List<DayAvailability> entityAvailability;

    // Explicit data contract
    private DataSharingPolicy dataSharingPolicy;

    // Cached display fields (updated on profile change via event)
    private String doctorName;
    private String doctorSpecialization;
    private String entityName;
    private String department;           // optional: which department doctor joins

    // Lifecycle timestamps
    private Date peerAcceptedAt;
    private String peerAcceptedByUserId;
    private Date adminApprovedAt;
    private String adminApprovedByUserId;
    private Date rejectedAt;
    private String rejectionReason;
    private Date suspendedAt;
    private Date terminatedAt;
    private String terminationReason;

    private Date createdAt;
    private Date updatedAt;
}
```

### 6.5 DataSharingPolicy

```java
public class DataSharingPolicy {
    private int version;

    // Doctor → Entity shared fields (field names from DoctorEntity)
    private Set<String> doctorSharedFields;
    // e.g. ["firstName", "lastName", "specialization", "profilePicture", "medicalLicense"]

    // Entity → Doctor shared fields
    private Set<String> entitySharedFields;
    // e.g. ["name", "address", "departments", "phoneNumber"]

    // Feature-level toggles (extensible)
    private Map<String, Boolean> featureFlags;
    // e.g. {"shareAppointmentStats": true, "sharePatientCount": false}

    private Date agreedAt;
    private String agreedByDoctorUserId;
    private String agreedByEntityUserId;
}
```

### 6.6 AffiliationStaffAssignment

```java
@Document(collection = "affiliation_staff")
@CompoundIndex(name = "affiliation_user_idx", def = "{'affiliationId': 1, 'userId': 1}", unique = true)
public class AffiliationStaffAssignment {
    @Id private String id;

    private String affiliationId;
    private String entityId;       // denormalized for query performance
    private String doctorId;       // denormalized
    private String userId;         // the staff member

    private StaffAssigner assignedBy;   // DOCTOR, ENTITY
    private String assignedByUserId;
    private StaffAssignmentScope scope; // DOCTOR_CONTEXT, ENTITY_CONTEXT

    private String role;             // RECEPTIONIST, NURSE, SCHEDULER, etc.
    private List<String> permissions;
    private boolean active;

    private Date assignedAt;
    private Date revokedAt;
}
```

### 6.7 AffiliationAuditLog

```java
@Document(collection = "affiliation_audit_logs")
public class AffiliationAuditLog {
    @Id private String id;
    private String affiliationId;
    private String entityId;
    private String doctorId;
    private String actorUserId;
    private String actorRole;
    private String action;          // e.g. AFFILIATION_REQUESTED, AVAILABILITY_UPDATED
    private Map<String, Object> metadata;
    private Instant timestamp;
}
```

---

## 7. Affiliation Lifecycle

### 7.1 State Machine

```
                              ┌──────────────┐
                              │   REJECTED   │◄──── peer reject / admin reject
                              └──────────────┘
                                     ▲
                                     │
┌──────┐   initiate   ┌──────────────────────┐   peer accept   ┌─────────────────────────┐
│ NEW  │─────────────►│  PENDING_PEER_ACCEPT │────────────────►│ PENDING_ADMIN_APPROVAL  │
└──────┘              └──────────────────────┘                 └─────────────────────────┘
                              │                                           │
                              │ peer reject                               │ admin approve
                              ▼                                           ▼
                        ┌──────────┐                              ┌──────────────┐
                        │ REJECTED │                              │    ACTIVE    │
                        └──────────┘                              └──────────────┘
                                                                        │
                                                    ┌───────────────────┼───────────────────┐
                                                    │ suspend           │ terminate          │
                                                    ▼                   ▼                    │
                                              ┌───────────┐      ┌─────────────┐           │
                                              │ SUSPENDED │─────►│ TERMINATED  │           │
                                              └───────────┘      └─────────────┘           │
                                                    │ reinstate                               │
                                                    └──────────────────────────────────────►│
                                                                                      ACTIVE │
```

### 7.2 State Transition Rules

| From | To | Trigger | Actor | Preconditions |
|------|----|---------|-------|---------------|
| — | `PENDING_PEER_ACCEPT` | `initiate()` | Doctor or Entity Admin | Both parties verified; no duplicate affiliation |
| `PENDING_PEER_ACCEPT` | `PENDING_ADMIN_APPROVAL` | `accept()` | Non-initiating party | — |
| `PENDING_PEER_ACCEPT` | `REJECTED` | `reject()` | Non-initiating party | Reason required |
| `PENDING_ADMIN_APPROVAL` | `ACTIVE` | `approve()` | Platform Admin | Doctor license valid; entity verified |
| `PENDING_ADMIN_APPROVAL` | `REJECTED` | `reject()` | Platform Admin | Reason required |
| `ACTIVE` | `SUSPENDED` | `suspend()` | Admin or either party | Reason required |
| `SUSPENDED` | `ACTIVE` | `reinstate()` | Platform Admin | — |
| `ACTIVE` / `SUSPENDED` | `TERMINATED` | `terminate()` | Admin or either party | Cascade staff revocation |

### 7.3 State Machine Interface

```java
public interface IAffiliationStateMachine {
    EntityAffiliation initiate(AffiliationInitiateRequest request);
    EntityAffiliation acceptPeer(String affiliationId, String userId);
    EntityAffiliation rejectPeer(String affiliationId, String userId, String reason);
    EntityAffiliation approveAdmin(String affiliationId, String adminUserId);
    EntityAffiliation rejectAdmin(String affiliationId, String adminUserId, String reason);
    EntityAffiliation suspend(String affiliationId, String userId, String reason);
    EntityAffiliation reinstate(String affiliationId, String adminUserId);
    EntityAffiliation terminate(String affiliationId, String userId, String reason);
}
```

All transitions:
1. Validate actor permissions
2. Validate current state
3. Persist new state
4. Publish domain event
5. Write audit log

---

## 8. Role-Based Access Control

### 8.1 Platform Roles (extend `RolesEnum`)

```java
public enum RolesEnum {
    DOCTOR,
    ADMIN,
    USER,
    COLLABORATOR,
    ROGER,
    // NEW
    ENTITY_ADMIN,
    ENTITY_SUPERVISOR,
    ENTITY_COLLABORATOR,
}
```

> **Note:** Entity roles are also stored on `EntityMember.role` for fine-grained entity-scoped permissions. JWT carries the primary role; entity membership is resolved at request time.

### 8.2 Permission Model

Permissions are **strings** (not hard-coded booleans) for extensibility:

```java
public enum EntityPermission {
    // Entity management
    ENTITY_READ, ENTITY_UPDATE, ENTITY_MANAGE_MEMBERS,

    // Affiliation
    AFFILIATION_INITIATE, AFFILIATION_ACCEPT, AFFILIATION_VIEW,
    AFFILIATION_MANAGE_AVAILABILITY, AFFILIATION_TERMINATE,

    // Staff
    STAFF_ASSIGN_ENTITY, STAFF_ASSIGN_DOCTOR, STAFF_REVOKE, STAFF_VIEW,

    // Appointments (entity-scoped)
    APPOINTMENT_VIEW_ENTITY, APPOINTMENT_MANAGE_ENTITY,

    // Admin
    AFFILIATION_ADMIN_APPROVE,
}
```

**Default permission sets by role:**

| Role | Default Permissions |
|------|---------------------|
| `ENTITY_ADMIN` | All entity-scoped permissions |
| `ENTITY_SUPERVISOR` | All except `ENTITY_UPDATE`, `ENTITY_MANAGE_MEMBERS` |
| `ENTITY_COLLABORATOR` | `AFFILIATION_VIEW`, `APPOINTMENT_VIEW_ENTITY`, `APPOINTMENT_MANAGE_ENTITY` (if assigned) |
| `DOCTOR` | Full on own affiliations; `STAFF_ASSIGN_DOCTOR` |
| `COLLABORATOR` (doctor-context) | Inherited from `DoctorAssociation.permissions` |
| `COLLABORATOR` (entity-context) | Only permissions on `AffiliationStaffAssignment` |
| `ADMIN` | `AFFILIATION_ADMIN_APPROVE` + all read |

### 8.3 Authorization Flow

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter          → authenticate user, extract JWT claims
    │
    ▼
ContextResolutionFilter          → resolve EffectiveContext from headers
    │
    ▼
@PreAuthorize / PermissionEvaluator → check role + permissions for resource
    │
    ▼
Service Layer                    → enforce data sharing policy on reads
    │
    ▼
Response
```

### 8.4 Permission Matrix

| Action | ADMIN | ENTITY_ADMIN | SUPERVISOR | ENTITY_COLLAB | DOCTOR | Doctor-Ctx Collab | Entity-Ctx Collab |
|--------|:-----:|:------------:|:----------:|:-------------:|:------:|:-----------------:|:-----------------:|
| Create entity | — | ✓ | — | — | — | — | — |
| Initiate affiliation | — | ✓ | ✓ | — | ✓ | — | — |
| Peer accept/reject | — | ✓ | — | — | ✓ | — | — |
| Admin approve | ✓ | — | — | — | — | — | — |
| Set entity availability | — | ✓ | ✓ | — | ✓ | — | ✓ (assigned only) |
| Assign entity staff | — | ✓ | ✓ | — | — | — | — |
| Assign doctor staff | — | — | — | — | ✓ | — | — |
| View shared doctor data | — | ✓ | ✓ | scoped | own | doctor ctx | affiliation only |
| Manage entity appointments | — | ✓ | ✓ | scoped | ✓ | doctor ctx | affiliation only |
| Terminate affiliation | ✓ | ✓ | — | — | ✓ | — | — |

---

## 9. Context Model

### 9.1 EffectiveContext

Central object resolved once per request:

```java
public class EffectiveContext {
    private String userId;
    private String primaryRole;          // from JWT

    // Optional scopes (null if not applicable)
    private String doctorId;
    private String entityId;
    private String affiliationId;

    // Resolved permissions for this request
    private Set<String> permissions;

    // Assignment scope (for collaborators)
    private StaffAssignmentScope staffScope;

    public boolean hasPermission(String permission);
    public boolean isEntityScoped();
    public boolean isAffiliationScoped();
}
```

### 9.2 Context Headers

| Header | Required When | Purpose |
|--------|---------------|---------|
| `Authorization: Bearer <jwt>` | Always | Authentication |
| `X-Active-Doctor-Id` | Collaborator in doctor context | Existing — unchanged |
| `X-Active-Entity-Id` | Entity member operating in entity | NEW |
| `X-Active-Affiliation-Id` | Entity-context staff on specific affiliation | NEW |

### 9.3 Context Resolution Rules

```
IF role == DOCTOR
    → doctorId = JWT doctorId, no entity/affiliation required

IF role == COLLABORATOR
    IF X-Active-Affiliation-Id present
        → validate AffiliationStaffAssignment (scope=ENTITY_CONTEXT)
        → set affiliationId, entityId, doctorId from assignment
    ELSE IF X-Active-Doctor-Id present
        → validate DoctorAssociation (existing flow)
        → set doctorId

IF role IN (ENTITY_ADMIN, ENTITY_SUPERVISOR, ENTITY_COLLABORATOR)
    → require X-Active-Entity-Id
    → validate EntityMember membership
    → set entityId
    IF X-Active-Affiliation-Id present
        → validate affiliation belongs to entity
        → set affiliationId, doctorId
```

### 9.4 ContextService API

```java
public interface IContextService {
    EffectiveContext resolveContext(HttpServletRequest request);
    List<UserContextOptionDTO> getAvailableContexts(String userId);
    void setActiveContext(String userId, ContextSwitchRequest request);
}
```

---

## 10. Data Sharing Policy

### 10.1 Principles

1. **Opt-in fields only** — parties choose what to share at initiation
2. **Versioned policy** — changes require re-acceptance by both parties
3. **Enforced at service layer** — `DataSharingService.filterDoctorProfile()` strips non-shared fields
4. **Never share patient data by default** in v1

### 10.2 Default Policy Templates

**Standard (recommended default):**

| Direction | Shared Fields |
|-----------|---------------|
| Doctor → Entity | `firstName`, `lastName`, `specialization`, `profilePicture`, `medicalLicense`, `verificationStatus` |
| Entity → Doctor | `name`, `address`, `city`, `phoneNumber`, `departments`, `logoUrl` |

**Minimal:**

| Direction | Shared Fields |
|-----------|---------------|
| Doctor → Entity | `firstName`, `lastName`, `specialization` |
| Entity → Doctor | `name`, `address` |

### 10.3 DataSharingService

```java
public interface IDataSharingService {
    DoctorSharedProfileDTO filterDoctorProfile(DoctorEntity doctor, DataSharingPolicy policy);
    EntitySharedProfileDTO filterEntityProfile(HealthcareEntity entity, DataSharingPolicy policy);
    DataSharingPolicy createDefaultPolicy();
    DataSharingPolicy updatePolicy(String affiliationId, DataSharingPolicy newPolicy, String userId);
}
```

---

## 11. Availability & Scheduling

### 11.1 Two-Tier Availability Model

| Tier | Location | Used For |
|------|----------|----------|
| **Global** | `DoctorEntity.availability` | Personal practice, non-affiliated bookings |
| **Entity-scoped** | `EntityAffiliation.entityAvailability` | Bookings at a specific hospital |

Both reuse existing `DayAvailability` and `TimeSlot` models.

### 11.2 Conflict Detection

```java
public interface IAvailabilityConflictService {
    ConflictCheckResult checkConflicts(String doctorId, String affiliationId, List<DayAvailability> proposed);
    ConflictCheckResult checkAllActiveAffiliations(String doctorId);
}
```

**Rules:**
- Slots across all `ACTIVE` affiliations for the same doctor must not overlap
- Global availability is included in conflict checks when `settings.includeGlobalAvailability = true`
- Return structured conflicts: `{ day, slot, conflictingAffiliationId, conflictingEntityName }`

### 11.3 Appointment Integration

Extend `AppointmentEntity`:

```java
// NEW optional fields
private String entityId;
private String affiliationId;
private String bookedAtEntityName;  // cached
```

**Booking rules:**
- If doctor has active affiliation at entity → patient can book entity-scoped slots
- Entity-context staff can only create appointments using entity availability
- Appointment queries filter by `EffectiveContext`

### 11.4 Availability Edit Permissions

| Actor | Can Edit Global | Can Edit Entity-Scoped |
|-------|:---------------:|:----------------------:|
| Doctor | ✓ | ✓ (own affiliations) |
| Entity Admin / Supervisor | — | ✓ (configurable: direct or proposal) |
| Entity-context staff | — | ✓ (if permission granted) |
| Doctor-context staff | ✓ (if doctor granted) | ✓ (if doctor granted) |

**Configurable entity setting:** `availabilityEditMode: DIRECT | PROPOSE`

- `DIRECT` — entity staff edits take effect immediately (doctor notified)
- `PROPOSE` — edits create a proposal; doctor must accept

---

## 12. Staff Assignment Model

### 12.1 Assignment Scopes

```
┌─────────────────────────────────────────────────────────────────┐
│                    DOCTOR-CONTEXT ASSIGNMENT                     │
│  Assigned by: Doctor                                             │
│  Scope: Doctor's full context (optionally limited to affiliations)│
│  Can access: All doctor data per DoctorAssociation.permissions   │
│  Cannot access: Other doctors                                   │
│  Pattern: Extends existing CollaboratorProfile + DoctorAssociation│
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    ENTITY-CONTEXT ASSIGNMENT                     │
│  Assigned by: Entity Admin / Supervisor                          │
│  Scope: Single affiliation (doctor + entity pair)                  │
│  Can access: Shared doctor data per DataSharingPolicy            │
│            Entity availability for that affiliation              │
│            Appointments at that entity for that doctor           │
│  Cannot access: Doctor's other affiliations, global schedule,     │
│                 doctor's personal/clinic data beyond policy        │
└─────────────────────────────────────────────────────────────────┘
```

### 12.2 Staff Assignment Flow

**Entity assigns staff to affiliation:**
1. Entity Admin selects affiliation (doctor at their hospital)
2. Invites user by email (reuse invitation pattern)
3. On accept → `AffiliationStaffAssignment` created with `scope=ENTITY_CONTEXT`
4. Doctor receives informational notification (not approval required in v1)

**Doctor assigns staff with affiliation access:**
1. Doctor selects affiliation(s) from their list
2. Invites/adds existing collaborator
3. `AffiliationStaffAssignment` created with `scope=DOCTOR_CONTEXT`
4. Collaborator's existing `DoctorAssociation` permissions apply; affiliation IDs optionally restrict scope

### 12.3 Termination Cascade

When affiliation → `TERMINATED`:
1. Revoke all `AffiliationStaffAssignment` where `affiliationId` matches
2. Notify assigned entity-context staff
3. Remove affiliation from doctor-context staff's allowed affiliations (if restricted)
4. Archive entity-scoped appointments (status → `ARCHIVED`)
5. Audit log entry

---

## 13. Service Layer Design

### 13.1 Service Responsibilities

| Service | Responsibility |
|---------|----------------|
| `EntityService` | CRUD for HealthcareEntity, member management, verification |
| `AffiliationService` | Query affiliations, shared profile retrieval |
| `AffiliationStateMachine` | All state transitions (single entry point) |
| `AvailabilityConflictService` | Overlap detection across affiliations |
| `DataSharingService` | Field-level filtering per policy |
| `StaffAssignmentService` | Assign, revoke, list staff per affiliation |
| `ContextService` | Resolve and switch active context |
| `EntityNotificationOrchestrator` | Map domain events → notifications |
| `AuditService` | Append-only audit log writes |

### 13.2 Domain Events

```java
// Published by AffiliationStateMachine and other services
public record AffiliationRequestedEvent(String affiliationId, AffiliationInitiator initiatedBy) {}
public record AffiliationPeerAcceptedEvent(String affiliationId) {}
public record AffiliationAdminApprovedEvent(String affiliationId) {}
public record AffiliationRejectedEvent(String affiliationId, String reason) {}
public record AffiliationTerminatedEvent(String affiliationId, String reason) {}
public record AffiliationAvailabilityChangedEvent(String affiliationId, String changedByUserId) {}
public record StaffAssignedEvent(String assignmentId, StaffAssignmentScope scope) {}
public record StaffRevokedEvent(String assignmentId) {}
```

### 13.3 Event Listeners

```java
@Component
public class AffiliationNotificationListener {
    @EventListener @Async
    public void onAffiliationRequested(AffiliationRequestedEvent event) { ... }

    @EventListener @Async
    public void onAdminApproved(AffiliationAdminApprovedEvent event) { ... }
    // ...
}

@Component
public class AffiliationAuditListener {
    @EventListener
    public void onAnyAffiliationEvent(AffiliationEvent event) { ... }
}
```

Controllers **never** send notifications directly.

### 13.4 Dependency Graph

```
AffiliationController
    └── AffiliationService
            ├── AffiliationStateMachine
            │       ├── AffiliationRepository
            │       ├── EntityRepository
            │       ├── DoctorRepository
            │       └── ApplicationEventPublisher
            ├── DataSharingService
            ├── AvailabilityConflictService
            └── AuditService

StaffAssignmentController
    └── StaffAssignmentService
            ├── AffiliationRepository
            ├── StaffAssignmentRepository
            └── ApplicationEventPublisher
```

---

## 14. API Specification

Base path: `/api/v1`

### 14.1 Entity APIs

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/entities` | ENTITY_ADMIN | Create entity |
| `GET` | `/entities/{entityId}` | Member / Admin | Get entity profile |
| `PUT` | `/entities/{entityId}` | ENTITY_ADMIN | Update entity |
| `GET` | `/entities/{entityId}/members` | ENTITY_ADMIN, SUPERVISOR | List members |
| `POST` | `/entities/{entityId}/members` | ENTITY_ADMIN | Add member |
| `PUT` | `/entities/{entityId}/members/{userId}` | ENTITY_ADMIN | Update member role |
| `DELETE` | `/entities/{entityId}/members/{userId}` | ENTITY_ADMIN | Remove member |
| `GET` | `/entities/search` | DOCTOR, ADMIN | Search verified entities |

### 14.2 Affiliation APIs

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/affiliations` | DOCTOR, ENTITY_ADMIN, SUPERVISOR | Initiate affiliation |
| `GET` | `/affiliations` | Authenticated | List affiliations (filtered by context) |
| `GET` | `/affiliations/{id}` | Involved parties | Get affiliation detail + shared data |
| `PUT` | `/affiliations/{id}/accept` | Peer party | Accept affiliation request |
| `PUT` | `/affiliations/{id}/reject` | Peer party | Reject with reason |
| `PUT` | `/admin/affiliations/{id}/approve` | ADMIN | Admin approve |
| `PUT` | `/admin/affiliations/{id}/reject` | ADMIN | Admin reject |
| `PUT` | `/affiliations/{id}/suspend` | ADMIN, involved parties | Suspend |
| `PUT` | `/affiliations/{id}/terminate` | ADMIN, involved parties | Terminate |
| `PUT` | `/affiliations/{id}/data-sharing` | DOCTOR, ENTITY_ADMIN | Update sharing policy |

### 14.3 Availability APIs

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET` | `/affiliations/{id}/availability` | Involved parties | Get entity-scoped availability |
| `PUT` | `/affiliations/{id}/availability` | Authorized editors | Update availability |
| `GET` | `/affiliations/{id}/availability/conflicts` | Authorized editors | Check conflicts before save |

### 14.4 Staff APIs

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/affiliations/{id}/staff` | DOCTOR, ENTITY_ADMIN, SUPERVISOR | Assign staff |
| `GET` | `/affiliations/{id}/staff` | Involved parties | List assigned staff |
| `DELETE` | `/affiliations/{id}/staff/{userId}` | Assigner or ADMIN | Revoke staff |

### 14.5 Context APIs

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET` | `/contexts` | Authenticated | List all contexts user can switch to |
| `PUT` | `/contexts/active` | Authenticated | Set active context |

### 14.6 Standard Response Envelope

```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "context": {
      "entityId": "ent_abc",
      "affiliationId": "aff_xyz"
    }
  }
}
```

### 14.7 Error Codes

| Code | HTTP | Description |
|------|------|-------------|
| `AFFILIATION_NOT_FOUND` | 404 | Affiliation does not exist |
| `AFFILIATION_INVALID_STATE` | 409 | Action not allowed in current state |
| `AFFILIATION_DUPLICATE` | 409 | Doctor already affiliated with entity |
| `AFFILIATION_CONFLICT` | 409 | Availability overlap detected |
| `CONTEXT_REQUIRED` | 400 | Missing required context header |
| `CONTEXT_FORBIDDEN` | 403 | User not authorized in this context |
| `DATA_SHARING_VIOLATION` | 403 | Attempted access to non-shared field |

---

## 15. Frontend Architecture

### 15.1 App Structure (fhpotionV2)

Role-based route groups using expo-router:

```
app/
├── (auth)/                    # existing
├── (tabs)/                    # doctor dashboard (existing)
│   ├── dashboard/
│   ├── appointments/
│   ├── notifications/
│   └── profile/
│       └── affiliations/      # NEW: manage affiliations
├── (entity-tabs)/             # NEW: entity dashboard
│   ├── overview/
│   ├── doctors/
│   ├── schedule/
│   ├── staff/
│   ├── appointments/
│   └── settings/
├── affiliation/               # NEW: shared flows
│   ├── request/[entityId].tsx
│   ├── detail/[id].tsx
│   └── availability/[id].tsx
└── _layout.tsx                # role-based redirect
```

### 15.2 Redux Store Extensions

```typescript
// New slices (mirror activeDoctorSlice pattern)
activeEntitySlice      // entity switcher for entity members
activeAffiliationSlice // affiliation switcher for scoped staff
affiliationSlice       // affiliation list, detail, actions
entitySlice            // entity profile, members
entityStaffSlice       // staff assignments
```

### 15.3 API Client Extensions

```typescript
// client.ts interceptor additions
if (activeEntityId) headers['X-Active-Entity-Id'] = activeEntityId;
if (activeAffiliationId) headers['X-Active-Affiliation-Id'] = activeAffiliationId;
```

### 15.4 Context Switch Behavior

On context switch (same pattern as `switchActiveDoctor`):
1. Call `PUT /contexts/active`
2. Update Redux state
3. Refetch: appointments, notifications, dashboard data
4. Re-subscribe WebSocket topics for new context

### 15.5 Entity Dashboard Screens

| Screen | Key Components | Data Sources |
|--------|----------------|--------------|
| Overview | KPI cards, pending actions, today's schedule | `/affiliations`, `/appointments` |
| Doctors | Affiliated roster, search, pending requests | `/affiliations?entityId=` |
| Schedule | Multi-doctor calendar, department filter | `/affiliations/{id}/availability` |
| Staff | Member list, assignment manager | `/entities/{id}/members`, `/affiliations/{id}/staff` |
| Appointments | Entity-scoped appointment list | `/appointments?entityId=` |
| Settings | Entity profile, policies, notifications | `/entities/{id}` |

---

## 16. Notification Architecture

### 16.1 Extended Recipient Types

```java
public enum NotificationRecipientType {
    // existing
    INDIVIDUAL, ROLE, BROADCAST, DOCTOR_COLLABORATORS, ADMINS,
    // NEW
    ENTITY_MEMBERS,              // all active members of an entity
    ENTITY_SUPERVISORS,          // supervisors of an entity
    AFFILIATION_PARTIES,         // doctor + entity admins for one affiliation
    AFFILIATION_ENTITY_STAFF,    // entity-context staff on an affiliation
    AFFILIATION_DOCTOR_STAFF,    // doctor-context staff for an affiliation
}
```

### 16.2 Notification Context Envelope

Extend `NotificationEntity` with context metadata:

```java
public class NotificationContext {
    private String entityId;
    private String entityName;
    private String doctorId;
    private String doctorName;
    private String affiliationId;
    private NotificationScope scope;  // DOCTOR, ENTITY, AFFILIATION, PLATFORM
}
```

### 16.3 Notification Event Map

| Domain Event | Recipients | Channels | Priority |
|--------------|------------|----------|----------|
| `AffiliationRequested` (by doctor) | Entity Admins, Supervisors | push, in-app, email | HIGH |
| `AffiliationRequested` (by entity) | Doctor | push, in-app, email | HIGH |
| `AffiliationPeerAccepted` | Initiator + Admins | in-app, email | NORMAL |
| `AffiliationAdminApproved` | Doctor + Entity Admins | push, in-app, email | HIGH |
| `AffiliationRejected` | Initiator | push, in-app | NORMAL |
| `AffiliationTerminated` | Both parties + assigned staff | push, in-app, email | HIGH |
| `AvailabilityChanged` | Doctor (if changed by entity) | push, in-app | NORMAL |
| `StaffAssigned` (entity) | Assigned user + Doctor (info) | push, in-app | NORMAL |
| `StaffRevoked` | Assigned user | in-app | LOW |

### 16.4 Notification Filtering API

```
GET /api/v1/notification?scope=ENTITY&contextId={entityId}
GET /api/v1/notification?scope=AFFILIATION&contextId={affiliationId}
```

Frontend notification tab filters by active context automatically.

### 16.5 WebSocket Topics

```
/topic/notifications/{userId}                          # existing — all notifications
/topic/notifications/{userId}/entity/{entityId}        # NEW — entity-scoped
/topic/notifications/{userId}/affiliation/{affId}      # NEW — affiliation-scoped
```

---

## 17. Security & Audit

### 17.1 Security Checklist

- [ ] All affiliation endpoints require authentication
- [ ] Context headers validated against user's memberships/assignments
- [ ] Data sharing policy enforced on every profile read
- [ ] Admin approval required before affiliation goes ACTIVE
- [ ] Rate limiting on affiliation initiation (prevent spam)
- [ ] Input validation on all DTOs (Jakarta Validation)
- [ ] No sensitive data in JWT (only userId, role, doctorId)
- [ ] CORS unchanged; context headers added to allowed headers

### 17.2 Audit Requirements

Every state-changing operation writes to `affiliation_audit_logs`:

```json
{
  "affiliationId": "aff_123",
  "actorUserId": "user_456",
  "actorRole": "ENTITY_ADMIN",
  "action": "AVAILABILITY_UPDATED",
  "metadata": {
    "previousSlotCount": 5,
    "newSlotCount": 7,
    "changedDays": ["MONDAY", "WEDNESDAY"]
  },
  "timestamp": "2026-06-30T10:00:00Z"
}
```

**Audited actions:** All state transitions, availability changes, staff assign/revoke, data sharing policy updates, context switches.

### 17.3 License & Verification Gates

Before admin approval:
- Doctor `verificationStatus` must be `VERIFIED`
- Doctor medical license must not be expired (reuse `DoctorLicenseExpiryScheduler`)
- Entity `verificationStatus` must be `VERIFIED`

---

## 18. Database Design

### 18.1 Collections

| Collection | Est. Documents | Growth Rate |
|------------|------------------|-------------|
| `healthcare_entities` | Low | Slow |
| `entity_affiliations` | Medium | Moderate |
| `affiliation_staff` | Medium | Moderate |
| `affiliation_audit_logs` | High | Fast (append-only) |

### 18.2 Indexes

```javascript
// healthcare_entities
{ entityId: 1 }                          // unique
{ status: 1, verificationStatus: 1 }     // admin queries
{ name: "text", city: 1 }                // search

// entity_affiliations
{ affiliationId: 1 }                     // unique
{ entityId: 1, doctorId: 1 }             // unique compound
{ entityId: 1, status: 1 }               // entity roster
{ doctorId: 1, status: 1 }               // doctor affiliations
{ status: 1, createdAt: -1 }             // admin approval queue

// affiliation_staff
{ affiliationId: 1, userId: 1 }          // unique compound
{ userId: 1, active: 1 }                 // user's assignments
{ entityId: 1, active: 1 }               // entity staff list

// affiliation_audit_logs
{ affiliationId: 1, timestamp: -1 }      // affiliation history
{ entityId: 1, timestamp: -1 }           // entity audit
{ actorUserId: 1, timestamp: -1 }        // user activity
```

### 18.3 Denormalization Strategy

Cached fields on `EntityAffiliation` (`doctorName`, `entityName`, etc.) are updated via domain events when source profiles change. This avoids joins on list queries.

---

## 19. Event Flows

### 19.1 Doctor-Initiated Affiliation (Happy Path)

```
Doctor App                API                     Entity Admin           Platform Admin
    │                      │                          │                      │
    │ POST /affiliations   │                          │                      │
    │─────────────────────►│                          │                      │
    │                      │ validate + persist       │                      │
    │                      │ status=PENDING_PEER_ACCEPT                      │
    │                      │ emit AffiliationRequestedEvent                  │
    │                      │─────────────────────────►│ notify               │
    │                      │                          │                      │
    │                      │                          │ PUT .../accept       │
    │                      │◄─────────────────────────│                      │
    │                      │ status=PENDING_ADMIN_APPROVAL                   │
    │                      │ emit PeerAcceptedEvent   │                      │
    │                      │──────────────────────────────────────────────────►│ notify
    │                      │                          │                      │
    │                      │                          │                      │ PUT .../approve
    │                      │◄─────────────────────────────────────────────────│
    │                      │ status=ACTIVE            │                      │
    │                      │ emit AdminApprovedEvent  │                      │
    │◄─────────────────────│ notify                   │◄─────────────────────│
    │  affiliation active  │                          │  affiliation active  │
```

### 19.2 Entity Staff Manages Doctor Availability

```
Entity Collaborator       API                     Doctor App
    │                      │                          │
    │ PUT .../availability │                          │
    │ (X-Active-Affiliation-Id)                       │
    │─────────────────────►│                          │
    │                      │ validate ENTITY_CONTEXT  │
    │                      │ check conflicts          │
    │                      │ persist                  │
    │                      │ emit AvailabilityChanged │
    │                      │─────────────────────────►│ notify
    │◄─────────────────────│ 200 OK                   │
```

### 19.3 Context Switch

```
User (any role)           API                     Redux Store
    │                      │                          │
    │ PUT /contexts/active │                          │
    │ { entityId, ... }    │                          │
    │─────────────────────►│                          │
    │                      │ validate membership       │
    │                      │ return resolved context   │
    │◄─────────────────────│                          │
    │                      │                          │ update activeEntitySlice
    │                      │                          │ refetch appointments
    │                      │                          │ refetch notifications
```

---

## 20. Migration Strategy

### Phase M0: Coexistence (No Breaking Changes)

- Existing `DoctorEntity.clinicName/Address/...` fields remain functional
- New entity features are opt-in
- Appointments without `affiliationId` continue using global availability

### Phase M1: Claim Clinic

- Doctors with existing clinic data can "claim" → creates `HealthcareEntity` + self-affiliation
- Admin fast-track approval for self-claimed entities

### Phase M2: Dual Mode Booking

- Public booking shows entity-affiliated slots alongside personal slots
- `AppointmentEntity.entityId` populated for entity bookings

### Phase M3: Deprecation

- UI nudges doctors to migrate clinic fields to entity affiliations
- API marks clinic fields as `@Deprecated`
- No hard removal until v2

---

## 21. Implementation Phases

### Phase 1: Foundation (Weeks 1–4)

**Backend:**
- [ ] Enums: `EntityType`, `EntityStatus`, `AffiliationStatus`, etc.
- [ ] Models: `HealthcareEntity`, `EntityAffiliation`, `DataSharingPolicy`
- [ ] Repositories with indexes
- [ ] `EntityService`, `AffiliationStateMachine`
- [ ] Admin approval endpoints
- [ ] `AuditService` + audit listener
- [ ] Unit tests for state machine

**Frontend:**
- [ ] Affiliation list/detail screens (doctor side)
- [ ] Affiliation request flow

**Exit criteria:** Doctor can initiate affiliation → entity accepts → admin approves.

---

### Phase 2: Availability & Data Sharing (Weeks 5–7)

**Backend:**
- [ ] Affiliation-scoped availability CRUD
- [ ] `AvailabilityConflictService`
- [ ] `DataSharingService` with field filtering
- [ ] Extend `AppointmentEntity` with `entityId`, `affiliationId`

**Frontend:**
- [ ] Entity-scoped availability editor
- [ ] Conflict warning UI

**Exit criteria:** Doctor sets per-entity schedule; conflicts detected; shared profiles respect policy.

---

### Phase 3: Entity Dashboard (Weeks 8–11)

**Backend:**
- [ ] Entity member management
- [ ] Entity role auth (`ENTITY_ADMIN`, `SUPERVISOR`, `ENTITY_COLLABORATOR`)
- [ ] `ContextService` + `ContextResolutionFilter`
- [ ] Entity-scoped appointment queries

**Frontend:**
- [ ] `(entity-tabs)/` route group
- [ ] Entity overview, doctors roster, schedule views
- [ ] `activeEntitySlice` + entity switcher
- [ ] Role-based app entry redirect

**Exit criteria:** Entity admin can manage roster, view schedules, see entity appointments.

---

### Phase 4: Staff Assignment (Weeks 12–14)

**Backend:**
- [ ] `AffiliationStaffAssignment` model + service
- [ ] Entity-context and doctor-context assignment flows
- [ ] Permission enforcement per scope
- [ ] Termination cascade

**Frontend:**
- [ ] Staff assignment UI (entity + doctor sides)
- [ ] `activeAffiliationSlice` for entity-context staff

**Exit criteria:** Entity assigns receptionist scoped to one doctor's affiliation; doctor assigns PA with full context.

---

### Phase 5: Notifications (Weeks 15–16)

**Backend:**
- [ ] Extended `NotificationRecipientType`
- [ ] `NotificationContext` on notifications
- [ ] `EntityNotificationOrchestrator` + event listeners
- [ ] Email templates for affiliation events
- [ ] Context-filtered notification API

**Frontend:**
- [ ] Context-filtered notification list
- [ ] Actionable notification buttons (accept/reject)

**Exit criteria:** All affiliation events trigger correct notifications to correct parties.

---

### Phase 6: Hardening & Migration (Weeks 17–18)

- [ ] End-to-end integration tests
- [ ] Claim-clinic migration flow
- [ ] Performance testing on affiliation queries
- [ ] Security review
- [ ] Documentation update

---

## 22. Extension Points

Designed for future features without schema breaking changes:

| Future Feature | Extension Mechanism |
|----------------|---------------------|
| **Departments** | Already on `HealthcareEntity.departments`; affiliation has optional `department` field |
| **Billing / revenue share** | New `AffiliationBillingPolicy` embedded doc; new service |
| **Patient sharing** | New feature flag in `DataSharingPolicy.featureFlags` |
| **Multi-location entities** | New `EntityLocation` embedded doc on `HealthcareEntity` |
| **Affiliation tiers** | New enum `AffiliationTier` (VISITING, FULL_TIME, ON_CALL) |
| **External integrations** | Webhook events alongside domain events |
| **Proposal workflow** | `AvailabilityProposal` collection; `availabilityEditMode=PROPOSE` already planned |
| **Entity web portal** | Separate Next.js app consuming same API; entity routes already RESTful |
| **Analytics** | Audit logs + appointment aggregates; new read models |
| **Custom roles** | `EntityMember.permissions` override defaults; permissions are strings |

---

## 23. Open Decisions

| # | Decision | Options | Recommendation |
|---|----------|---------|----------------|
| D1 | Entity verification process | Manual admin / Third-party API / Self-declare + audit | Manual admin for v1; API hook for v2 |
| D2 | Availability edit by entity staff | Direct edit / Proposal + doctor accept | Configurable per entity (`availabilityEditMode`) |
| D3 | Doctor notification on entity staff assign | Info only / Requires doctor approval | Info only for v1 |
| D4 | App delivery for entity dashboard | Same Expo app / Separate web app | Same Expo app with role routing for v1 |
| D5 | Existing collaborator access to affiliations | Auto-include all / Manual per affiliation | Manual per affiliation (explicit consent) |
| D6 | Patient data at entity | Never / Appointment metadata only / Full (future) | Appointment metadata only for v1 |
| D7 | JWT entity role vs membership lookup | Role in JWT / Resolve from DB each request | Resolve from DB (membership can change without re-login) |

---

## 24. Appendix

### A. Enum Definitions

```java
public enum EntityType { HOSPITAL, CLINIC, DIAGNOSTIC, PHARMACY, OTHER }
public enum EntityStatus { DRAFT, PENDING_VERIFICATION, ACTIVE, SUSPENDED }
public enum AffiliationStatus {
    PENDING_PEER_ACCEPT, PENDING_ADMIN_APPROVAL, ACTIVE, SUSPENDED, TERMINATED, REJECTED
}
public enum AffiliationInitiator { DOCTOR, ENTITY }
public enum EntityMemberRole { ENTITY_ADMIN, SUPERVISOR, ENTITY_COLLABORATOR }
public enum StaffAssignmentScope { DOCTOR_CONTEXT, ENTITY_CONTEXT }
public enum StaffAssigner { DOCTOR, ENTITY }
public enum NotificationScope { DOCTOR, ENTITY, AFFILIATION, PLATFORM }
```

### B. Existing Code References

| Pattern | File | Reuse For |
|---------|------|-----------|
| Collaborator association | `models/DoctorAssociation.java` | `AffiliationStaffAssignment` structure |
| Invitation flow | `services/impl/InvitationServiceImpl.java` | Affiliation invite + staff invite |
| Context switching | `utils/CurrentUserName.java` | Extend for entity/affiliation context |
| Active doctor slice | `fhpotionV2/src/store/slices/activeDoctorSlice.ts` | Template for entity/affiliation slices |
| Notification delivery | `services/impl/NotificationService.java` | Extend recipient resolution |
| Availability model | `models/DayAvailability.java` | Affiliation-scoped availability |
| Role authorization | `utils/RoleUtils.java` | Extend for entity permissions |

### C. Related Documents

- [SYSTEM_ARCHITECTURE.md](./SYSTEM_ARCHITECTURE.md) — Overall system architecture
- [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md) — Video consultation implementation
- [IMPLEMENTATION_PHASES.md](./IMPLEMENTATION_PHASES.md) — Phased delivery tracker

### D. Glossary of API Request/Response Examples

**Initiate Affiliation (Doctor → Entity):**

```json
POST /api/v1/affiliations
{
  "entityId": "ent_abc123",
  "department": "Cardiology",
  "entityAvailability": [
    { "day": "MONDAY", "slots": [{ "startTime": "09:00", "endTime": "13:00" }] },
    { "day": "WEDNESDAY", "slots": [{ "startTime": "14:00", "endTime": "18:00" }] }
  ],
  "dataSharingPolicy": {
    "doctorSharedFields": ["firstName", "lastName", "specialization", "medicalLicense"],
    "entitySharedFields": ["name", "address", "departments", "phoneNumber"]
  }
}
```

**Assign Entity-Context Staff:**

```json
POST /api/v1/affiliations/aff_xyz/staff
{
  "email": "reception@hospital.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "RECEPTIONIST",
  "scope": "ENTITY_CONTEXT",
  "permissions": ["AFFILIATION_VIEW", "APPOINTMENT_MANAGE_ENTITY", "AFFILIATION_MANAGE_AVAILABILITY"]
}
```

**Switch Context:**

```json
PUT /api/v1/contexts/active
{
  "entityId": "ent_abc123",
  "affiliationId": "aff_xyz"
}
```

---

*End of document.*
