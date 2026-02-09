package com.heal.doctor.content.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "site_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteStatistics {

    @Id
    private String id;
    
    private Integer totalPatients;
    private Integer totalDoctors;
    private Integer satisfactionRate;
    private String avgWaitTime;
    private String monthlyGrowth;
    private Integer specialtiesCount;
    private Integer citiesCount;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private Instant createdAt;
    private Instant updatedAt;
}
