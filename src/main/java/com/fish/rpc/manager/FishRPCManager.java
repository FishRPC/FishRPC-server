package com.fish.rpc.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import com.fish.rpc.RPC;
import com.fish.rpc.util.FishRPCConfig;
//import com.fish.rpc.util.Log;
import com.fish.rpc.util.TimeUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;


public class FishRPCManager {
	 
 	private static Map<String,RPCInterface> RPCReference = new HashMap<String,RPCInterface>();
	public static FishRPCManager rpcManager = new FishRPCManager();
	private FishRPCManager(){
		
	}
	public static FishRPCManager getInstance(){
		return rpcManager;
	}
	
	public void initClient() throws Exception{
		FishRPCConfig.initClient(); 
	}
	
	public void initServer() throws Exception{
		FishRPCConfig.initServer();
		for(String packageStr : FishRPCConfig.getStringValue("fish.rpc.scan.packages", "").split(",")){
			init(packageStr);
		}  
		if(FishRPCConfig.onDebug()){
			System.out.println("FishRPC-server regist info:");
		}
        for(RPCInterface aRPCInterface : RPCReference.values()){
        	if(FishRPCConfig.onDebug()){
    			System.out.println(aRPCInterface.toString());
    		}
    	} 
	}
	
	private void init(String packageStr){
		try {
 			ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
			ImmutableSet<ClassPath.ClassInfo> set = classPath.getTopLevelClasses(packageStr);
			
			for(ClassPath.ClassInfo classInfo : set){
				Class<?> clazz = Class.forName(classInfo.getName());
				if(clazz.isInterface())continue;
				RPC rpc = clazz.getAnnotation(RPC.class);
				if(rpc==null)continue;
				Class<?>[] interfaces = clazz.getInterfaces();
				if(interfaces==null || interfaces.length==0)continue;
                String server = FishRPCConfig.getStringValue("fish.rpc.server", "127.0.0.1:5050");
                for(Class<?> interfaceClz : interfaces){
                	RPCInterface rpcInterface = new RPCInterface();
                 	rpcInterface.interfaceName = interfaceClz.getName();
                 	rpcInterface.server=server; 
                 	rpcInterface.impl = Class.forName(clazz.getName(),true, Thread.currentThread().getContextClassLoader()).newInstance();
                  	RPCReference.put(rpcInterface.interfaceName,rpcInterface);
                }
			} 
		} catch (Exception e) { 
			  e.printStackTrace();
		}
	}
	
	public  RPCInterface  getRPCInterface(String interfaceName){
		return RPCReference.get(interfaceName);
	}
	
	public class RPCInterface{
		public String interfaceName;
		public String server;
		public Object impl;
		
		public String toString(){
			return ReflectionToStringBuilder.toString(this);
		} 
	}
}
