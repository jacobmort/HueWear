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

import huewear.models.HueSharedPreferences;
import huewear.services.HueService;

public class MainActivity extends AppCompatActivity {

	public static final String TAG = "MainActivity";
	private BridgeActivityFragment mBridgeFragment;
	private LightsActivityFragment mConnectedFragment;
	private BroadcastReceiver mMessageReceiver;
	private HueSharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBridgeFragment = new BridgeActivityFragment();
		mConnectedFragment = new LightsActivityFragment();

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(R.id.container, mBridgeFragment, "first");
		transaction.addToBackStack(null);
		transaction.commit();

		mMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mPrefs = HueSharedPreferences.getInstance(context.getApplicationContext());
				mPrefs.setLastConnectedIPAddress(intent.getStringExtra(HueService.CONNECT_IP));
				mBridgeFragment.hideSpinner();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.replace(R.id.container, mConnectedFragment, "first");
				transaction.addToBackStack(null);
				transaction.commit();
			}
		};
	}

	@Override
	public void onPause(){
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	@Override
	public void onResume(){
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter(HueService.CONNECT_SUCCESS));
		super.onResume();
	}

	@Override
	public void onDestroy(){
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
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				Intent serviceIntent = new Intent(this, HueService.class);
				serviceIntent.setAction(HueService.DISCONNECT);
				startService(serviceIntent);
				transaction.replace(R.id.container, mBridgeFragment, "first");
				transaction.commit();
				break;
		}
		return true;
	}

}
