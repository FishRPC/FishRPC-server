package com.fish.rpc.netty.recv;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.manager.FishRPCChannelGroups;
import com.fish.rpc.util.FishRPCLog;
//import com.fish.rpc.util.Log;
import com.fish.rpc.util.TimeUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RecvHandler  extends ChannelInboundHandlerAdapter {
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FishRPCRequest request = (FishRPCRequest) msg;
        
        FishRPCLog.debug("The request %s,server-read at %s",request.getRequestId(),TimeUtil.currentDateString());
	        
        FishRPCResponse response = new FishRPCResponse();
        RecvInitTask recvTask = new RecvInitTask(request, response);
        FishRPCExceutorServer.getInstance().submit(recvTask, ctx, request, response);
    }

	public void channelActive(ChannelHandlerContext ctx)throws Exception{
		FishRPCChannelGroups.add(ctx.channel());
	}

	public void channelInactive(ChannelHandlerContext ctx)throws Exception{
	    ctx.fireChannelInactive();
	    FishRPCChannelGroups.discard(ctx.channel());
	}
	    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	FishRPCLog.error(cause, cause.getMessage(), "");
    	FishRPCChannelGroups.discard(ctx.channel());
        ctx.close(); 
    }
}
