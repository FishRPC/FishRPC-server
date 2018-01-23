package com.fish.rpc.netty.recv;

import java.io.UnsupportedEncodingException;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.core.server.ICallback;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 支持GET POST请求
 * POST支持：content-type=application/json,application/x-www-form-urlencoded; 不支持表单提交
 * 
 * @author fish
 *
 */
public class HttpServerInboundHandler extends ChannelInboundHandlerAdapter{
	
	@Override  
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			FullHttpRequest request = (FullHttpRequest)msg;
			FishRPCExceutorServer.getInstance().submit(new HttpServiceTask(ctx,request),new ICallback<Boolean>(){
				@Override
				public void onSuccess(Boolean t) { }
				@Override
				public void onFailure(Throwable t) { }
			} );
		} else {
			super.channelRead(ctx, msg);
		}
	}  
  
    @Override  
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {   
        ctx.flush();  
    }  
    @Override  
    public void exceptionCaught(ChannelHandlerContext ctx,
            Throwable cause) throws Exception{
    	cause.printStackTrace();
    	INTERNAL_SERVER_ERROR(ctx,cause.getMessage());
    }
    
    private void INTERNAL_SERVER_ERROR(ChannelHandlerContext ctx,String msg) throws UnsupportedEncodingException{
		byte[] content = ("[500 INTERNAL_SERVER_ERROR]"+msg).getBytes("UTF-8");
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.INTERNAL_SERVER_ERROR,Unpooled.copiedBuffer(content));
    	response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
              "text/plain");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,content.length);
        ctx.writeAndFlush(response); 
	}
    
     

}
