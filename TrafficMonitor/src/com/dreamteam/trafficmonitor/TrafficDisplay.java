package com.dreamteam.trafficmonitor;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamteam.trafficmonitor.customdialog.MaskImage;
import com.dreamteam.trafficmonitor.customdialog.Shaker;
import com.dreamteam.trafficmonitor.customdialog.Shaker.OnShakeListener;
import com.dreamteam.trafficmonitor.db.MySQLiteOpenHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class TrafficDisplay extends Activity {

	private TextView company;
	private TextView usedTraffic;
	private TextView traffic;
	private TextView suggestion;
	private ImageButton setting;
	MaskImage maskImage;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trafficdisplay);

		MySQLiteOpenHelper dbhelper = new MySQLiteOpenHelper(this);
		SQLiteDatabase db = dbhelper.getWritableDatabase();
		
		long sum=0;
		float usedTrafficText=0;

				
		//��ȡ���ײ͡��������������ٷֱ�
		SharedPreferences userInfo = getSharedPreferences("traffic", 
				Context.MODE_PRIVATE);
		// getString()�ڶ�������Ϊȱʡֵ�����preference�в����ڸ�key��������ȱʡֵ
		String companyText = userInfo.getString("company", "δ֪");
		float monthPlan = userInfo.getInt("MonthPlanValue", 50);
		
		
		//����һ�����³����Ͱ�����
		int monthFromFile = userInfo.getInt("Month", 1);
		Date date=new Date();
		//�����Ǽ���
		int day=date.getDate();
		Calendar c=Calendar.getInstance();
		c.setTime(date);
		int month = c.get(Calendar.MONTH)+1;
		if (monthFromFile != month){
			//ʵ����SharedPreferences.Editor����
			SharedPreferences.Editor editor = userInfo.edit(); 
			//��putString�ķ����������� 
			editor.putInt("Month", month); 
			editor.putInt("PackagePlusValue", 0); 
			//�ύ��ǰ���� 
			editor.commit(); 
		}
		
		//��ȡ�ֶ�����У�����·�
		String changedDate = userInfo.getString("Date", "2000-13-32 25:61:61");
		int changedMonth =  Integer.parseInt(String.valueOf(changedDate.charAt(5)) 
				+ String.valueOf(changedDate.charAt(6)));
		//���ݱ����Ƿ��ֶ�У������ȡ��������
		if (month!=changedMonth){
			String sql = "select GPRS from TrafficInfo where time>=datetime('now', 'start of month')";
			Cursor cursor = db.rawQuery(sql, null);	
	        // ������ƶ�����һ�У��Ӷ��жϸý�����Ƿ�����һ�����ݣ�������򷵻�true��û���򷵻�false  
	        while (cursor.moveToNext()) {
	        	sum += cursor.getLong(cursor.getColumnIndex("GPRS"));
	        	Log.i("trafficDisplay", sum + "M");
	        }  
			usedTrafficText = ((float)sum)/1024/1024;
		} else { 
			String sql = "select GPRS from TrafficInfo where time>=datetime('"+ changedDate +"')";
			Cursor cursor = db.rawQuery(sql, null);			
	        // ������ƶ�����һ�У��Ӷ��жϸý�����Ƿ�����һ�����ݣ�������򷵻�true��û���򷵻�false  
	        while (cursor.moveToNext()) {
	        	sum += cursor.getLong(cursor.getColumnIndex("GPRS")); 
	        	Log.i("trafficDisplay", sum + "M");
	        }  
			usedTrafficText = ((float)sum)/1024/1024;	
			usedTrafficText += userInfo.getFloat("UsedTrafficValue", 0);
		}
		
		//ʵ����SharedPreferences.Editor����
		SharedPreferences.Editor editor = userInfo.edit(); 
		//��putString�ķ����������� 
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");      
        Date curDate = new Date(System.currentTimeMillis());//��ȡ��ǰʱ��    
        String currentTime = formatter.format(curDate);     
		userInfo.edit().putString("Date", currentTime).commit();
		editor.putFloat("UsedTrafficValue", usedTrafficText); 
		//�ύ��ǰ���� 
		editor.commit(); 
		
		//�����ܿ�������Ϊ���ײͼ��ϼ��Ͱ�
		float trafficText = monthPlan + userInfo.getInt("PackagePlusValue", 0);
		
		//�����ϼ���������ʾ����
		DecimalFormat df = new DecimalFormat("0.00"); 
		company = (TextView) findViewById(R.id.company);
		company.setText(companyText);
		usedTraffic = (TextView) findViewById(R.id.usedTraffic);
		usedTraffic.setText(df.format(usedTrafficText) + "");
		traffic = (TextView) findViewById(R.id.traffic);
		traffic.setText(df.format(trafficText) + "");
		
		float usedPercentage = (usedTrafficText / trafficText) ;
				
		maskImage = (MaskImage) findViewById(R.id.imageView2);
		maskImage.setUsedPercentage(usedPercentage);
		maskImage.draw();

		//��ת������ҳ��İ�ť
		setting = (ImageButton) findViewById(R.id.setting);
		setting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(TrafficDisplay.this, Setting.class);
				startActivity(intent);
			}
		});
		
		
		//���²����������ײ��Ƽ�
		//�����Ǽ���
		int curDay = c.get(Calendar.DATE);
		//��ǰ�µ����һ���Ǽ���
		int lastDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);
		//Ԥ�Ȿ�½������ĵ�����
		float forecast = usedTrafficText/curDay*lastDay;
		suggestion = (TextView) findViewById(R.id.suggestion);
		
		if (forecast < monthPlan){
			suggestion.setText("�����������㣬�����ʹ��");
		} else if (forecast < monthPlan+20){
			suggestion.setText("��������ʹ�ýϿ죬�뿼��ʹ���������Ͱ�");
		} else {
			// ����SQLiteDatabase�����query�������в�ѯ������һ��Cursor���������ݿ��ѯ���صĽ��������  
	        // ��һ������String������  
	        // �ڶ�������String[]:Ҫ��ѯ������  
	        // ����������String����ѯ����  
	        // ���ĸ�����String[]����ѯ�����Ĳ���  
	        // ���������String:�Բ�ѯ�Ľ�����з���  
	        // ����������String���Է���Ľ����������  
	        // ���߸�����String���Բ�ѯ�Ľ����������  
			int minPlan=999999999;
			int temp=999999999;
	        Cursor planCursor = db.query("Plans", new String[] { "planTraffic" }, "planTraffic>=?",
	        		new String[] { forecast+"" },
	        		null, null, null);  
	        // ������ƶ�����һ�У��Ӷ��жϸý�����Ƿ�����һ�����ݣ�������򷵻�true��û���򷵻�false  
	        while (planCursor.moveToNext()) {
	        	temp = minPlan;
	        	minPlan = planCursor.getInt(planCursor.getColumnIndex("planTraffic"));
	        	if (temp<minPlan)
	        		minPlan = temp;
	        }  
	        
	        String planName="";
	        planCursor = db.query("Plans", new String[] { "planName" }, "planTraffic=?",
	        		new String[] { minPlan+"" },
	        		null, null, null);  
	        // ������ƶ�����һ�У��Ӷ��жϸý�����Ƿ�����һ�����ݣ�������򷵻�true��û���򷵻�false  
	        while (planCursor.moveToNext()) {
	        	planName = planCursor.getString(planCursor.getColumnIndex("planName"));
	        }
			suggestion.setText("ÿ���ײ��������㣬�Ƽ�����Ϊ" + planName + "�ײ�");
		}
		
		db.close();
