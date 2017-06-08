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

/**
 * 
 * @author fish
 * FishRPC启动入口
 * FishRPC基于Netty4.0开发
 */
public class FishRPC {
    public static void start() {
    	try{ 
    		System.out.println("");
    		System.out.println("▄▄▄▄▄      ●●●●  ");
    		System.out.println("▌          ▋         ●  ●        ▌      ▌");
    		System.out.println("▌▄▄  ▋  ●  ");
    		System.out.println("▌     ▋   ▋▅▅▅▅▅▌");
    		System.out.println("▌     ▋        ●  ▌      ▌");
    		System.out.println("▌     ▋        ●  ▌      ▌");
    		System.out.println("▌     ▋   ●●●●●   ▌      ▌");
    		System.out.println("");
    		System.out.println("");
    		System.out.println("FishRPC 1.0 Base Netty4.0 Build 2017/06/07 高考  Author：Fish");
    		System.out.println("");
    		System.out.println("");
	    	FishRPCManager.getInstance().initServer();
	    	FishRPCExceutorServer.getInstance().start();
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    		e.printStackTrace();
    	}
    }
}

