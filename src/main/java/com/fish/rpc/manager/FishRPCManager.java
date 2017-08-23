package com.fish.rpc.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fish.rpc.RPC;
import com.fish.rpc.util.FishRPCConfig;
import com.fish.rpc.util.FishRPCLog;
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
	
	/**
	 * 默认采用当前运行路径下的fishRPC-client.properties
	 * @param configPath
	 * @throws Exception
	 */
	public void initClient(String configPath) throws Exception{
		FishRPCConfig.initClient(configPath); 
	}
	/**
	 * 默认采用当前运行路径下的fishRPC-server.properties
	 * @param configPath
	 * @throws Exception
	 */
	public void initServer(String configPath) throws Exception{
		FishRPCConfig.initServer(configPath);
		String rpcPackages = FishRPCConfig.getStringValue("fish.rpc.scan.packages", "");
		if(!StringUtils.isEmpty(rpcPackages)){
			for(String packageStr : FishRPCConfig.getStringValue("fish.rpc.scan.packages", "").split(",")){
				init(packageStr);
			}
		}else{
			FishRPCLog.warn("[FishRPCManager][initServer][没有需要注册的RPC服务][fish.rpc.scan.packages : %s]", rpcPackages);
			return ;
		}
        for(RPCInterface aRPCInterface : RPCReference.values()){
        	FishRPCLog.info("[FishRPCManager][initServer][注册RPC服务信息][%s]",aRPCInterface);
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
	
	public Map<String,RPCInterface> getRPCReferences(){
		return RPCReference;
	}
}
