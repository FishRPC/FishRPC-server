/**
 * Copyright (C) 2017 Fish Group Holding Limited
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fish.rpc.boot;

import com.fish.rpc.core.server.FishRPCExceutorServer;
import com.fish.rpc.manager.FishRPCManager;
import com.fish.rpc.manager.JMXAgentImpl;
import com.fish.rpc.util.FishRPCLog;

import io.netty.util.internal.StringUtil;

/**
 * 
 * @author fish
 * FishRPC启动入口
 * FishRPC基于Netty4.0开发
 */
public class FishRPC {
	private static boolean shutdownHookEnabled = false;
	public static void start(String configPath) {
    	try{  
			FishRPCLog.info("[FishRPC][start] [v1.0] [Based on Netty4.0] [Build 2017/06/07 高考  Author：Fish]\n");
    		if( !StringUtil.isNullOrEmpty(configPath) ){
    			FishRPCLog.info("[FishRPC][start][config:%s]",configPath);
    		}
	    	FishRPCManager.getInstance().initServer(configPath);
	    	FishRPCExceutorServer.getInstance().start();
	    	//JMXAgentImpl.getInstance().server();
    	}catch(Exception e){
    		 e.printStackTrace();
    	}finally{
    		exit();
    	}
    } 
    public static void start(String configPath,ClassLoader cl) {
    	try{ 
    		Thread.currentThread().setContextClassLoader(cl);
    		start(configPath);
    	}catch(Exception e){
    		 e.printStackTrace();
    	}
    } 
    
    private static void exit(){
    	 if (!shutdownHookEnabled) {
             shutdownHookEnabled = true; 
             try {
                 Runtime.getRuntime().addShutdownHook(new Thread() {
                     public void run() {
                         try {
                        	 FishRPCLog.warn("FishRPC 服务进程正准备关闭，处理未完成的任务中，千万别kill -9 pid ");
                        	 FishRPCExceutorServer.getInstance().shutDown();
                        	 FishRPCLog.warn("FishRPC 服务进程正常关闭");
                         } catch (Exception e) {
                        	 FishRPCLog.error(e, "FishRPC addShutdownHook run  error");
                         }
                     }
                 });
             } catch (Exception e) {
            	 FishRPCLog.error(e, "FishRPC addShutdownHook error");
             }
         }
    }
     
}

