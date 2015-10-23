package huewear.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import huewear.common.MessagePaths;
import huewear.models.PHAccessPointParcelable;

/**
 * Created by jacob on 7/19/15.
 */

public class HueService extends Service implements PHSDKListener, PHLightListener {
	private static final String TAG = "HueService";
	private static final int MAX_HUE=65535;

	public static final String ACTION_CONNECT = "connect";
	public static final String ACTION_DISCONNECT = "disconnect";
	public static final String ACTION_SEARCH = "doBridgeSearch";
	public static final String ACTION_RANDOM = MessagePaths.LIGHTS_RANDOM;
	public static final String ACTION_OFF = MessagePaths.LIGHTS_OFF;
	public static final String ACTION_BRIGHTNESS = MessagePaths.LIGHTS_BRIGTHNESS;

	public static final String POINTS_FOUND = "points_found";
	public static final String LIGHT_SUCCESS = "light.success";
	public static final String CONNECT_SUCCESS = "connect.success";
	public static final String CONNECT_IP = "connect.success.ip";
	public static final String CONNECT_AUTH = "connect.authrequired";
	public static final String DISCONNECT = "disconnect";
	public static final String ERROR = "error";
	public static final String ACCESS_POINT_EXTRA = "access_point_extra";
	public static final String WATCH_MESSAGE_EXTRA = "WATCH_MESSAGE_EXTRA";

	private static final String COMMAND = "HueService.command";

	// These store IDs to running services so they can be stopped when their callback runs
	private List<Integer> mAllIds; //some Hue callbacks can't tell which it was called from- stop all
	private List<Integer> mLightControlIds;
	private List<Integer> mSearchIds;
	private List<Integer> mConnectIds;

	private boolean lastSearchWasIPScan = false;
	private PHHueSDK phHueSDK;

