package com.cnc.netService;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import android.util.Log;

public class Post {
	private static String encoding = "utf-8";//这个是必须，不然传输的数据会发生乱码
	private final static String tag = "hnctest";
	
	//发送数据到服务器端，需要发送的数据都是提前转化为json格式的字符串，已经提前处理好了
	public static String sendData(String path,String data) throws SocketTimeoutException {
		
		String res=null;
		String upData = "data=" + data;
		byte[] entity = null;
		try {
			entity = upData.toString().getBytes(encoding); //将string转化为字节数组
		} catch (UnsupportedEncodingException e1) {			
			e1.printStackTrace();
		}//生成实体数据
		
		try {
			res = sendPOSTRequest(path, entity);
		} catch (Exception e) {		
			e.printStackTrace();
		}
		return res;
	}
	//发送字节流函数
	private static String sendPOSTRequest(String path, byte[] entity) throws Exception{		
		HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();    //建立http连接
		conn.setConnectTimeout(8000); //超时时间8s
		conn.setReadTimeout(20000);   //设置数据读取超时时间为20s 
		conn.setRequestMethod("POST"); //传输方式post
		conn.setDoOutput(true);		//允许对外输出数据，即http传输中的实际内容
		conn.setDoInput(true);   //允许读取数据
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); //
		conn.setRequestProperty("Content-Length", String.valueOf(entity.length));	//这两个http请求头
		OutputStream outStream = (OutputStream) conn.getOutputStream();//通过输出流可以传输更多的数据
		outStream.write(entity);//写出数据
		outStream.flush();  //add  by  wei
		
		if(conn.getResponseCode() == 200){//只有执行到这一步的时候，才会发送数据流  ； 200表示发送成功
			InputStream is = conn.getInputStream(); //输入流
			String rtnStr = new String(read(is));
			
			if(outStream!=null){
				outStream.close();
			}
			if(conn!=null){
				conn.disconnect(); //断开连接 ，add  by  wei
			}
			
			return rtnStr;
		}
		return null;  //发送失败 ，返回空
	}
	
	
	/**
	 * 读取流中的数据
	 * @param inStream
	 * @return
	 * @throws Exception
	 */
	private static byte[] read(InputStream inStream) throws Exception{
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[512];
		int len = 0;
		while( (len = inStream.read(buffer)) != -1){
			outStream.write(buffer, 0, len);
		}
		inStream.close();
		return outStream.toByteArray();
	}
	
	/**
	 * 测试能否连接服务器，ping测试
	 */
	public static boolean startPing(String ip) {
        boolean res=false;
		try {
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip,null,null);
            int status = p.waitFor();
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer buffer = new StringBuffer();  
            String line = "";
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            if (status == 0) {
                //服务器IP可以正常连接
            	res=true;
            } else {
            	//服务器IP无法正常连接
            }
        } catch (Exception e) {
        	//服务器IP无法正常连接
        }
		return res;
    }

}