//		/*Shaker sensorHelper = new Shaker(this);  
//	    sensorHelper.setOnShakeListener(new OnShakeListener() {  
//	          
//	        @Override  
//	        public void onShake() {  
//	            // TODO Auto-generated method stub  
//	            System.out.println("shake");  
//	            RotateAnimation animation = new RotateAnimation(0, -15, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//				animation.setDuration(500);
//				maskImage.startAnimation(animation);
//				
//				Timer timer = new Timer();
//				TimerTask task = new TimerTask() {
//					
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//						RotateAnimation animation = new RotateAnimation(-15, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//						animation.setDuration(500);
//						
//						maskImage.startAnimation(animation);
//						
//						Timer timer = new Timer();
//						TimerTask task = new TimerTask() {
//							
//							@Override
//							public void run() {
//								// TODO Auto-generated method stub
//								RotateAnimation animation = new RotateAnimation(0, 15, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//								animation.setDuration(500);
//								
//								maskImage.startAnimation(animation);
//								
//								Timer timer = new Timer();
//								TimerTask task = new TimerTask() {
//									
//									@Override
//									public void run() {
//										// TODO Auto-generated method stub
//										RotateAnimation animation = new RotateAnimation(15, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//										animation.setDuration(500);
//										
//										maskImage.startAnimation(animation);
//										
//										
//									}
//								};
//								
//								timer.schedule(task, 500);
//								
//								
//							}
//						};
//						
//						timer.schedule(task, 500);
//						
//						
//					}
//				};
//				
//				timer.schedule(task, 500);  
//	        }  
//	    });*/
	}
}
