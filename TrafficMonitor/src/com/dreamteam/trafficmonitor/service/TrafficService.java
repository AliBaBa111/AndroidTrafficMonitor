package com.dreamteam.trafficmonitor.service;

import com.dreamteam.trafficmonitor.R;
import com.dreamteam.trafficmonitor.Setting;
import com.dreamteam.trafficmonitor.customdialog.TrafficInfo;
import com.dreamteam.trafficmonitor.db.MySQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;


/**
 * @author Ryan Mo 
 * �ж���2g/3g����wifi�����ø�����W��Ȼ���жϣ�
 * �����ʼ����wifi״̬���ͻ�ȡ�Ǹ�uid����T��Ȼ��ֵ��W��ֱ���л�״̬Ϊֹ��
 * ����е�2g/3g�ͻ�ȡ�����ǲ������Ǹ�����������T��T-WΪ3g��������ΪG��
 * ���һֱû�л�������Wһֱδ�䣻�˺�����л���״̬������T�����£�-G=W�����£���
 */

public class TrafficService extends Service {

	private TrafficReceiver tReceiver;
	private WifiManager wifiManager;
	private ConnectivityManager cManager;
	private PackageManager pm;
	List<TrafficInfo> trafficInfosOrigin = new ArrayList<TrafficInfo>();
	List<TrafficInfo> trafficInfosWifi;
	List<TrafficInfo> trafficInfosGprs;
	MySQLiteOpenHelper dbhelper = new MySQLiteOpenHelper(this);
	private boolean isWIFI;
	private boolean isGPRS;

	@Override
	public IBinder onBind(Intent intent){
		return null;
	}

