package com.fish.rpc.manager;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;

import com.fish.rpc.manager.channel.FishRPCChannelGroups;
import com.fish.rpc.manager.timing.Hello;
import com.fish.rpc.manager.timing.TimingCurrentRankList;
import com.fish.rpc.manager.timing.TimingTotalRankList;
import com.fish.rpc.util.FishRPCConfig;
import com.sun.jdmk.comm.HtmlAdaptorServer;

public class JMXAgentImpl implements JMXAgent{
	
	private static class TimingRankJMXAgentHolder {
		 private static final JMXAgentImpl instance = new JMXAgentImpl();
	}
	
	private JMXAgentImpl(){}
	
	public static JMXAgentImpl getInstance(){
		return TimingRankJMXAgentHolder.instance;
	}
	
	public void server() throws MalformedObjectNameException, InstanceAlreadyExistsException,
		 MBeanRegistrationException, NotCompliantMBeanException, IOException{
		
		 if(!FishRPCConfig.getBooleanValue("fish.rpc.server.jmx", false)){
			System.out.println("JMX was closed.");
			return;
		 }
		 
		
		
		 MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		 ObjectName timingTotalRankJMX = new ObjectName(JMX_DOMAIN+":name=timingTotalRankJMX");
		 mbs.registerMBean(TimingTotalRankList.getInstance(), timingTotalRankJMX);
		 
		 ObjectName timingCurrentRankJMX = new ObjectName(JMX_DOMAIN+":name=timingCurrentRankJMX");
		 mbs.registerMBean(TimingCurrentRankList.getInstance(), timingCurrentRankJMX);
		 
		 ObjectName channelsJMX = new ObjectName(JMX_DOMAIN+":name=channelsJMX");
		 mbs.registerMBean(FishRPCChannelGroups.getInstance(), channelsJMX);
		 
		 ObjectName helloJMX = new ObjectName(JMX_DOMAIN+":name=helloJMX");
		 mbs.registerMBean(Hello.getInstance(), helloJMX);
		
		 
		 String rpcServer = FishRPCConfig.getStringValue("fish.rpc.server", null);
		 if(StringUtils.isEmpty(rpcServer))return;
		 
		 String ip = rpcServer.split(":")[0];
		 String port = "1"+rpcServer.split(":")[1];
		 
		 ObjectName adapterName = new ObjectName(JMX_DOMAIN+":name=htmladapter,port="+port);
	     HtmlAdaptorServer adapter = new HtmlAdaptorServer(Integer.parseInt(port));
	     adapter.start();
	     mbs.registerMBean(adapter,adapterName);

	     System.out.println("Access JMX by http://"+ip+":"+port+"/,CTR+F(jmx.FishRPC.server)");
	}
}
