package com.hospital.mindraylis.drivers;

import com.hospital.mindraylis.core.AstmConstants;
import com.hospital.mindraylis.service.ResultService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ChannelHandler.Sharable
public class Bs240DataHandler extends SimpleChannelInboundHandler<String> {

    @Autowired
    private ResultService resultService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final AttributeKey<StringBuilder> BUFFER = AttributeKey.valueOf("buffer");

    // মেশিনের কানেকশন স্ট্যাটাস ট্র্যাক করার জন্য (AtomicBoolean থ্রেড-সেফ)
    @Getter
    private final AtomicBoolean machineConnected = new AtomicBoolean(false);

    /**
     * বর্তমান স্ট্যাটাস সরাসরি পাওয়ার জন্য মেথড (API Controller থেকে কল করার জন্য)
     */
    public boolean isMachineConnected() {
        return machineConnected.get();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Analyzer Connected: {}", ctx.channel().remoteAddress());
        ctx.channel().attr(BUFFER).set(new StringBuilder());

        // স্ট্যাটাস আপডেট করুন
        machineConnected.set(true);

        // UI-তে কানেকশন স্ট্যাটাস ব্রডকাস্ট করুন
        messagingTemplate.convertAndSend("/topic/connection", "CONNECTED");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("Analyzer Disconnected: {}", ctx.channel().remoteAddress());

        // স্ট্যাটাস আপডেট করুন
        machineConnected.set(false);

        // UI-তে ডিসকানেকশন স্ট্যাটাস ব্রডকাস্ট করুন
        messagingTemplate.convertAndSend("/topic/connection", "DISCONNECTED");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        StringBuilder sb = ctx.channel().attr(BUFFER).get();
        if (sb == null) return;

        for (char c : msg.toCharArray()) {
            if (c == AstmConstants.ENQ) {
                // ১. মেশিন সেশন শুরু করতে চাইলে ACK দিন
                ctx.writeAndFlush(String.valueOf(AstmConstants.ACK));
                log.info("Handshake: Received ENQ, sent ACK");

            } else if (c == AstmConstants.EOT) {
                // ২. মেশিন সেশন শেষ করলে ডেটা প্রসেস করুন
                // এখানে ACK পাঠাবেন না (ASTM Standard অনুযায়ী EOT এর ACK হয় না)
                log.info("Session End: Received EOT. Processing data...");
                if (sb.length() > 0) {
                    resultService.processAndSave(sb.toString());
                    sb.setLength(0); // বাফার ক্লিয়ার করুন
                }

            } else if (c == AstmConstants.STX) {
                // ৩. ফ্রেম শুরু হলে বাফার ক্লিয়ার করে নতুন ফ্রেম নিন (ঐচ্ছিক কিন্তু সেফ)
                // sb.setLength(0);
            } else if (c == AstmConstants.ETX || c == AstmConstants.ETB) {
                // ৪. ফ্রেম শেষ হলে ACK দিন যাতে মেশিন পরের ফ্রেম পাঠায়
                ctx.writeAndFlush(String.valueOf(AstmConstants.ACK));
            } else if (c != AstmConstants.STX && c != '\n' && c != '\r') {
                // ৫. আসল ডেটা বাফারে যোগ করুন
                sb.append(c);
            }

            // Mindray-তে অনেক সময় প্রতি লাইনের শেষে \r (CR) থাকে।
            // যদি আপনার মেশিন প্রতি লাইনের পর ACK চায়, তবে নিচের ব্লকটি ব্যবহার করুন:
            if (c == AstmConstants.CR) {
                ctx.writeAndFlush(String.valueOf(AstmConstants.ACK));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Network error with analyzer {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        machineConnected.set(false); // এক্সেপশন হলে স্ট্যাটাস অফলাইন করে দিন
        ctx.close();
    }
}