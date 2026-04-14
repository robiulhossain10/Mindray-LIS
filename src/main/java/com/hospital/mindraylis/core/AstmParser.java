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
     */
    public List<String> parseRawMessage(String rawData) {
        List<String> cleanLines = new ArrayList<>();

        if (rawData == null || rawData.isEmpty()) {
            return cleanLines;
        }

        // ১. কন্ট্রোল ক্যারেক্টার ক্লিন করা (STX \x02, ETX \x03 রিমুভ)
        String cleanData = rawData.replaceAll("[\\x02\\x03]", "");

        // ২. সাধারণত ASTM এ প্রতিটি সেগমেন্ট Carriage Return (\r) দিয়ে শেষ হয়।
        String[] segments = cleanData.split("\\r|\\n");

        for (String segment : segments) {
            String trimmed = segment.trim();

            // ৩. চেকসাম হ্যান্ডলিং
            // ASTM এ ডেটার শেষে একটি চেকসাম থাকে (যেমন: "L|1|N31" এখানে 31 হলো চেকসাম)
            // চেকসাম সাধারণত ২ ক্যারেক্টারের হেক্স ভ্যালু হয় যা ডেটা সেগমেন্টের শেষে থাকে।
            if (trimmed.length() > 3) {
                // চেকসাম রিমুভ করার লজিক: যদি লাইনের শেষে ২ ক্যারেক্টার হেক্স থাকে
                // অনেক সময় Mindray-তে চেকসামের আগে ETB বা ETX এর অবশিষ্টাংশ থাকতে পারে
                if (trimmed.matches(".*[0-9A-F]{2}$")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 2);
                }

                // ৪. অতিরিক্ত স্পেশাল ক্যারেক্টার ক্লিনআপ
                trimmed = trimmed.replaceAll("[^\\x20-\\x7E|\\^]", "");

                if (!trimmed.isEmpty()) {
                    cleanLines.add(trimmed);
                }
            }
        }

        log.info("Successfully parsed {} clean ASTM segments.", cleanLines.size());
        return cleanLines;
    }

    /**
     * পাইপ (|) দিয়ে ফিল্ড আলাদা করা।
     */
    public String[] getFields(String segment) {
        if (segment == null) return new String[0];
        // -1 দেওয়ার কারণ হলো যাতে খালি ফিল্ডগুলোও (Empty strings) অ্যারেতে আসে
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