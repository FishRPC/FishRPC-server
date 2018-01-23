package com.fish.rpc.netty.recv;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.core.server.ICallback;
import com.fish.rpc.dto.FishRPCHeartbeat;
import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.manager.FishRPCManager;
import com.fish.rpc.manager.RPCInterface;
import com.fish.rpc.manager.channel.FishRPCChannelGroups;
import com.fish.rpc.util.FishRPCLog; 
import com.fish.rpc.util.TimeUtil;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@Sharable//与状态无关
public class RecvHandler extends ChannelDuplexHandler {
	@Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if( msg instanceof FishRPCHeartbeat ){
        	FishRPCLog.debug("[RecvHandler][channelRead][心跳][%s]", msg);
        	return;
        }
        //接收请求
		final FishRPCRequest request = (FishRPCRequest) msg;
		FishRPCLog.debug("[RecvHandler][channelRead][读取数据：%s][请求ID：%s]",TimeUtil.currentDateString(), request.getRequestId());
        
		//构造响应
		/*final FishRPCResponse response = new FishRPCResponse();
		response.setRequestId(request.getRequestId());
		response.setServerReceiveAtTime(System.currentTimeMillis());*/
		doTask(request,ctx);
		//提交线程池处理
        /*RecvInitTask recvTask = new RecvInitTask(request,response);
        FishRPCExceutorServer.getInstance().submit(recvTask,new ICallback<Boolean>(){
			@Override
			public void onSuccess(Boolean t) {
            	FishRPCLog.debug("[RecvHandler][channelRead][onSuccess][服务器执行成功][开始发送数据：%s][请求ID：%s]",TimeUtil.currentDateString(),request.getRequestId());
            	response.setServerDoneSendDataTime(System.currentTimeMillis());
            	ctx.writeAndFlush(response);
			}
			@Override
			public void onFailure(Throwable t) {
            	FishRPCLog.debug("[RecvHandler][channelRead][onFailure][服务器执行失败][开始发送数据：%s][请求ID：%s][error:%s]",TimeUtil.currentDateString(),request.getRequestId(),t.getMessage());
            	response.setServerDoneSendDataTime(System.currentTimeMillis());
            	response.setResult(new Exception("服务器异常："+t.getMessage())+",请求ID："+request.getRequestId());
            	ctx.writeAndFlush(response);
			}
        });*/
    }
	@Override
	public void channelActive(ChannelHandlerContext ctx)throws Exception{
		FishRPCChannelGroups.getInstance().add(ctx.channel());
		ctx.fireChannelActive();
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx)throws Exception{
	    FishRPCChannelGroups.getInstance().discard(ctx.channel());
	    ctx.fireChannelInactive();
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
    private void doTask(FishRPCRequest request,ChannelHandlerContext ctx){
		final FishRPCResponse response = new FishRPCResponse();
		long start = System.currentTimeMillis();
		try{
			response.setRequestId(request.getRequestId());
			response.setServerReceiveAtTime(System.currentTimeMillis());
			response.setServerStartBusinessTime(start);
  			Object result = reflect(request);
			response.setResult(result);
			response.setCode(0);
			FishRPCLog.info("[RecvInitTask][call][服务端执行结果]\n[请求：%s]\n[响应：%s]",request,response);
		}catch(NoSuchMethodException e){
			response.setCode(-1);
			response.setError(e.getMessage()); 
			response.setResult(e);
			FishRPCLog.error(e,"[RecvInitTask][call][服务端执行异常][Exception : %s]\n[请求：%s]\n[响应：%s]",e.getMessage(),request,response);
		}catch(IllegalAccessException e){
			response.setCode(-1);
			response.setError(e.getMessage()); 
			response.setResult(e);
			FishRPCLog.error(e,"[RecvInitTask][call][服务端执行异常][Exception : %s]\n[请求：%s]\n[响应：%s]",e.getMessage(),request,response);
		}catch(InvocationTargetException e){
			InvocationTargetException target = (InvocationTargetException)e;
			response.setCode(-1);
			response.setError(target.getMessage()); 
			response.setResult(target.getTargetException());
			FishRPCLog.error(e,"[RecvInitTask][call][服务端执行异常][Exception : %s]\n[请求：%s]\n[响应：%s]",e.getMessage(),request,response);
		}finally{
			response.setServerDoneBusinessTime(System.currentTimeMillis());
			FishRPCLog.debug("[RecvHandler][channelRead][onSuccess][服务器执行完成][耗时：%s ms][开始发送数据：%s][请求ID：%s]",(System.currentTimeMillis() - start),TimeUtil.currentDateString(),request.getRequestId());
        	response.setServerDoneSendDataTime(System.currentTimeMillis());
        	ctx.writeAndFlush(response);
		} 
    }
    private Object reflect(FishRPCRequest request) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException  {
        String className = request.getClassName();
        RPCInterface aRPCInterface = FishRPCManager.getInstance().getRPCInterface(className);
        String methodName = request.getMethodName();
        Object[] parameters = request.getParamsVal();
        return MethodUtils.invokeMethod(aRPCInterface.impl, methodName, parameters);
    }
}
