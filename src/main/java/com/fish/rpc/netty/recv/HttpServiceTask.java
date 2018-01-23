package com.fish.rpc.netty.recv;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.reflect.MethodUtils;

import com.fish.rpc.manager.FishRPCManager;
import com.fish.rpc.manager.RPCInterface;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.internal.StringUtil;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

public class HttpServiceTask implements Callable<Boolean>{
	private ChannelHandlerContext ctx;
	private FullHttpRequest request;
	public HttpServiceTask(ChannelHandlerContext _ctx,FullHttpRequest _request){
		this.ctx = _ctx;
		this.request = _request;
	}
	@Override
	public Boolean call() throws Exception {
		Object serviceInstance = getServiceInstance(request);
		if(serviceInstance == null){
			NOT_FOUND(ctx); 
		}
		if (request.getMethod() == HttpMethod.GET) {
			doGet(ctx,request); 
		} else if (request.getMethod() == HttpMethod.POST) {
			doPost(ctx,request); 
		} else {
			NOT_ACCEPTABLE(ctx); 
		} 
		return Boolean.TRUE;
	}
	
	private void INTERNAL_SERVER_ERROR(ChannelHandlerContext ctx,String msg) throws UnsupportedEncodingException{
		byte[] content = ("[500 INTERNAL_SERVER_ERROR]"+msg).getBytes("UTF-8");
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.INTERNAL_SERVER_ERROR,Unpooled.copiedBuffer(content));
    	response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
              "text/plain");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,content.length);
        ctx.writeAndFlush(response); 
	}
	private void NOT_FOUND(ChannelHandlerContext ctx) throws UnsupportedEncodingException{
		byte[] content = "[404 NOT FOUND]".getBytes("UTF-8");
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.NOT_FOUND,Unpooled.copiedBuffer(content));
    	response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
              "text/plain");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,content.length);
        ctx.writeAndFlush(response); 
	}
	private void NOT_ACCEPTABLE(ChannelHandlerContext ctx) throws UnsupportedEncodingException{
		byte[] content = "[406 NOT ACCEPTABLE]".getBytes("UTF-8");
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.NOT_ACCEPTABLE,Unpooled.copiedBuffer(content));
    	response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
              "text/plain");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,content.length);
        ctx.writeAndFlush(response); 
	}
	private void OK(ChannelHandlerContext ctx,Object result) throws UnsupportedEncodingException{
		String resutlJson = "{}";
		if(result != null){
			resutlJson = new Gson().toJson(result);
		} 
		byte[] content = resutlJson.getBytes("UTF-8");
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK,Unpooled.copiedBuffer(content));
    	response.headers().set(HttpHeaders.Names.CONTENT_TYPE,
              "application/json");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH,content.length);
        ctx.writeAndFlush(response); 
	}
	
	private void doGet(ChannelHandlerContext ctx, FullHttpRequest request) throws UnsupportedEncodingException, NotFoundException, CannotCompileException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
		Map<String,String> httpParams = getUrlParams(request);
		Object instance = getServiceInstance(request);
		String method = getServiceMethod(request);
		Object[] reflectParams = combineParamsVals(httpParams,instance.getClass().getName(),method);
		Object result = MethodUtils.invokeMethod(instance,method, reflectParams);
		OK(ctx,result);
	}
	private void doPost(ChannelHandlerContext ctx, FullHttpRequest request) throws UnsupportedEncodingException, NotFoundException, CannotCompileException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
		String contentType = request.headers().get(HttpHeaders.Names.CONTENT_TYPE);
		ByteBuf directContentBuf = request.content();
		String content = "";
		if (!directContentBuf.hasArray()) { 
			int len = directContentBuf.readableBytes();
			byte[] array = new byte[len];
			directContentBuf.getBytes(directContentBuf.readerIndex(), array);
			content = new String(array, "UTF-8");
		} 
		if(content.contains("form-data")){
			INTERNAL_SERVER_ERROR(ctx,"不支持表单提交");
			return;
		}
		Map<String,String> httpParams = new HashMap<String, String>();
		if(contentType.equals("application/x-www-form-urlencoded")){
			httpParams = splitUrlParam(content);
		}else if(contentType.equals("application/json")){
			JsonObject json = new JsonParser().parse(content).getAsJsonObject();
			Set<Entry<String, JsonElement>> set = json.entrySet();
			Iterator<Entry<String, JsonElement>> it = set.iterator();  
			while (it.hasNext()) {  
			  Entry<String, JsonElement> entity = it.next();
			  String key = entity.getKey();  
			  JsonElement element = entity.getValue();
			  httpParams.put(key, element.toString());
			}  
		}
		
		Object instance = getServiceInstance(request);
		String method = getServiceMethod(request);
		Object[] reflectParams = combineParamsVals(httpParams,instance.getClass().getName(),method);
		Object result = MethodUtils.invokeMethod(instance,method, reflectParams);
		OK(ctx,result);
	}
	
	private Map<String,String> getUrlParams(FullHttpRequest request) throws UnsupportedEncodingException{
		String uri = request.getUri();
		String params = uri.split("\\?").length == 2 ? uri.split("\\?")[1] : "";
		return splitUrlParam(params);
	} 
	
	private Map<String,String> splitUrlParam(String params) throws UnsupportedEncodingException{
		if(StringUtil.isNullOrEmpty(params)){
			return new HashMap<String, String>();
		}
		Map<String, String> keyValue = new HashMap<String, String>();
		String[] paramArray = params.split("&"); 
		if (paramArray.length > 0) {
			for (String param : paramArray) {
				String[] keyValues = param.split("=");
				if (keyValues.length == 2) {
					keyValue.put(keyValues[0], URLDecoder.decode(keyValues[1], "utf-8"));
				}else if (keyValues.length == 1) {
					keyValue.put(keyValues[0], null);
				}
			}
		}
		return keyValue;
	}

	
	/**
	 * 反射获取方法参数与请求参数的
	 * @param keyValue
	 * @param className
	 * @param methodName
	 * @return
	 * @throws NotFoundException
	 * @throws UnsupportedEncodingException 
	 * @throws CannotCompileException 
	 */
	private Object[] combineParamsVals(Map<String, String> keyValue, String className, String methodName)
			throws NotFoundException, CannotCompileException {
		
		ClassPool pool = ClassPool.getDefault();
		CtMethod cm = pool.getMethod(className, methodName);
		MethodInfo methodInfo = cm.getMethodInfo();
		
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attribute = (LocalVariableAttribute) codeAttribute
				.getAttribute(LocalVariableAttribute.tag);
		
		int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;

		Object[] parameters = null;
		CtClass[] paramTypes = cm.getParameterTypes();
		
		if (paramTypes != null && paramTypes.length > 0) {
			parameters = new Object[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++) {
				String key = attribute.variableName(i + pos);
				CtClass type = paramTypes[i];
				String primitiveParamValue = keyValue.get(key);
				if (type instanceof CtPrimitiveType) {
					CtPrimitiveType primitiveType = (CtPrimitiveType) type;
					String wrapName = primitiveType.getWrapperName(); 
					//设置原始类型初始值
					if(StringUtil.isNullOrEmpty(primitiveParamValue) && !wrapName.equals("java.lang.Boolean")){
						primitiveParamValue = "0";
					}
					if(StringUtil.isNullOrEmpty(primitiveParamValue) && wrapName.equals("java.lang.Boolean")){
						primitiveParamValue = "false";
					}
					if (wrapName.equals("java.lang.Boolean")) {
						parameters[i] = Boolean.valueOf(primitiveParamValue);
					} else if (wrapName.equals("java.lang.Character")) {
						parameters[i] = new Character(primitiveParamValue.charAt(0));
					} else if (wrapName.equals("java.lang.Byte")) {
						parameters[i] = Byte.valueOf(primitiveParamValue);					 
					} else if (wrapName.equals("java.lang.Short")) {
						parameters[i] = Short.valueOf(primitiveParamValue);		
					} else if (wrapName.equals("java.lang.Integer")) {
						parameters[i] = Integer.valueOf(primitiveParamValue);	
					} else if (wrapName.equals("java.lang.Long")) {
						parameters[i] = Long.valueOf(primitiveParamValue);
					} else if (wrapName.equals("java.lang.Double")) {
						parameters[i] = Double.valueOf(primitiveParamValue);
					} else if (wrapName.equals("java.lang.Float")) {
						parameters[i] = Float.valueOf(primitiveParamValue);
					} else {
						parameters[i] = null;
					}
				} else if(StringUtil.isNullOrEmpty(primitiveParamValue)) {
					parameters[i] = null;
				} else{
					Object o = null; 
					if (CtClassPool.classPool.containsKey(type)) {
						o = new Gson().fromJson(primitiveParamValue, CtClassPool.classPool.get(type));
					} else {
						Class<?> clz = type.toClass();
						o = new Gson().fromJson(primitiveParamValue, clz);
						CtClassPool.classPool.put(type, clz);
					}
					parameters[i] = o;
				}
			}
		}
		return parameters;
	}
	/**
	 * 获取服务实例
	 * @param request
	 * @return
	 */
	private Object getServiceInstance(FullHttpRequest request){
		String uri = request.getUri();
		String tmpUri = uri.substring(0, uri.indexOf("?") == -1 ? uri.length() : uri.indexOf("?"));
		int classStartIdx = tmpUri.lastIndexOf("/"); 
		int classEndIdx = tmpUri.indexOf("."); 
		String className = tmpUri.substring(classStartIdx + 1, classEndIdx); 
		RPCInterface aRPCInterface = FishRPCManager.getInstance().getRPCInterface(className); 
		if(aRPCInterface==null){
			return null;
		}
		return aRPCInterface.impl;
	}
	/**
	 * 获取服务方法
	 * @param request
	 * @return
	 */
	private String getServiceMethod(FullHttpRequest request){
		String uri = request.getUri();
		String tmpUri = uri.substring(0, uri.indexOf("?") == -1 ? uri.length() : uri.indexOf("?"));
 		int classEndIdx = tmpUri.indexOf("."); 
		String methodName = tmpUri.substring(classEndIdx + 1, tmpUri.length());
		return methodName;
	}

}
