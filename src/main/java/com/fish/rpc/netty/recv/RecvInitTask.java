package com.fish.rpc.netty.recv;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.manager.FishRPCManager;
import com.fish.rpc.manager.RPCInterface;
import com.fish.rpc.util.FishRPCLog;

public class RecvInitTask implements Callable<Boolean>{
	
	private FishRPCRequest request;
	private FishRPCResponse response;
	public RecvInitTask(FishRPCRequest req,FishRPCResponse rsp){
		this.request = req;
		this.response = rsp;
 	}
	@Override
	public Boolean call()  {
		long start = System.currentTimeMillis();
		response.setServerStartBusinessTime(start);
		try{
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
		}
		return Boolean.TRUE;
	}
	
	private Object reflect(FishRPCRequest request) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException  {
        String className = request.getClassName();
        RPCInterface aRPCInterface = FishRPCManager.getInstance().getRPCInterface(className);
        String methodName = request.getMethodName();
        Object[] parameters = request.getParamsVal();
        return MethodUtils.invokeMethod(aRPCInterface.impl, methodName, parameters);
    }

}
