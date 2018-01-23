package com.fish.rpc.netty.recv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CtClass;

public class CtClassPool {
	
	public static Map<CtClass,Class<?>> classPool = new ConcurrentHashMap <CtClass,Class<?>>();
	
}
