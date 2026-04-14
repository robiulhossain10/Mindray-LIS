package com.hospital.mindraylis.domain.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class RawDataLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SQLite এর জন্য IDENTITY ব্যবহার করা ভালো
    private Long id;

    @Column(columnDefinition = "TEXT") // @Lob এর বদলে TEXT সরাসরি ডিফাইন করা নিরাপদ
    private String content;

    private LocalDateTime receivedAt = LocalDateTime.now();
}