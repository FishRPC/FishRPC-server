package com.fish.rpc.netty.recv;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.manager.FishRPCManager;
import com.fish.rpc.manager.RPCInterface;
import com.fish.rpc.manager.timing.Timing;
import com.fish.rpc.manager.timing.TimingTask;
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
		response.setRequestId(request.getRequestId());
		try{
  			Object result = reflect(request); 
			response.setResult(result);
			response.setCode(0);
			FishRPCLog.debug("the request [%s] , the response [%s]", request,response);
			
		}catch(NoSuchMethodException e){
			response.setCode(-1);
			response.setError(e.getMessage()); 
			response.setResult(e);
			e.printStackTrace(); 
		}catch(IllegalAccessException e){
			response.setCode(-1);
			response.setError(e.getMessage()); 
			response.setResult(e);
			e.printStackTrace(); 
		}catch(InvocationTargetException e){
			InvocationTargetException target = (InvocationTargetException)e;
			response.setCode(-1);
			response.setError(target.getMessage()); 
			response.setResult(target.getTargetException());
			e.printStackTrace(); 
		} 
		FishRPCExceutorServer.getInstance().submitSingle(new TimingTask(new Timing(request,response,System.currentTimeMillis() - start)));
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
