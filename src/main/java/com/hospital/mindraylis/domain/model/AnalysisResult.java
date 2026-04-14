package com.hospital.mindraylis.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String sampleId;
    private String testCode;
    private String resultValue;
    private String unit;
    private String machineName = "MINDRAY_BS240";
    private LocalDateTime createdAt = LocalDateTime.now();
    // Getters & Setters
}