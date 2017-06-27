package com.fish.rpc.manager.timing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fish.rpc.util.FishRPCLog;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Monitor;
/**
 * 实时RPC服务反射执行方法耗时统计排名
 * 单线程
 * top50
 * @author fish
 *
 */
public class TimingCurrentRankList extends TimingRankList{
	
	public static  List<Timing> timingCurrentRankList = Collections.synchronizedList(new ArrayList<Timing>());
	
	private static class TimingCurrentRankListHolder {
		 private static final TimingCurrentRankList instance = new TimingCurrentRankList();
	}
	
	public static TimingCurrentRankList getInstance(){
		return TimingCurrentRankListHolder.instance;
	}
	
	private TimingCurrentRankList(){}
	
	private boolean canPrint=true; 
	
	private Monitor monitor = new Monitor();
	private Monitor.Guard print = new Monitor.Guard(monitor) {
        @Override
        public boolean isSatisfied() {
            return canPrint;
        }
    }; 
	public void add(Timing t){
		try{
			canPrint = false;
			timingCurrentRankList.add(t);
			
			Ordering<Timing> byOrdering = Ordering.from(new Comparator<Timing>(){
				@Override
				public int compare(Timing t1, Timing t2) {
					return Longs.compare(t2.getTiming(),t1.getTiming());
				} 
			}); 
			Collections.sort(timingCurrentRankList,byOrdering);
			timingCurrentRankList = top(timingCurrentRankList,topN);
		}catch(Exception e){
			FishRPCLog.error(e, "", "");
		}finally{
			canPrint = true; 
		}
	} 
	
	public List<Timing> get(){ 
		try{
			monitor.enterWhen(print); 
		}catch(Exception e){
			FishRPCLog.error(e, "", "");
		}finally{ 
			monitor.leave();
		}
		return timingCurrentRankList;
	}
	
	@Override
	public String print(){
		try{
			monitor.enterWhen(print);
			String line = System.getProperty("line.separator", "\n");
			StringBuffer sb = new StringBuffer("Current elapsed TOP"+topN+":").append(line);
			for(Timing t:timingCurrentRankList){
				sb.append(t.toString()).append(line);
			}
			System.out.println(sb.toString());
			return sb.toString();
		}catch(Exception e){
			FishRPCLog.error(e, "", "");
		}finally{
			monitor.leave();
		}  
		return null;
	}

	
	@Override
	public void setTopN(int topN) {
		super.topN = topN;
	}

	@Override
	public String printHtml() {
		String line = System.getProperty("line.separator", "\n");
		return print().replaceAll(line, "</br>");
	} 
	
	@Override
	public String lockInfo() {
		return "canPrint="+canPrint;
	}
	 
}
