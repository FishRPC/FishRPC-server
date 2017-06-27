package com.fish.rpc.manager.timing;

public class Hello implements HelloMBean{

	private static class HelloHolder {
		 private static final Hello instance = new Hello();
	}
	
	
	public static Hello getInstance(){
		return HelloHolder.instance;
	}
	
	private Hello(){}
	
	
	@Override
	public String sayHello() {
		return "Hello word !";
	}

}