	private LocalBroadcastManager manager;

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			String command = b.getString(COMMAND);
			mAllIds.add(msg.arg1);
			switch(command){
				case ACTION_RANDOM:
					mLightControlIds.add(msg.arg1);
					randomLights();
					break;
				case ACTION_SEARCH:
					mSearchIds.add(msg.arg1);
					doBridgeSearch();
					break;
				case ACTION_CONNECT:
					mConnectIds.add(msg.arg1);
					PHAccessPoint point = b.getParcelable(ACCESS_POINT_EXTRA);
					connect(point);
					break;
				case ACTION_DISCONNECT:
					disconnect();
					break;
				case ACTION_OFF:
					mLightControlIds.add(msg.arg1);
					lightsOff();
					break;
				case ACTION_BRIGHTNESS:
					mLightControlIds.add(msg.arg1);
					String amt = b.getString(WATCH_MESSAGE_EXTRA);
					adjustBrightness(Integer.parseInt(amt));
					break;
				default:
					Log.i(TAG, "non-matching command"+command);
					stopSelf(msg.arg1);
			}
		}
	}

	@Override
	public void onCreate(){
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		mAllIds = new ArrayList<Integer>();
		mLightControlIds = new ArrayList<Integer>();
		mSearchIds = new ArrayList<Integer>();
		mConnectIds = new ArrayList<Integer>();

		phHueSDK = PHHueSDK.create();
		// Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
		phHueSDK.setAppName("HueWear");
		phHueSDK.setDeviceName(android.os.Build.MODEL);

		// Register the PHSDKListener to receive callbacks from the bridge.
		phHueSDK.getNotificationManager().registerSDKListener(this);
		manager = LocalBroadcastManager.getInstance(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job

		Bundle b = new Bundle();
		b.putString(COMMAND, intent.getAction());
		b.putParcelable(ACCESS_POINT_EXTRA, intent.getParcelableExtra(ACCESS_POINT_EXTRA));
		b.putString(WATCH_MESSAGE_EXTRA, intent.getStringExtra(WATCH_MESSAGE_EXTRA));
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.setData(b);

		mServiceHandler.sendMessage(msg);
		// If we get killed, after returning from here, restart
		return START_REDELIVER_INTENT;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		phHueSDK.getNotificationManager().unregisterSDKListener(this);
		phHueSDK.disableAllHeartbeat();
	}

	private void stopServices(List<Integer> ids){
		for (int i : ids){
			stopSelf(i);
		}
		ids.clear();
	}

	private void doBridgeSearch() {
		PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
		// Start the UPNP Searching of local bridges.
		sm.search(true, true);
	}

	private void connect(PHAccessPoint lastAccessPoint){
		if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
			phHueSDK.connect(lastAccessPoint);
		}
	}

	public void disconnect(){
		String connectedIP = phHueSDK.getSelectedBridge().getResourceCache().getBridgeConfiguration().getIpAddress();
		if (connectedIP != null) {   // We are already connected here:-
			phHueSDK.disableHeartbeat(phHueSDK.getSelectedBridge());
			phHueSDK.disconnect(phHueSDK.getSelectedBridge());
		}
		Intent intent = new Intent(DISCONNECT);
		manager.sendBroadcast(intent);
	}

	private void randomLights() {
		Log.i(TAG, "randomLights");
		PHBridge bridge = phHueSDK.getSelectedBridge();

		List<PHLight> allLights = bridge.getResourceCache().getAllLights();
		Random rand = new Random();

		for (PHLight light : allLights) {
			PHLightState lightState = new PHLightState();
			lightState.setOn(true);
			lightState.setHue(rand.nextInt(MAX_HUE));
			// To validate your lightstate is valid (before sending to the bridge) you can use:
			// String validState = lightState.validateState();
			bridge.updateLightState(light, lightState, this);
			//  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
		}
		stopServices(mLightControlIds);
	}

	private void lightsOff() {
		Log.i(TAG, "lightsOff");
		PHBridge bridge = phHueSDK.getSelectedBridge();
		List<PHLight> allLights = bridge.getResourceCache().getAllLights();
		Random rand = new Random();

		for (PHLight light : allLights) {
			PHLightState lightState = new PHLightState();
			lightState.setOn(false);
			// To validate your lightstate is valid (before sending to the bridge) you can use:
			// String validState = lightState.validateState();
			bridge.updateLightState(light, lightState, this);
			//  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
		}
		stopServices(mLightControlIds);
	}

	private void adjustBrightness(int val){
		Log.i(TAG, "adjustBrightness to "+String.valueOf(val));
		PHBridge bridge = phHueSDK.getSelectedBridge();
		List<PHLight> allLights = bridge.getResourceCache().getAllLights();

		for (PHLight light : allLights) {
			PHLightState lightState = new PHLightState();
			lightState.setBrightness(val, true);
			bridge.updateLightState(light, lightState, this);
		}
	}

	//PHSDKListener methods
	@Override
	public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
		Log.w(TAG, "Access Points Found. " + accessPoint.size());
		if (accessPoint.size() > 0) {
			phHueSDK.getAccessPointsFound().clear();
			phHueSDK.getAccessPointsFound().addAll(accessPoint);
			Intent intent = new Intent(POINTS_FOUND);
			intent.putParcelableArrayListExtra(POINTS_FOUND, PHAccessPointParcelable.convert(phHueSDK.getAccessPointsFound()));
			manager.sendBroadcast(intent);
		}
		stopServices(mSearchIds);
	}

	@Override
	public void onCacheUpdated(List<Integer> arg0, PHBridge bridge) {
		Log.w(TAG, "On CacheUpdated");
	}

	@Override
	public void onBridgeConnected(PHBridge b) {
		phHueSDK.setSelectedBridge(b);
		phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
		phHueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
		Intent intent = new Intent(CONNECT_SUCCESS);
		intent.putExtra(CONNECT_IP, b.getResourceCache().getBridgeConfiguration().getIpAddress());
		manager.sendBroadcast(intent);
		stopServices(mConnectIds);
	}

	@Override
	public void onAuthenticationRequired(PHAccessPoint accessPoint) {
		Log.w(TAG, "Authentication Required.");
		phHueSDK.startPushlinkAuthentication(accessPoint);
		Intent intent = new Intent(CONNECT_AUTH);
		manager.sendBroadcast(intent);
	}

	@Override
	public void onConnectionResumed(PHBridge bridge) {
		Log.v(TAG, "onConnectionResumed" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
		phHueSDK.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
		for (int i = 0; i < phHueSDK.getDisconnectedAccessPoint().size(); i++) {
			if (phHueSDK.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
				phHueSDK.getDisconnectedAccessPoint().remove(i);
			}
		}
	}

	@Override
	public void onConnectionLost(PHAccessPoint accessPoint) {
		Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
		if (!phHueSDK.getDisconnectedAccessPoint().contains(accessPoint)) {
			phHueSDK.getDisconnectedAccessPoint().add(accessPoint);
		}
		stopServices(mAllIds);
	}

	@Override
	public void onError(int code, final String message) {
		Log.e(TAG, "on Error Called : " + code + ":" + message);
		if (code == PHMessageType.BRIDGE_NOT_FOUND && !lastSearchWasIPScan) {// Perform an IP Scan (backup mechanism) if UPNP and Portal Search fails.
			phHueSDK = PHHueSDK.getInstance();
			PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
			sm.search(false, false, true);
			lastSearchWasIPScan = true;
		}else { //return error
			String error;
			if (code == PHHueError.NO_CONNECTION) {
				error = "On No Connection";
			}
			else if (code == PHHueError.AUTHENTICATION_FAILED || code==1158) {
				error ="Authentication failed";
			}
			else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
				error = "Bridge not responding";
			}
			else {
				error = message;
			}
			Log.w(TAG, error);
			Intent intent = new Intent(ERROR);
			intent.putExtra(ERROR, error);
			manager.sendBroadcast(intent);
		}
		stopServices(mAllIds);
	}

	@Override
	public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
		for (PHHueParsingError parsingError: parsingErrorsList) {
			Log.e(TAG, "ParsingError : " + parsingError.getMessage());
		}
		Intent intent = new Intent(ERROR);
		intent.putExtra(ERROR, "ParsingError");
		manager.sendBroadcast(intent);
		stopServices(mAllIds);
	}


	//PHLightListener methods
	@Override
	public void onSuccess() {
		Intent intent = new Intent(LIGHT_SUCCESS);
		manager.sendBroadcast(intent);
		stopServices(mAllIds);
	}

	@Override
	public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
		Log.w(TAG, "Light has updated");
		stopServices(mLightControlIds);
	}

	@Override
	public void onReceivingLightDetails(PHLight arg0) {}

	@Override
	public void onReceivingLights(List<PHBridgeResource> arg0) {}

	@Override
	public void onSearchComplete() {}

}
