package com.hospital.mindraylis.config;

import com.hospital.mindraylis.drivers.Bs240DataHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TcpServerConfig {

    @Value("${lis.mindray.port:8080}")
    private int port;

    // গ্রুপগুলোকে ক্লাসের মেম্বার হিসেবে রাখা ভালো যাতে অ্যাপ্লিকেশন বন্ধ হওয়ার সময় এগুলো শাটডাউন করা যায়
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Bean
    public CommandLineRunner serverRunner(Bs240DataHandler handler) {
        return args -> {
            // আলাদা থ্রেড তৈরি করে সার্ভার স্টার্ট করা যাতে মেইন থ্রেড ব্লক না হয়
            Thread serverThread = new Thread(() -> {
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) {
                                    ChannelPipeline p = ch.pipeline();
                                    // Decoder এবং Encoder সঠিকভাবে সেট করা
                                    p.addLast(new StringDecoder());
                                    p.addLast(new StringEncoder());
                                    p.addLast(handler);
                                }
                            })
                            // TCP কনফিগারেশন যা কানেকশন স্ট্যাবিলিটি বাড়ায়
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            .childOption(ChannelOption.TCP_NODELAY, true); // ডেটা ট্রান্সমিশনে ল্যাটেন্সি কমাবে

                    log.info(">>> Initializing Mindray BS-240 LIS TCP Server on Port: {}", port);

                    ChannelFuture f = b.bind(port).sync();
                    log.info(">>> LIS Server is ONLINE and listening for analyzer...");

                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    log.error("TCP Server Interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Failed to start LIS Server: {}", e.getMessage());
                } finally {
                    stop(); // রিসোর্স ক্লিনআপ
                }
            });

            serverThread.setName("NettyServerThread");
            serverThread.start();
        };
    }

    /**
     * Spring Application বন্ধ হওয়ার সময় Netty এর রিসোর্সগুলো রিলিজ করা নিশ্চিত করে
     */
    @PreDestroy
    public void stop() {
        log.warn("Stopping LIS TCP Server and releasing event loops...");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}