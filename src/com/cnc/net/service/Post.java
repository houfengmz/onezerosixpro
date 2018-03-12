package com.cnc.net.service;

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

/**
 * �������ݷ���
 * @author wei
 *
 */
public class Post {
	private static String encoding = "utf-8";//����Ǳ��룬��Ȼ��������ݻᷢ������
	private final static String tag = "hnctest";
	
	//�������ݵ��������ˣ���Ҫ���͵����ݶ�����ǰת��Ϊjson��ʽ���ַ������Ѿ���ǰ��������
	public static String sendData(String path,String data) throws SocketTimeoutException {
		
		String res=null;
		String upData = "data=" + data;
		byte[] entity = null;
		try {
			entity = upData.toString().getBytes(encoding); //��stringת��Ϊ�ֽ�����
		} catch (UnsupportedEncodingException e1) {			
			e1.printStackTrace();
		}//����ʵ������
		
		try {
			res = sendPOSTRequest(path, entity);
		} catch (Exception e) {		
			e.printStackTrace();
		}
		return res;
	}
	//�����ֽ�������
	private static String sendPOSTRequest(String path, byte[] entity) throws Exception{		
		HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();    //����http����
		conn.setConnectTimeout(3000); //��ʱʱ��2s
		conn.setReadTimeout(5000);   //�������ݶ�ȡ��ʱʱ��Ϊ5s 
		conn.setRequestMethod("POST"); //���䷽ʽpost
		conn.setDoOutput(true);		//��������������ݣ���http�����е�ʵ������
		conn.setDoInput(true);   //������ȡ����		
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); //
		conn.setRequestProperty("Content-Length", String.valueOf(entity.length));	//������http����ͷ
		OutputStream outStream = (OutputStream) conn.getOutputStream();//ͨ����������Դ�����������
		outStream.write(entity);//д������
		outStream.flush();  //add  by  wei
		
		if(conn.getResponseCode() == 200){//ֻ��ִ�е���һ����ʱ�򣬲Żᷢ��������  �� 200��ʾ���ͳɹ�
			InputStream is = conn.getInputStream(); //������
			String rtnStr = new String(read(is));
			
			if(outStream!=null){
				outStream.close();
			}
			if(conn!=null){
				conn.disconnect(); //�Ͽ����� ��add  by  wei
			}
			
			return rtnStr;
		}
		return null;  //����ʧ�� �����ؿ�
	}
	
	
	/**
	 * ��ȡ���е�����
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
	 * �����ܷ����ӷ�������ping����
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
                //������IP������������
            	res=true;
            } else {
            	//������IP�޷���������
            }
        } catch (Exception e) {
        	//������IP�޷���������
        }
		return res;
    }
	
	
	/**
	 * ���㷢������
	 * @param time, ms
	 * @param len, Byte
	 * @return
	 */
	 public static String speed(long time,long len){		 
		 String result="";
		 if(time>0){
			 long s=len*1000/time;
			 result=s+"B/s";
			 if(s>1024){
				 s=s/1024;
				 result=s+"KB/s";
			 }
			 if(s>1024){
				 s=s/1024;
				 result=s+"MB/s";
			 }
			 if(s>1024){
				 s=s/1024;
				 result=s+"GB/s";
			 }			 
			 String size=len+"B";
			 if(len>1024){
				 len=len/1024;
				 size=len+"KB";
			 }			 
			 if(len>1024){
				 len=len/1024;
				 size=len+"MB";
			 }			 
			 String t=time+"ms";
			 if(time>1000){
				 time=time/1000;
				 t=time+"sec";
				 if(time>60){
					 time=time/60;
					 t=time+"min";
				 }
			 }
			 result=result+"("+size+","+t+")";	 
		 }
		 return result;
	 }

}





