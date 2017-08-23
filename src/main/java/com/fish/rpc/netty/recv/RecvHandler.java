package com.fish.rpc.netty.recv;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.dto.FishRPCHeartbeat;
import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.manager.channel.FishRPCChannelGroups;
import com.fish.rpc.util.FishRPCLog;
//import com.fish.rpc.util.Log;
import com.fish.rpc.util.TimeUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class RecvHandler  extends ChannelInboundHandlerAdapter {
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if( msg instanceof FishRPCHeartbeat ){
        	FishRPCLog.debug("[RecvHandler][channelRead][心跳][%s]", msg);
        	return;
        }
		final FishRPCRequest request = (FishRPCRequest) msg;
		FishRPCLog.debug("[RecvHandler][channelRead][读取数据：%s][请求ID：%s]",TimeUtil.currentDateString(), request.getRequestId());
        FishRPCResponse response = new FishRPCResponse();
        //在NioEventLoop串行化设计的前提下，是否有必要切换线程[有必要]
        RecvInitTask recvTask = new RecvInitTask(request, response);
        FishRPCExceutorServer.getInstance().submit(recvTask, ctx, request, response);
    }

	public void channelActive(ChannelHandlerContext ctx)throws Exception{
		FishRPCChannelGroups.getInstance().add(ctx.channel());
	}

	public void channelInactive(ChannelHandlerContext ctx)throws Exception{
	    ctx.fireChannelInactive();
	    FishRPCChannelGroups.getInstance().discard(ctx.channel());
	}
	    
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	FishRPCLog.error(cause, cause.getMessage(), "");
    	FishRPCChannelGroups.getInstance().discard(ctx.channel());
        ctx.close(); 
    }
    
    @Override  
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {  
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {  
            IdleState state = ((IdleStateEvent) evt).state();  
            if (state == IdleState.READER_IDLE) {  
                throw new Exception("IDLE事件触发,释放客户端连接："+ctx);  
            }  
        } else {  
            super.userEventTriggered(ctx, evt);  
        }  
    }  
}
