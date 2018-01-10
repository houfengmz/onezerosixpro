package com.cnc.daq;

import com.cnc.daqnew.DataTransmitThread;
import com.cnc.service.DelMsgServie;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * 开机启动界面
 * 作初始化，加载动态库，开启服务
 * @author wei
 *
 */
public class SplashActivity extends Activity {
	final String TAG="SplashActivity";

	static {  
        // 加载华中动态库  
        System.loadLibrary("hncnet");  
        System.loadLibrary("hzHncAPI");  
        //加载广数动态库
        System.loadLibrary("gsknetw-lib");     
    }
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splashactivity);
		//开启服务
		Intent intentservice=new Intent(SplashActivity.this ,DelMsgServie.class);
		startService(intentservice);
		
		//开机界面延时，之后开启主活动
		new Thread(new Runnable(){
			@Override
			public void run() {	
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {					
					e.printStackTrace();
				}
				//开启主活动	
//				Intent intent=new Intent(SplashActivity.this , DaqActivity.class );
				Intent intent=new Intent(SplashActivity.this , MainActivity.class );
				startActivity(intent);
				SplashActivity.this.finish();
						
			} //end run()
		}).start();
		
	
		
	}
}







