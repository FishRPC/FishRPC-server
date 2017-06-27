package com.fish.rpc.manager.channel;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class FishRPCChannelGroups implements FishRPCChannelGroupsMBean{
	
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", 
			GlobalEventExecutor.INSTANCE);
	
	private static final Map<Channel,Long> channelTime = Maps.newConcurrentMap();
	
	
	private static class FishRPCChannelGroupsHolder {
		 private static final FishRPCChannelGroups instance = new FishRPCChannelGroups();
	}
	
	public static FishRPCChannelGroups getInstance(){
		return FishRPCChannelGroupsHolder.instance;
	}
	
	private FishRPCChannelGroups(){}
	
	
	public  void add(Channel c){
		CHANNEL_GROUP.add(c);
		channelTime.put(c, System.currentTimeMillis());
	}
	
	public  ChannelGroupFuture broadcast(Object msg){
		return CHANNEL_GROUP.writeAndFlush(msg);
	}
	
	public  ChannelGroup flush() {
		return CHANNEL_GROUP.flush();
	}
	public  boolean discard(Channel channel) {
		if(channel.isActive()){
			channel.disconnect();
		}
		return CHANNEL_GROUP.remove(channel);
	}
	
	public  boolean contains(Channel channel) {
        return CHANNEL_GROUP.contains(channel);
    }
     
    public  int size() {
        return CHANNEL_GROUP.size();
    }
    
    @Override
    public String info(){
    	String line = System.getProperty("line.separator", "\n");
    	Iterator<Channel> iterator = CHANNEL_GROUP.iterator();
    	StringBuffer sb = new StringBuffer("Connections:").append(size()).append(line);
    	while(iterator.hasNext()){
    		Channel channel = iterator.next();
    		long connectMills = channelTime.get(channel);
    		SocketAddress localAddr = channel.localAddress();
    		SocketAddress remoteAddr = channel.remoteAddress();
    		sb.append("From:").append(remoteAddr)
    		.append(" connected on ").append(localAddr)
    		.append(" keep alived ").append(System.currentTimeMillis() - connectMills).append(" ms")
    		.append(line);
    	}
    	return sb.toString();
    }
    
    public  void print(){
    	System.out.println(info());
    }
    
    public static ChannelGroupFuture disconnect(ChannelMatcher matcher) {
        return CHANNEL_GROUP.disconnect(matcher);
    }

	@Override
	public String infoHtml() {
		String line = System.getProperty("line.separator", "\n");
		return info().replaceAll(line, "</br>");
	}
}
