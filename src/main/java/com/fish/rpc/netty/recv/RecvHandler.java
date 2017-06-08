package com.fish.rpc.netty.recv;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RecvHandler  extends SimpleChannelInboundHandler<Object> {
	@Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        FishRPCRequest request = (FishRPCRequest) msg;
      //  System.out.println("RecvHandler recv:"+request);
		System.out.println(request.getRequestId()+",server-read:"+(System.currentTimeMillis()));

        FishRPCResponse response = new FishRPCResponse();
        RecvInitTask recvTask = new RecvInitTask(request, response);
        FishRPCExceutorServer.submit(recvTask, ctx, request, response);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
