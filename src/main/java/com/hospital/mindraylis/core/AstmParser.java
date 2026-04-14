package com.hospital.mindraylis.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AstmParser {

    /**
     * মেশিন থেকে আসা Raw ডেটাকে ক্লিন করে লাইন বাই লাইন আলাদা করে।
     * Unicode escape (\u0002) ব্যবহার করা হয়েছে Illegal escape character এরর এড়াতে।
     */
    public List<String> parseRawMessage(String rawData) {
        List<String> cleanLines = new ArrayList<>();
        if (rawData == null || rawData.isEmpty()) return cleanLines;

        // ১. কন্ট্রোল ক্যারেক্টার ক্লিন (STX = \u0002)
        String cleanData = rawData.replace("\u0002", "");

        // ২. ASTM সেগমেন্ট সাধারণত Carriage Return (\r) দিয়ে আলাদা করা থাকে
        String[] segments = cleanData.split("\\r|\\n");

        for (String segment : segments) {
            String trimmed = segment.trim();

            if (trimmed.length() > 1) {
                // ৩. চেকসাম এবং ফ্রেম ক্যারেক্টার রিমুভ (ETX = \u0003, ETB = \u0017)
                int etxIdx = trimmed.indexOf('\u0003'); // ETX
                if (etxIdx == -1) etxIdx = trimmed.indexOf('\u0017'); // ETB

                if (etxIdx != -1) {
                    // ETX/ETB এর আগের অংশটুকু আসল ডেটা
                    trimmed = trimmed.substring(0, etxIdx);
                } else if (trimmed.matches(".*[0-9A-F]{2}$")) {
                    // যদি কন্ট্রোল ক্যারেক্টার না থাকে কিন্তু শেষে ২ ডিজিট হেক্স চেকসাম থাকে
                    trimmed = trimmed.substring(0, trimmed.length() - 2);
                }

                // ৪. শুধুমাত্র প্রিন্টেবল ক্যারেক্টার রাখা (ASCII 32-126, |, ^)
                // জাভা রেজেক্সে ডাবল ব্যাকস্ল্যাশ \\ ব্যবহার করতে হয়
                trimmed = trimmed.replaceAll("[^\\x20-\\x7E|\\^]", "");

                if (!trimmed.isEmpty()) {
                    // লাইনের শুরুতে ফ্রেম নম্বর (১, ২, ৩ ইত্যাদি) থাকলে তা বাদ দিয়ে সেগমেন্ট টাইপ (H, P, O, R) রাখা
                    if (Character.isDigit(trimmed.charAt(0)) && trimmed.length() > 1) {
                        trimmed = trimmed.substring(1);
                    }
                    cleanLines.add(trimmed);
                }
            }
        }

        log.info("Successfully parsed {} ASTM segments.", cleanLines.size());
        return cleanLines;
    }

    /**
     * পাইপ (|) দিয়ে ফিল্ড আলাদা করা।
     */
    public String[] getFields(String segment) {
        if (segment == null) return new String[0];
        // -1 নিশ্চিত করে যে শেষের খালি ফিল্ডগুলোও অ্যারেতে আসবে
        return segment.split("\\|", -1);
    }

    /**
     * ক্যারেট (^) দিয়ে কম্পোনেন্ট আলাদা করা।
     */
    public String[] getComponents(String field) {
        if (field == null) return new String[0];
        return field.split("\\^", -1);
    }
}