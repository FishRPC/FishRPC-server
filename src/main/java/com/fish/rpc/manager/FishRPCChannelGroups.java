package com.fish.rpc.manager;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class FishRPCChannelGroups {
	private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", 
			GlobalEventExecutor.INSTANCE);
	
	public static void add(Channel c){
		CHANNEL_GROUP.add(c);
	}
	
	public static ChannelGroupFuture broadcast(Object msg){
		return CHANNEL_GROUP.writeAndFlush(msg);
	}
	
	public static ChannelGroup flush() {
		return CHANNEL_GROUP.flush();
	}
	public static boolean discard(Channel channel) {
		return CHANNEL_GROUP.remove(channel);
	}
	
	public static boolean contains(Channel channel) {
        return CHANNEL_GROUP.contains(channel);
    }
     
    public static int size() {
        return CHANNEL_GROUP.size();
    }
    
    public static ChannelGroupFuture disconnect(ChannelMatcher matcher) {
        return CHANNEL_GROUP.disconnect(matcher);
    }
}
