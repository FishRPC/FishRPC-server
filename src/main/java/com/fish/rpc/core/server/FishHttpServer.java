package com.fish.rpc.core.server;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ThreadFactory;

import com.fish.nacos.FishNacos;
import com.fish.rpc.netty.recv.HttpServerInboundHandler;
import com.fish.rpc.parallel.NamedThreadFactory;
import com.fish.rpc.util.FishRPCConfig;
import com.fish.rpc.util.FishRPCLog;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class FishHttpServer {
	ChannelFuture channel;
	ThreadFactory bossThreadRpcFactory = new NamedThreadFactory("FishHttp-ThreadFactory-boss");
	ThreadFactory workThreadRpcFactory = new NamedThreadFactory("FishHttp-ThreadFactory-work");
	EventLoopGroup boss = new NioEventLoopGroup(FishRPCConfig.PARALLEL, bossThreadRpcFactory);
	EventLoopGroup worker = new NioEventLoopGroup(FishRPCConfig.PARALLEL, workThreadRpcFactory,
			SelectorProvider.provider());

	public void start(final String host,final int port) throws Exception {
		try {

			Runtime.getRuntime().addShutdownHook(new Thread()
		    {
		      @Override
		      public void run() { shutdown(); }
		    });


			ServerBootstrap b = new ServerBootstrap(); // (2)
			b.group(boss, worker).channel(NioServerSocketChannel.class) // (3)
					.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast("codec", new HttpServerCodec());
							ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
							ch.pipeline().addLast("request", new HttpServerInboundHandler());
						}
					})
			 .handler(new LoggingHandler(LogLevel.INFO))
			.option(ChannelOption.SO_BACKLOG, 128) // (5)
			.childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

			channel = b.bind(host, port).sync(); // (7)

			channel.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                		FishRPCLog.info("[FishHttpSever][start][FishRPC监听][%s][%s][成功]",host,port);
                        boolean nacosEnable = FishRPCConfig.getBooleanValue("nacos.enable", false);
                        String nacosServerName = FishRPCConfig.getStringValue("nacos.server.name","fish.rpc.server")+".http";
                        FishNacos.registerNacosService(nacosEnable,nacosServerName, host, port);
                    }
                }
            });

			channel.channel().closeFuture().sync();
		} finally {
			worker.shutdownGracefully();
			boss.shutdownGracefully();
		}
	}

	public void shutdown() {
		worker.shutdownGracefully();
		boss.shutdownGracefully();
		try {
			channel.channel().closeFuture().sync();
		} catch (InterruptedException e) {
		}
	}
}
