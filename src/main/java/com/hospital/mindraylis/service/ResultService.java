package com.hospital.mindraylis.service;

import com.hospital.mindraylis.core.AstmParser;
import com.hospital.mindraylis.domain.model.AnalysisResult;
import com.hospital.mindraylis.domain.model.RawDataLog;
import com.hospital.mindraylis.repository.LogRepository;
import com.hospital.mindraylis.repository.ResultRepository;
import org.springframework.transaction.annotation.Transactional; // Spring Transactional ব্যবহার করা ভালো
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultService {

    private final ResultRepository resultRepo;
    private final LogRepository logRepo;
    private final AstmParser astmParser;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void processAndSave(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            log.warn("Received empty raw data from analyzer. Skipping...");
            return;
        }

        // ১. অডিট ট্রেইল হিসেবে লগ সেভ
        try {
            RawDataLog rawLog = new RawDataLog();
            rawLog.setContent(rawData);
            logRepo.save(rawLog);
            log.info("Raw data logged successfully.");
        } catch (Exception e) {
            log.error("Failed to save raw data log: {}", e.getMessage());
        }

        // ২. ASTM পার্সিং শুরু
        List<String> segments = astmParser.parseRawMessage(rawData);
        String currentSampleId = "UNKNOWN";

        for (String segment : segments) {
            String[] fields = astmParser.getFields(segment);
            if (fields.length == 0) continue;

            String segmentType = fields[0];

            switch (segmentType) {
                case "P": // Patient Segment (Optional log)
                    log.debug("Processing Patient segment: {}", segment);
                    break;

                case "O": // Order Segment
                    if (fields.length > 2) {
                        // Mindray-তে সাধারণত ৩য় ফিল্ডে স্যাম্পল আইডি থাকে (O|1|SampleID|...)
                        String sampleField = fields[2];
                        currentSampleId = astmParser.getComponents(sampleField)[0];
                        log.info("Processing results for Sample ID: {}", currentSampleId);
                    }
                    break;

                case "R": // Result Segment
                    if (fields.length > 3) {
                        try {
                            AnalysisResult res = new AnalysisResult();
                            res.setSampleId(currentSampleId);

                            // Mindray BS-240 Test Code parsing
                            String testInfo = fields[2];
                            String[] testComponents = astmParser.getComponents(testInfo);

                            // অনেক সময় টেস্ট কোড ^^^ কোড ফরম্যাটে থাকে
                            String testCode = testComponents.length >= 4 ? testComponents[3] :
                                    (testComponents.length > 0 ? testComponents[testComponents.length - 1] : testInfo);

                            res.setTestCode(testCode.replace("^", "").trim());
                            res.setResultValue(fields[3]); // Result Value
                            res.setUnit(fields.length > 4 ? fields[4].replace("^", "").trim() : "");

                            // ৩. ডাটাবেসে সেভ এবং WebSocket ব্রডকাস্ট
                            AnalysisResult savedResult = resultRepo.save(res);
                            log.info("Saved result: {} = {} {}", res.getTestCode(), res.getResultValue(), res.getUnit());

                            // WebSocket এর মাধ্যমে রিয়েল-টাইম পুশ
                            messagingTemplate.convertAndSend("/topic/results", savedResult);

                        } catch (Exception e) {
                            log.error("Error parsing Result (R) segment: {}", e.getMessage());
                        }
                    }
                    break;

                case "L": // Terminator Segment
                    log.info("Message termination segment reached for Sample: {}", currentSampleId);
                    break;
            }
        }
    }
}