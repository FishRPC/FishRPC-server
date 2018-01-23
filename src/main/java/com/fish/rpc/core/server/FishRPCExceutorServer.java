package com.fish.rpc.core.server;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import com.fish.rpc.netty.recv.RecvChannelInit;
import com.fish.rpc.parallel.NamedThreadFactory;
import com.fish.rpc.util.FishRPCConfig;
import com.fish.rpc.util.FishRPCLog;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;

public class FishRPCExceutorServer {
	
    ThreadFactory bossThreadRpcFactory = new NamedThreadFactory("FishRPC-ThreadFactory-boss");
    ThreadFactory workThreadRpcFactory = new NamedThreadFactory("FishRPC-ThreadFactory-work");
    EventLoopGroup boss = new NioEventLoopGroup(FishRPCConfig.PARALLEL, bossThreadRpcFactory);
    EventLoopGroup worker = new NioEventLoopGroup(FishRPCConfig.PARALLEL, workThreadRpcFactory, SelectorProvider.provider());
    EventLoopGroup local = new LocalEventLoopGroup();
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
                    		FishRPCLog.info("[FishRPCExceutorServer][start][FishRPC监听][%s][%s][成功]",host,port);
                         }
                    }
                }); 
            } else { 
        		FishRPCLog.error("[FishRPCExceutorServer][start][FishRPC监听失败][配置格式错误][fish.rpc.server config][%s]",server);
            }
        } catch (Exception e) { 
            FishRPCLog.error(e, "error=%s", e.getMessage());
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            local.shutdownGracefully();
        }finally{
        	
        }
    }
     
    public <T> void submit(Callable<T> task,final ICallback<T> callback) {
        Future<T> future = local.submit(task);
		try {
			 T t = future.get();
			 Throwable exception = future.cause();
			 if( exception == null ){
		        callback.onSuccess(t); 
		     }else{
		        callback.onFailure(exception);
		     } 
		} catch (InterruptedException | ExecutionException e) {
			 callback.onFailure(e);
		}
    }
    
    public void shutDown() throws InterruptedException{
    	 boss.shutdownGracefully().sync();
    	 FishRPCLog.warn("[FishRPCExceutorServer][shutDown][boss]");
    	 
    	 worker.shutdownGracefully().sync();
         FishRPCLog.warn("[FishRPCExceutorServer][shutDown][worker]");
         
         local.shutdownGracefully().sync();
    	 FishRPCLog.warn("[FishRPCExceutorServer][shutDown][local]");
    }
    
}
