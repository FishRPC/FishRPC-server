package com.fish.rpc.manager.timing;

import java.util.List;
import java.util.Map;

public abstract class TimingRankList implements TimingRankListMBean{
	protected int topN = 10;
	
	protected  <T> List<T> top(List<T> list,int topN){
		if(list.size()==0){
			return null;
		}
		int end = list.size()>=topN?(topN):list.size();
		list = list.subList(0, end);
		return list;
	} 
	
	public abstract void add(Timing t);
}
