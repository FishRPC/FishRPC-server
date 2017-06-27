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
import com.fish.rpc.util.FishRPCLog;
//import com.fish.rpc.util.Log;
import com.fish.rpc.util.TimeUtil;
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

            String server = FishRPCConfig.getStringValue("fish.rpc.server","127.0.0.1:5050");
            String[] ipAddr = server.split(":");
            if (ipAddr.length == 2) {
                final String host = ipAddr[0];
                final int port = Integer.parseInt(ipAddr[1]);
                ChannelFuture future = null;
                future = bootstrap.bind(host, port).sync();
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                    		FishRPCLog.debug("FishRPC server start success  on host %s port %s",host,port);
                        	System.out.println("FishRPC server start success !");
                        }
                    }
                }); 
            } else { 
   			 	FishRPCLog.debug("FishRPC server start fail.fish.rpc.server config wrong %s",server);
            }
        } catch (Exception e) {
            e.printStackTrace();
            FishRPCLog.error(e, "error=%s", e.getMessage());
            
        }finally{
        	
        }
    }

    public void stop() {
        worker.shutdownGracefully();
        boss.shutdownGracefully();
    }
    
    private volatile ListeningExecutorService threadPoolExecutor;
    private volatile ListeningExecutorService singleThreadPoolExecutor;
    
    public  void submit(Callable<Boolean> task, final ChannelHandlerContext ctx, final FishRPCRequest request, final FishRPCResponse response) {
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
            	FishRPCLog.debug("The request %s,server-replay-start at %s",request.getRequestId(),TimeUtil.currentDateString());
            	ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    	FishRPCLog.debug("The request %s,server-replay-end at %s",request.getRequestId(),TimeUtil.currentDateString());
                    }
                });
            }
            public void onFailure(Throwable t) {
            	  t.printStackTrace();
            }
        }, threadPoolExecutor);
    }
    
    /**
     * single thread doing 
     * @param task
     */
    public void submitSingle(Callable<Boolean> task){
    	if (singleThreadPoolExecutor == null) {
            synchronized (FishRPCExceutorServer.class) {
                if (singleThreadPoolExecutor == null) {
                	singleThreadPoolExecutor = MoreExecutors.listeningDecorator((ThreadPoolExecutor) (FishRPCThreadPool.getExecutor(1, -1)));
                }
            }
        }
    	singleThreadPoolExecutor.submit(task);
    }
    
}
