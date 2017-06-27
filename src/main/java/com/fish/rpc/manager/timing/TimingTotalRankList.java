package com.fish.rpc.manager.timing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fish.rpc.util.FishRPCLog;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Monitor;
/**
 * 实时RPC服务反射执行方法总耗时排名
 * top50
 * @author fish
 *
 */
public class TimingTotalRankList extends TimingRankList{

	private static final int maxRecords = 1000;
	
	private static  Map<String,TimingTotalInfo> totalInfoMap = Maps.newHashMap();
	
	public static List<TimingTotalInfo> totalRankList = Collections.synchronizedList(new ArrayList<TimingTotalInfo>());
	
	private static class TimingTotalRankListHolder {
		 private static final TimingTotalRankList instance = new TimingTotalRankList();
	}
	
	public static TimingTotalRankList getInstance(){
		return TimingTotalRankListHolder.instance;
	}
	
	private TimingTotalRankList(){}
	
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
			String key = t.getRequest().getClassName()+"."+t.getRequest().getMethodName()+"("+StringUtils.join(t.getRequest().getParamsType(),",")+")";
			if(totalInfoMap.containsKey(key)){
				TimingTotalInfo totalInfo = totalInfoMap.get(key);
				totalInfo.setTimes(totalInfo.getTimes()+1);
				totalInfo.setElapsedTotal(totalInfo.getElapsedTotal()+t.getTiming());
				totalInfoMap.put(key, totalInfo);
			}else if(totalInfoMap.size() <= maxRecords){
				TimingTotalInfo totalInfo = new TimingTotalInfo();
				totalInfo.setKey(key);
				totalInfo.setTimes(1);
				totalInfo.setElapsedTotal(t.getTiming());
				totalInfoMap.put(key, totalInfo);
			}else{
				FishRPCLog.debug("total rank api over max %s ,will give up it %s.", maxRecords,key);
				return;
			}
			totalRankList = Collections.list(Collections.enumeration(totalInfoMap.values()));
			Ordering<TimingTotalInfo> byOrdering = Ordering.from(new Comparator<TimingTotalInfo>(){
				@Override
				public int compare(TimingTotalInfo t1, TimingTotalInfo t2) {
					return Longs.compare(t2.getElapsedTotal(),t1.getElapsedTotal());
				} 
			});
			Collections.sort(totalRankList,byOrdering);
			totalRankList = top(totalRankList,topN);
		}catch(Exception e){
			FishRPCLog.error(e, "", "");
		}finally{
			canPrint = true;
		}
	} 
	
	
	public List<TimingTotalInfo> get(){
		try{
			monitor.enterWhen(print); 
		}catch(Exception e){
			FishRPCLog.error(e, "", "");
		}finally{ 
			monitor.leave();
		}
		return totalRankList;
	}
	
	@Override
	public String print(){
		try{
			monitor.enterWhen(print);  
			String line = System.getProperty("line.separator", "\n");
			StringBuffer sb = new StringBuffer("Total elasped rank TOP"+topN+":").append(line);
			for(TimingTotalInfo t:totalRankList){
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
	public String printHtml() {
		String line = System.getProperty("line.separator", "\n");
		return print().replaceAll(line, "</br>");
	} 
	
	@Override
	public void setTopN(int topN) {
		super.topN = topN;
	}

	@Override
	public String lockInfo() {
		return "canPrint="+canPrint;
	}
}
