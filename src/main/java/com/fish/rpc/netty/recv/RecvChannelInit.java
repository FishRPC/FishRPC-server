package com.fish.rpc.netty.recv;

import java.util.concurrent.TimeUnit;

import com.fish.rpc.serialize.kryo.KryoDecoder;
import com.fish.rpc.serialize.kryo.KryoEncoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class RecvChannelInit extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		pipeline.addLast(new KryoEncoder());
        pipeline.addLast(new KryoDecoder());
        pipeline.addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
		/*pipeline.addLast(new ProtostuffEncoder());
	    pipeline.addLast(new ProtostuffDecoder(false));
		*/
        pipeline.addLast(new RecvHandler());
	}
	
}
