/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.rpc;

import org.zbus.net.http.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.TypeUtils;
 

public class JsonRpcCodec implements RpcCodec {  
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	
	public Message encodeRequest(Request request) {
		Message msg = new Message(); 
		String encoding = request.getEncoding();
		if(encoding == null) encoding = DEFAULT_ENCODING;  
		msg.setBody(toJSONBytes(request, encoding,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteClassName));
		return msg;
	}
	
	public Request decodeRequest(Message msg) {
		String encoding = msg.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		String jsonString = msg.getBodyString(encoding);
		Request req = JSON.parseObject(jsonString, Request.class);
		Request.normalize(req); 
		return req;
	}

	public Object convert(Object param, Class<?> targetType) throws ClassNotFoundException {
		if(targetType.getName().equals("java.lang.Class")){
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			return classLoader.loadClass(param.toString());
		}
		return TypeUtils.castToJavaBean(param, targetType);
	}
	
	public Message encodeResponse(Response response) {
		Message msg = new Message(); 
		msg.setStatus("200"); 
		if(response.getError() != null){
			Throwable error = response.getError(); 
			if(error instanceof IllegalArgumentException){
				msg.setStatus("400");
			} else {
				msg.setStatus("500");
			} 
		}  
		String encoding = response.getEncoding();
		if(encoding == null) encoding = DEFAULT_ENCODING;  
		msg.setBody(toJSONBytes(response, encoding,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteClassName)); 
		return msg; 
	}
	

	public Response decodeResponse(Message msg){ 
		String encoding = msg.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		String jsonString = msg.getBodyString(encoding);
		Response res = null;
		try{
			res = JSON.parseObject(jsonString, Response.class);
		} catch (Exception e){ //probably error can not be instantiated
			res = new Response(); 
			JSONObject json = null;
			try{
				jsonString = jsonString.replace("@type", "unknown-class"); //禁止掉实例化
				json = JSON.parseObject(jsonString); 
			} catch(Exception ex){
				String prefix = "";
				if(msg.isStatus200()){
					prefix = "JSON format invalid: ";
				}
				throw new RpcException(prefix + jsonString);
			} 
			if(json != null){
				final String stackTrace = Response.KEY_STACK_TRACE;
				final String error = Response.KEY_ERROR;
				if(json.containsKey(stackTrace) &&
						json.get(stackTrace) != null){ 
					throw new RpcException(json.getString(stackTrace));
				}
				if(json.containsKey(error) &&
						json.get(error) != null){ 
					throw new RpcException(json.getString(error));
				}
				res.setResult(json.get(Response.KEY_RESULT));
			}
		} 
		return res;
	} 
	
	
	private static final byte[] toJSONBytes(Object object, String charsetName,
			SerializerFeature... features) {
		SerializeWriter out = new SerializeWriter();

		try {
			JSONSerializer serializer = new JSONSerializer(out);
			for (SerializerFeature feature : features) {
				serializer.config(feature, true);
			}

			serializer.write(object);

			return out.toBytes(charsetName);
		} finally {
			out.close();
		}
	}
}
