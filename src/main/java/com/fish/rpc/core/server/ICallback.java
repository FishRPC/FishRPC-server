package com.fish.rpc.core.server;

public interface ICallback<T> {

	public void onSuccess(T t);
	public void onFailure(Throwable t); 
}
