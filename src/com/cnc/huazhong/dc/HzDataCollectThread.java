package com.cnc.huazhong.dc;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;


import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.cnc.daq.DaqData;
import com.cnc.daq.MainActivity;
import com.cnc.domain.DataAlarm;
import com.cnc.domain.DataLog;
import com.cnc.domain.DataReg;
import com.cnc.domain.DataRun;

import com.cnc.domain.UiDataAlarmRun;
import com.cnc.domain.UiDataNo;
import com.cnc.huazhong.HncAPI;
import com.cnc.huazhong.HncSystem;
import com.cnc.mainservice.DelMsgService;
import com.cnc.net.datasend.HandleMsgTypeMcro;
import com.cnc.net.service.Intialize;
import com.cnc.utils.AlarmFilterList;


import com.cnc.utils.SaveRunTime;
import com.cnc.utils.TimeUtil;
/**
 * 华中数据采集子线程
 * @author wei
 *
 */
public class HzDataCollectThread implements Runnable,CommonDataCollectThreadInterface{
	
	private final String TAG="DataCollectThread...";
	 //线程循环执行标志，改为false时，线程退出循环，线程结束运行
    private volatile  boolean  threadflag=true; 
	public int Client = -1;	   //连接状态标识， -1标识未连接

	boolean boolGetMacInfo = false; //标识是否得到机床的基本信息
	int   macChannel = 0;//机床的通道信息
	int   count = 1;//存储运行信息的id,标识这是第几次采集信息
	
//	private Handler  daqActivityHandler=null,
	private Handler  delMsgHandler =null;
	private Handler  mainHander=null ;
					
	private final String tp = "HNC-818A";//数控系统型号
	String machine_SN=null;//数控系统ID

//	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");//时间戳格式
	private AlarmFilterList   alarmFilterList =null; //报警信息缓存过滤对象
	
	/*static {  
        // 加载动态库  
        System.loadLibrary("hncnet");  
        System.loadLibrary("hzHncAPI");  
    }*/

	final String machineIP; //="192.168.188.113"; //机床的IP地址
	final int    machinePort ;//= 21;			  //机床端口号
	
	public HzDataCollectThread(String ip){
		this(ip,21);
	}
	
