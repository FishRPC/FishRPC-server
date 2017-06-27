package com.fish.rpc.manager.timing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class TimingTotalInfo {
	 
	private String key;
	private int times;
	private long elapsedTotal;
	private final String start = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	 
	public int getTimes() {
		return times;
	}
	public void setTimes(int times) {
		this.times = times;
	}
	public long getElapsedTotal() {
		return elapsedTotal;
	}
	public void setElapsedTotal(long elapsedTotal) {
		this.elapsedTotal = elapsedTotal;
	}
	public String getStart() {
		return start;
	}
	
	public String toString(){
		return "Marked at["+start+"],elapsed total["+elapsedTotal+"]ms,"
				+ "access times["+times+"],avg["+elapsedTotal/times+"],api["+key+"]";
	}
	
}
