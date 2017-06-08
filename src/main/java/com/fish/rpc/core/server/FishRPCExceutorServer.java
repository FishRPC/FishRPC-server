package com.fish.rpc.core.server;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.netty.recv.RecvChannelInit;
import com.fish.rpc.parallel.FishRPCThreadPool;
import com.fish.rpc.parallel.NamedThreadFactory;
import com.fish.rpc.util.FishRPCConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class FishRPCExceutorServer {
	
    ThreadFactory threadRpcFactory = new NamedThreadFactory("FishRPC-ThreadFactory");
    EventLoopGroup boss = new NioEventLoopGroup();
    EventLoopGroup worker = new NioEventLoopGroup(FishRPCConfig.PARALLEL, threadRpcFactory, SelectorProvider.provider());
    
    private static class FishRPCExceutorServerHolder {
        static final FishRPCExceutorServer instance = new FishRPCExceutorServer();
    }

    public static FishRPCExceutorServer getInstance() {
        return FishRPCExceutorServerHolder.instance;
    }
    
    public void start() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
            		.channel(NioServerSocketChannel.class)
                    .childHandler(new RecvChannelInit())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] ipAddr = FishRPCConfig.getStringValue("fish.rpc.server","127.0.0.1:5050").split(":");

            if (ipAddr.length == 2) {
                final String host = ipAddr[0];
                final int port = Integer.parseInt(ipAddr[1]);
                ChannelFuture future = null;
                future = bootstrap.bind(host, port).sync();
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            System.out.printf("FishRPC Server start success!\nip:%s\nport:%d\nprotocol:protostuff\n\n", host, port);
                        }
                    }
                });
            } else {
                System.out.printf("FishRPC Server start fail!\n");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        worker.shutdownGracefully();
        boss.shutdownGracefully();
    }
    
    private static volatile ListeningExecutorService threadPoolExecutor;
    
    public static void submit(Callable<Boolean> task, final ChannelHandlerContext ctx, final FishRPCRequest request, final FishRPCResponse response) {
        if (threadPoolExecutor == null) {
            synchronized (FishRPCExceutorServer.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = MoreExecutors.listeningDecorator((ThreadPoolExecutor) (FishRPCThreadPool.getExecutor(FishRPCConfig.getIntValue("fish.rpc.server.thread.num", 10), -1)));
                }
            }
        }
        ListenableFuture<Boolean> listenableFuture = threadPoolExecutor.submit(task);
        
        Futures.addCallback(listenableFuture, new FutureCallback<Boolean>() {
            public void onSuccess(Boolean result) {
                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
            			System.out.println(request.getRequestId()+",server-write:"+(System.currentTimeMillis()));
                    }
                });
            }
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, threadPoolExecutor);
    }
    
}
