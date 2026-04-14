package com.hospital.mindraylis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
@Slf4j
public class MindrayLisApplication {
    @Value("${server.port}")
    private String port;

    public static void main(String[] args) {
        new SpringApplicationBuilder(MindrayLisApplication.class)
                .bannerMode(Banner.Mode.CONSOLE)
                .headless(false)
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String dynamicUrl = "http://localhost:" + port + "/";

        log.info("\n------------------------------------------------");
        log.info("✅ LIS Server is ready!");
        log.info("🔗 Dashboard URL: " + dynamicUrl);
        log.info("------------------------------------------------\n");

        launchBrowser(dynamicUrl);
    }

    private void launchBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                }
            }
        } catch (Exception e) {
            MindrayLisApplication.log.error("❌ Dashboard auto-launch failed : " + e.getMessage());
        }
    }
}
