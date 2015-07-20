package huewear.huewear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.philips.lighting.hue.sdk.PHAccessPoint;

public class MainActivity extends AppCompatActivity {

	public static final String TAG = "MainActivity";
	private TextView mTextView;
	private HueSharedPreferences prefs;
	private BridgeActivityFragment bridgeFragment;
	private ConnectedActivityFragment connectedFragment;
	private BroadcastReceiver mMessageReceiver;
	private boolean lastSearchWasIPScan = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bridgeFragment = new BridgeActivityFragment();
		connectedFragment = new ConnectedActivityFragment();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(R.id.container, bridgeFragment, "first");
		transaction.addToBackStack(null);
		transaction.commit();

		mMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.add(R.id.container, connectedFragment, "first");
				transaction.addToBackStack(null);
				transaction.commit();
			}
		};

		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter(HueService.CONNECT_SUCCESS));
		setupHue();
	}

	private void setupHue() {
		// Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
		prefs = HueSharedPreferences.getInstance(getApplicationContext());
		String lastIpAddress   = prefs.getLastConnectedIPAddress();
		String lastUsername    = prefs.getUsername();

		// Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
		if (lastIpAddress !=null && !lastIpAddress.equals("")) {
			PHAccessPoint lastAccessPoint = new PHAccessPoint();
			lastAccessPoint.setIpAddress(lastIpAddress);
			lastAccessPoint.setUsername(lastUsername);

			Intent serviceIntent = new Intent(this, HueService.class);
			serviceIntent.putExtra(HueService.COMMAND, "connect");
			serviceIntent.putExtra(HueService.ACCESS_POINT_EXTRA, new PHAccessPointParcelable(lastAccessPoint));
			startService(serviceIntent);
		}
		else {  // First time use, so perform a bridge search.
			Intent serviceIntent = new Intent(this, HueService.class);
			bridgeFragment.showSpinner();
			serviceIntent.putExtra(HueService.COMMAND, "doBridgeSearch");
			startService(serviceIntent);
		}
	}

	@Override
	public void onDestroy(){
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.find_new_bridge:
				Intent serviceIntent = new Intent(this, HueService.class);
				bridgeFragment.showSpinner();
				serviceIntent.putExtra(HueService.COMMAND, "doBridgeSearch");
				startService(serviceIntent);
				break;
		}
		return true;
	}

}
