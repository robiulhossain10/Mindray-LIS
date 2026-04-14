package com.hospital.mindraylis.repository;


import com.hospital.mindraylis.domain.model.RawDataLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends JpaRepository<RawDataLog, Long> {
    // ভবিষ্যতে প্রয়োজন হলে নির্দিষ্ট সময়ের লগ খোঁজার জন্য মেথড যোগ করা যাবে
}