	public HzDataCollectThread(String ip,int port){
		this.delMsgHandler=DelMsgService.getHandlerService();
		this.mainHander=MainActivity.getMainActivityHandler();
		machineIP=ip;
		machinePort=port;
		this.alarmFilterList=new AlarmFilterList(delMsgHandler);
	}
	
		
	@Override
	public void run() {
		int inialRes = -1;//是否已经初始化    inialRes != 0
		long  starttime=0; //线程开启时间	
		starttime=Calendar.getInstance().getTimeInMillis(); //记录下线程开启的时间
		
		while(threadflag){
			
			if(inialRes != 0){
				//首先初始化,只能初始化一次；对于不同的机床IP初始化不同的端口号
				inialRes=Intialize.getInstance().inial6(getInitLocalport(machineIP));
			}else{ //初始化成功
				if(HncAPI.HNCNetIsConnect(Client) != 0)
				{
					Client=HncAPI.HNCNetConnect(machineIP, machinePort);
					//采集数据之前初始化，得到目的机器的IP和port
					if (Client >= 0 ){
						sendMsg2Main("华中连接机床成功", HandleMsgTypeMcro.MSG_ISUCCESS);//初始化成功
					}else{
						sendMsg2Main("华中连接机床失败", HandleMsgTypeMcro.MSG_IFAILURE);
						try {							
							Thread.sleep(1000*20); //连接机床失败过一分钟再连							
						} catch (InterruptedException e) {					
							e.printStackTrace();
					    } 
					}					
				}else{				
					daq();//调用数据采集函数采集数据，同时把采集到的数据发送到主线程
				}			
			}

			//采集数据间隔时间,采集结束之后线程休眠一段时间
			try {
//				Thread.sleep(700);//采集数据间隔时间设置为1S,因为采集过程耗时大约300毫秒，所以设置为700
				Thread.sleep(1000);
			}catch (InterruptedException e) {					
				e.printStackTrace();
			} 
			
		}//end while(true)	
		
		HncAPI.HNCNetExit();//退出线程断开连接，重新连接需要重新初始化
		//计算运行时长
		starttime= Calendar.getInstance().getTimeInMillis() - starttime;
		SaveRunTime.saveOnTime(machineIP, starttime /1000); //以秒为单位保存,使用IP地址作为主键
		
	}//end run()
	
	
	/**
	 * 数据采集函数
	 */
	private void daq() 
	{	//开始采集信息的各种事件		
		String strTime = TimeUtil.getTimestamp();
		
		if(!boolGetMacInfo)   //如果没有获得过机床的基本信息
		{
			//注册信息
			DataReg dataReg = HncTools.getMacInfo(Client); //获取机床的基本信息			
			//机床ID
			machine_SN=dataReg.getId();
			if(machine_SN==null){
				Log.d(TAG,"没有读到华中机床的ID");
				return ;  //如果没有读到机床的ID则不进行任何接下来的采集工作
			}
//			DaqData.setCncid(machine_SN);
			//得到通道号
			macChannel = HncAPI.HNCSystemGetValueInt(HncSystem.HNC_SYS_ACTIVE_CHAN, Client);//机床的通道信息
			dataReg.setTime(strTime);		//设置采集的时间戳
			dataReg.setTp(tp);   			//数控系统型号
			/*synchronized(RegLock.class){   //加锁同步，同一时刻只能有一个线程修改list中的数据
				DaqData.getListDataReg().add(dataReg);
			}*/
			DaqData.saveDataReg(dataReg);
			long ontime=SaveRunTime.getOnTime(machineIP);//累计运行时长			
			DataLog dataLog=new DataLog(machine_SN,
					ontime,
					ontime,
					strTime);//华中数控不提供“累计加工时间”和“累计运行时间”
		/*	synchronized(LogLock.class){  //加锁同步
				DaqData.getListDataLog().add(dataLog);
			}*/
			DaqData.saveDataLog(dataLog);
			
			UiDataNo uiDataNo=new UiDataNo("","",machine_SN , DaqData.getAndroidId());
			sendMsg(mainHander,uiDataNo,HandleMsgTypeMcro.HUAZHONG_UINO,0,0); //发送消息到活动，显示IP地址信息
					
			boolGetMacInfo = true;//置为true，表明已经得到了机床基本信息
		}
		else//采集运行信息和报警信息
		{	
			StringBuilder  sbalram=new StringBuilder();
			//采集报警信息
			LinkedList<DataAlarm> listDataAlarm = HncTools.getAlarmData(Client);
			for (DataAlarm dataAlarm : listDataAlarm) {
				dataAlarm.setId(machine_SN);//设置数控系统ID
				dataAlarm.setTime(strTime);//设置采集的时间戳
				sbalram.append(dataAlarm.getCtt()).append(":");
			}
			//如果采集到的报警信息不为零或者已有的报警信息不为零，那么就要对报警信息进行分析
			//对报警信息进行处理,必须要判断报警信息的来到是发生报警还是解除报警，这个分析过程留到主线程中
			if ((!listDataAlarm.isEmpty())|| (!alarmFilterList.getNowAlarmList().isEmpty())) 
			{
//				sendMsg2Main(listDataAlarm, HandleMsgTypeMcro.MSG_ALRAM, count);
				alarmFilterList.saveCollectedAlarmList(listDataAlarm);
			}		
			
			//采集运行信息
			DataRun dataRun = HncTools.getDataRun(Client,macChannel);
			dataRun.setId(machine_SN); //设置数控系统ID
			dataRun.setTime(strTime); //加时间戳
			sendMsg2Main(dataRun,HandleMsgTypeMcro.MSG_RUN,count);
					
			count++;//采集次数记录
			if(count == Integer.MAX_VALUE)//达到最大值的时候记得清零
				count = 1;
			
			//发送到主线程
			UiDataAlarmRun uiDataAlarmRun=new UiDataAlarmRun(sbalram.toString(), dataRun.toString());
			sendMsg(mainHander, uiDataAlarmRun, HandleMsgTypeMcro.HUAZHONG_UIALARM	, 0, 0);			
		}
		
	}//end daq()函数
	
	/**
	 * 获取properties文件中值
	 * @param key
	 * @return
	 */
	/*private String  getPath(String key){
		InputStream in=null;
		Properties pro=null;
		String path=null;
		
		pro =new Properties();
		in=DataTransmitThread.class.getResourceAsStream(DataType.pathResource);
				
		try {
			pro.load(new InputStreamReader(in,"utf-8"));
			
			path=pro.getProperty(key);
			
		} catch (UnsupportedEncodingException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}finally{			
			if(in!=null){
				try {
					in.close();
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}						
		}
		return path;
	}*/
	
	
	//发送消息到主线程
	private void sendMsg2Main(Object obj, int what) 
	{
		sendMsg(delMsgHandler, obj, what,0,0);
	}
	
	//发送消息到主线程
	private void sendMsg2Main(Object obj, int what, int arg1) 
	{
		sendMsg(delMsgHandler, obj, what, arg1, 0);
	}

	//发送消息，通用型
	private void sendMsg(Handler handler,Object obj, int what, int arg1, int arg2) 
	{
		Message msg = Message.obtain();			
		msg.what = what;
		msg.obj = obj;
		msg.arg1 = arg1;
		msg.arg2 = arg2;
		handler.sendMessage(msg);
	}

	@Override
	public void stopCollect() {
		this.threadflag=false;	//结束线程
		
	}

	@Override
	public boolean isThreadRunning() {		
		  return threadflag;
	}
	
	//根据IP地址获取不同初始化端口号
	//实现采集机床的切换
	private int getInitLocalport(String ip){
		if(ip==null || "".equals(ip.trim())){
			return 10015;
		}		
		int port=10015;
		String  num= ip.substring(ip.lastIndexOf('.')+1);
		int n = Integer.valueOf(num);	
		return n+9905;
	}


}
