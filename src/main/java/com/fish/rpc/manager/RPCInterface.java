package com.fish.rpc.manager;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class RPCInterface {

	public String interfaceName;
	public String server;
	public Object impl;
	
	public String toString(){
		return ReflectionToStringBuilder.toString(this);
	} 

}
