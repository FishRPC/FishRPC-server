package com.fish.rpc.manager.timing;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fish.rpc.dto.FishRPCRequest;
import com.fish.rpc.dto.FishRPCResponse;

public class Timing {
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private FishRPCRequest request;
	private FishRPCResponse response;
	private long timing;
	private final String start = df.format(new Date());

	
	public Timing(FishRPCRequest request,FishRPCResponse response,long timing){
		this.request = request;
		this.response = response;
		this.timing = timing;
	}
	public FishRPCRequest getRequest() {
		return request;
	}
	public void setRequest(FishRPCRequest request) {
		this.request = request;
	}
	public FishRPCResponse getResponse() {
		return response;
	}
	public void setResponse(FishRPCResponse response) {
		this.response = response;
	}
	public long getTiming() {
		return timing;
	}
	public void setTiming(long timing) {
		this.timing = timing;
	}
	
	public String toString() {
	  String className =request.getClassName();
	  String method = request.getMethodName();
	  String params = StringUtils.join(request.getParamsVal(), ",");
	  String all = className+"."+method+"("+params+")";
	  String requestTime = df.format(new Date(request.getClientAwaitAtTime()));
	  String responseTime = df.format(new Date(request.getClientSignalAtTime()));
	  return "Marked at ["+start+"],elapsed["+timing+"]ms on ["+all+"],"
	  		+ "request id["+request.getRequestId()+"],resquest time["+requestTime+"],response time["+responseTime+"]";
	}
		
	
}
