package com.hospital.mindraylis.config;

import com.hospital.mindraylis.drivers.Bs240DataHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class TcpServerConfig {

    @Value("${lis.mindray.port:8080}")
    private int port;

    @Bean
    public CommandLineRunner serverRunner(Bs240DataHandler handler) {
        return args -> Executors.newSingleThreadExecutor().submit(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new StringDecoder());
                                ch.pipeline().addLast(new StringEncoder());
                                ch.pipeline().addLast(handler);
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                log.info("Starting Mindray BS-240 LIS Server on port {}", port);
                ChannelFuture f = b.bind(port).sync();
                log.info("Server is now live and waiting for analyzer connection...");
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Critical error in TCP Server: {}", e.getMessage(), e);
            } finally {
                log.warn("Shutting down LIS TCP Server...");
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
    }
}