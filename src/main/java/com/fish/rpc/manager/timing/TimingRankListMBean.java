package com.fish.rpc.manager.timing;

public interface TimingRankListMBean {

	public String print();
	public String printHtml();
	public void setTopN(int topN);
	public String lockInfo();
	
}
