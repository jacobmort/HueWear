package huewear.huewear;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;

import java.util.List;

public class MainActivity extends AppCompatActivity {

	public static final String TAG = "MainActivity";
	private PHHueSDK phHueSDK;
	private TextView mTextView;
	private HueSharedPreferences prefs;
	BridgeActivityFragment bridgeFragment;

	private boolean lastSearchWasIPScan = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bridgeFragment = new BridgeActivityFragment();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(R.id.container, bridgeFragment, "first");
		transaction.addToBackStack(null);
		transaction.commit();

		setupHue();
	}

	private void setupHue() {
		// Gets an instance of the Hue SDK.
		phHueSDK = PHHueSDK.create();

		// Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
		phHueSDK.setAppName("HueWear");
		phHueSDK.setDeviceName(android.os.Build.MODEL);

		// Register the PHSDKListener to receive callbacks from the bridge.
		phHueSDK.getNotificationManager().registerSDKListener(listener);

		// Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
		prefs = HueSharedPreferences.getInstance(getApplicationContext());
		String lastIpAddress   = prefs.getLastConnectedIPAddress();
		String lastUsername    = prefs.getUsername();

		// Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
		if (lastIpAddress !=null && !lastIpAddress.equals("")) {
			PHAccessPoint lastAccessPoint = new PHAccessPoint();
			lastAccessPoint.setIpAddress(lastIpAddress);
			lastAccessPoint.setUsername(lastUsername);

			if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
				bridgeFragment.showSpinner();
				phHueSDK.connect(lastAccessPoint);
			}
		}
		else {  // First time use, so perform a bridge search.
			doBridgeSearch();
		}
	}

	public PHHueSDK getHueSdk(){
		return phHueSDK;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (listener !=null) {
			phHueSDK.getNotificationManager().unregisterSDKListener(listener);
		}
		phHueSDK.disableAllHeartbeat();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.find_new_bridge:
				doBridgeSearch();
				break;
		}
		return true;
	}

	// Local SDK Listener
	private PHSDKListener listener = new PHSDKListener() {

		@Override
		public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
			Log.w(TAG, "Access Points Found. " + accessPoint.size());

			//PHWizardAlertDialog.getInstance().closeProgressDialog();
			if (accessPoint != null && accessPoint.size() > 0) {
				phHueSDK.getAccessPointsFound().clear();
				phHueSDK.getAccessPointsFound().addAll(accessPoint);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						bridgeFragment.foundAccessPoints(phHueSDK.getAccessPointsFound());
					}
				});

			}

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
			prefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
			prefs.setUsername(prefs.getUsername());
			bridgeFragment.hideSpinner();
			Fragment fragment = new ConnectedActivityFragment();
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.add(R.id.container, fragment, "second");
			transaction.addToBackStack(null);
			transaction.commit();
		}

		@Override
		public void onAuthenticationRequired(PHAccessPoint accessPoint) {
			Log.w(TAG, "Authentication Required.");
			phHueSDK.startPushlinkAuthentication(accessPoint);
			startActivity(new Intent(MainActivity.this, PHPushlinkActivity.class));

		}

		@Override
		public void onConnectionResumed(PHBridge bridge) {
			if (MainActivity.this.isFinishing())
				return;

			Log.v(TAG, "onConnectionResumed" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
			phHueSDK.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(),  System.currentTimeMillis());
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
		}

		@Override
		public void onError(int code, final String message) {
			Log.e(TAG, "on Error Called : " + code + ":" + message);

			if (code == PHHueError.NO_CONNECTION) {
				Log.w(TAG, "On No Connection");
			}
			else if (code == PHHueError.AUTHENTICATION_FAILED || code==1158) {
				bridgeFragment.hideSpinner();
			}
			else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
				Log.w(TAG, "Bridge Not Responding . . . ");
				//PHWizardAlertDialog.getInstance().closeProgressDialog();
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						PHWizardAlertDialog.showErrorDialog(MainActivity.this, message, R.string.btn_ok);
					}
				});

			}
			else if (code == PHMessageType.BRIDGE_NOT_FOUND) {

				if (!lastSearchWasIPScan) {  // Perform an IP Scan (backup mechanism) if UPNP and Portal Search fails.
					phHueSDK = PHHueSDK.getInstance();
					PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
					sm.search(false, false, true);
					lastSearchWasIPScan=true;
				}
				else {
					bridgeFragment.hideSpinner();
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							PHWizardAlertDialog.showErrorDialog(MainActivity.this, message, R.string.btn_ok);
						}
					});
				}


			}
		}

		@Override
		public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
			for (PHHueParsingError parsingError: parsingErrorsList) {
				Log.e(TAG, "ParsingError : " + parsingError.getMessage());
			}
		}
	};

	public void doBridgeSearch() {
		bridgeFragment.showSpinner();
		PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
		// Start the UPNP Searching of local bridges.
		sm.search(true, true);
	}

}