	@Override
	public void onCreate() {
		// ��ʼ��
		trafficInfosOrigin = getTrafficInfos();
		SharedPreferences userInfo = getSharedPreferences("traffic",
				MODE_PRIVATE);
		if (userInfo.getInt("MonthPlanValue", -1) == -1) {
			// ��Ϣ֪ͨ��
			// ����NotificationManager
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
			// ����֪ͨ��չ�ֵ�������Ϣ
			int icon = R.drawable.logosmall;
			CharSequence tickerText = "��������ײ�δ�趨(������')";
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);

			// ��������֪ͨ��ʱҪչ�ֵ�������Ϣ
			Context context = getApplicationContext();
			CharSequence contentTitle = "��������ײ�δ�趨(������')";
			CharSequence contentText = "����ȥ���ð�~(��o������)��o������";
			Intent notificationIntent = new Intent(this, Setting.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					notificationIntent, 0);
			notification.setLatestEventInfo(context, contentTitle, contentText,
					contentIntent);

			// ��mNotificationManager��notify����֪ͨ�û����ɱ�������Ϣ֪ͨ
			mNotificationManager.notify(1, notification);
		}
		// WifiManager,ConnectivityManager
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		cManager = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		// ע��TrafficReceiver
		tReceiver = new TrafficReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(tReceiver, filter);
		System.out.print("Service Start");
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return super.onStartCommand(intent, flags, startId);
	}
    /**
     * @author Ryan Mo
     * @description ����״̬�㲥����
     */
	private class TrafficReceiver extends BroadcastReceiver {
		private String action = "";
		private static final String TAG = "TrafficReceiver";

		public void onReceive(Context context, Intent intent) {
			action = intent.getAction();

			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				isWIFI = true;
				if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
					// �����߳�ÿ������ͳ��WIFI����
					Log.i(TAG, "WIFI_STATE_ENABLED");
					new Thread(new Runnable() {

						@Override
						public void run() {
							while (isWIFI) {
								try {
									Thread.sleep(2000);
									Log.i(TAG, "һ�βɼ�WIFI>>>>>start");
									trafficInfosWifi = new ArrayList<TrafficInfo>();
									trafficInfosWifi = getTrafficInfos();

									for (TrafficInfo infoO : trafficInfosOrigin)
										for (TrafficInfo infoW : trafficInfosWifi) {
											long traffic = infoW.getTraffic()
													- infoO.getTraffic();
											if (infoO.getPackageName().equals(
													infoW.getPackageName())
													&& traffic != 0) {
												dbhelper.insertTrafficWIFI(traffic,infoW.getPackageName());
												Log.i(TAG,infoW
														.getPackageName()
														+ "WIFI:"
														+ Formatter
																.formatFileSize(
																		TrafficService.this,
																		traffic));
											}
										}
									trafficInfosOrigin = trafficInfosWifi;
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}).start();
				} else if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
					Log.i(TAG, "WIFI_STATE_DISABLED");
					//ֹͣͳ��WIFI
					isWIFI = false;
					Log.i(TAG, "һ�βɼ�WIFI>>>>>end");
				}
			} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
				Log.i(TAG, "CONNECTIVITY_ACTION");
				NetworkInfo networkInfo = cManager
						.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				State state = networkInfo.getState();
				if (state == State.CONNECTED
						&& !(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)) {
					Log.i(TAG, "State.CONNECTED");
					// �����߳�ÿ������ͳ��GPRS����
					isGPRS = true;
					new Thread(new Runnable() {

						@Override
						public void run() {
							while (isGPRS) {

								try {
									Thread.sleep(2000);
									Log.i(TAG, "һ�βɼ�GPRS>>>>>start");
									trafficInfosGprs = new ArrayList<TrafficInfo>();
									trafficInfosGprs = getTrafficInfos();
									for (TrafficInfo infoO : trafficInfosOrigin)
										for (TrafficInfo infoG : trafficInfosGprs) {
											long traffic = infoG.getTraffic()
													- infoO.getTraffic();
											if (infoO.getPackageName().equals(
													infoG.getPackageName())
													&& traffic != 0) {
												dbhelper.insertTrafficGPRS(traffic,infoG.getPackageName());
												Log.i(TAG,infoG
														.getPackageName()
														+ "GPRS:"
														+ Formatter
																.formatFileSize(
																		TrafficService.this,
																		traffic));
											}
										}
									trafficInfosOrigin = trafficInfosGprs;
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						}
					}).start();
				} else if (state == State.DISCONNECTED) {
					Log.i(TAG, "State.DISCONNECTED");
					//ֹͣͳ��GPRS
					isGPRS = false;
					Log.i(TAG, "һ�βɼ�GPRS>>>>>end");
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(tReceiver);
		super.onDestroy();
	}
    /**
     * ��ȡ����������Ϣ
     * @param void
     * @return List<TrafficInfo> MyTrafficInfos
     */
	public List<TrafficInfo> getTrafficInfos() {
		List<TrafficInfo> MyTrafficInfos = new ArrayList<TrafficInfo>();

		pm = getPackageManager();
		List<PackageInfo> packinfos = pm
				.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES
						| PackageManager.GET_PERMISSIONS);

		for (PackageInfo info : packinfos) {
			String[] premissions = info.requestedPermissions;
			if (premissions != null && premissions.length > 0) {
				for (String premission : premissions) {
					if ("android.permission.INTERNET".equals(premission)) {
						int uid = info.applicationInfo.uid;
						long total = TrafficStats.getUidRxBytes(uid)
								+ TrafficStats.getUidTxBytes(uid);
						if (total < 0) {
							TrafficInfo trafficInfo = new TrafficInfo();
							trafficInfo.setPackageName(info.packageName);
							trafficInfo.setTraffic(0);
							MyTrafficInfos.add(trafficInfo);
						} else {
							TrafficInfo trafficInfo = new TrafficInfo();
							trafficInfo.setPackageName(info.packageName);
							trafficInfo.setTraffic(total);
							MyTrafficInfos.add(trafficInfo);
						}
					}
				}
			}
		}
		return MyTrafficInfos;
	}
}
