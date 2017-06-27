package com.fish.rpc.netty.recv;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;
import com.fish.rpc.manager.FishRPCManager;
import com.fish.rpc.manager.RPCInterface;
import com.fish.rpc.manager.timing.Timing;
import com.fish.rpc.manager.timing.TimingTask;

public class RecvInitTask implements Callable<Boolean>{
	
	private FishRPCRequest request;
	private FishRPCResponse response;
	public RecvInitTask(FishRPCRequest req,FishRPCResponse rsp){
		this.request = req;
		this.response = rsp;
	}
	@Override
	public Boolean call() throws Exception {
		
		response.setRequestId(request.getRequestId());
		try{
			long start = System.currentTimeMillis();
  			Object result = reflect(request);
			response.setResult(result);
			FishRPCExceutorServer.getInstance().submitSingle(new TimingTask(new Timing(request,response,System.currentTimeMillis() - start)));
			return Boolean.TRUE;
		}catch(Throwable e){
			response.setError(e.getMessage()); 
			e.printStackTrace();
			return Boolean.FALSE;
		}
	}
	
	private Object reflect(FishRPCRequest request) throws Throwable {
        String className = request.getClassName();
        RPCInterface aRPCInterface = FishRPCManager.getInstance().getRPCInterface(className);
        String methodName = request.getMethodName();
        Object[] parameters = request.getParamsVal();
        return MethodUtils.invokeMethod(aRPCInterface.impl, methodName, parameters);
    }

}
