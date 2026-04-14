package com.hospital.mindraylis.controller;


import com.hospital.mindraylis.drivers.Bs240DataHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @Autowired
    private Bs240DataHandler handler;

    @GetMapping("/machine")
    public Map<String, Object> getMachineStatus() {
        // হ্যান্ডলারে একটি ফ্ল্যাগ রাখতে হবে যা বলবে মেশিন কানেক্টেড কি না
        return Map.of("connected", handler.isMachineConnected());
    }
}