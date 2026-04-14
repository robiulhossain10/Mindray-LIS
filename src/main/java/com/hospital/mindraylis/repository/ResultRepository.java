package com.hospital.mindraylis.repository;

import com.hospital.mindraylis.domain.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<AnalysisResult, Long> {

    // নির্দিষ্ট স্যাম্পল আইডি দিয়ে রেজাল্ট খোঁজার জন্য
    List<AnalysisResult> findBySampleId(String sampleId);

    // লেটেস্ট রেজাল্টগুলো আগে দেখার জন্য
    List<AnalysisResult> findAllByOrderByCreatedAtDesc();
}