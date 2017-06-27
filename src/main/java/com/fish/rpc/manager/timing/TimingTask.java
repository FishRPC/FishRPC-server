package com.fish.rpc.manager.timing;

import java.util.concurrent.Callable;

import com.fish.rpc.util.FishRPCConfig;

public class TimingTask  implements Callable<Boolean>  {

	private Timing timing;
	
	public TimingTask(Timing timing){
		this.timing = timing;
	}
	@Override
	public Boolean call() throws Exception {
		if(!FishRPCConfig.getBooleanValue("fish.rpc.server.jmx", false)){
			return Boolean.TRUE;
		}
		TimingCurrentRankList.getInstance().add(timing);
		TimingTotalRankList.getInstance().add(timing);
		return Boolean.TRUE;
	} 
}
