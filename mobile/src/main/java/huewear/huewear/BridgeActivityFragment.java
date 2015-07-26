package huewear.huewear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.philips.lighting.hue.sdk.PHAccessPoint;

import java.util.ArrayList;


public class BridgeActivityFragment extends Fragment implements AdapterView.OnItemClickListener {
	public static final String TAG = "BridgeActivityFragment";
	private AccessPointListAdapter adapter;
	private ProgressBar scanProgressBar;
	private ListView accessPointList;
	private HueSharedPreferences prefs;

	private boolean connected = false;

	public BridgeActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view =  inflater.inflate(R.layout.fragment_bridge, container, false);
		accessPointList = (ListView) view.findViewById(R.id.bridge_list);
		accessPointList.setOnItemClickListener(this);
		adapter = new AccessPointListAdapter(getActivity().getApplicationContext(), new ArrayList<PHAccessPointParcelable>());
		accessPointList.setAdapter(adapter);
		scanProgressBar = (ProgressBar) view.findViewById(R.id.scan_progress);
		setupHue();
		return view;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			//hideSpinner();
			if (action.equals(HueService.CONNECT_AUTH)){
				Toast.makeText(getActivity(), R.string.press_pushlink_button, Toast.LENGTH_SHORT).show();
			}else {
				ArrayList<PHAccessPointParcelable> points = intent.getParcelableArrayListExtra(HueService.POINTS_FOUND);
				adapter.updateData(points);
			}
		}
	};

	private void setupHue() {
		// Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
		prefs = HueSharedPreferences.getInstance(getActivity().getApplicationContext());
		String lastIpAddress   = prefs.getLastConnectedIPAddress();
		String lastUsername    = prefs.getUsername();

		this.showSpinner();
		// Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
		if (lastIpAddress !=null && !lastIpAddress.equals("")) {
			PHAccessPoint lastAccessPoint = new PHAccessPoint();
			lastAccessPoint.setIpAddress(lastIpAddress);
			lastAccessPoint.setUsername(lastUsername);

			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.setAction(HueService.ACTION_CONNECT);
			serviceIntent.putExtra(HueService.ACCESS_POINT_EXTRA, new PHAccessPointParcelable(lastAccessPoint));
			getActivity().startService(serviceIntent);
		}
		else {  // First time use, so perform a bridge search.
			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.setAction(HueService.ACTION_SEARCH);
			getActivity().startService(serviceIntent);
		}
	}

	@Override
	public void onPause(){
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	@Override
	public void onResume(){
		IntentFilter filterFor = new IntentFilter((HueService.POINTS_FOUND));
		filterFor.addAction(HueService.CONNECT_AUTH);
		filterFor.addAction(HueService.DISCONNECT);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, filterFor);
		super.onResume();
	}

	public void hideSpinner(){
		if (scanProgressBar != null){
			scanProgressBar.setVisibility(View.GONE);
		}
	}

	public void showSpinner(){
		if (scanProgressBar != null){
			scanProgressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		HueSharedPreferences prefs = HueSharedPreferences.getInstance(getActivity().getApplicationContext());
		PHAccessPointParcelable accessPoint = (PHAccessPointParcelable) adapter.getItem(position);
		accessPoint.setUsername(prefs.getUsername());

		if (connected) {
			this.showSpinner();
			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.setAction(HueService.ACTION_DISCONNECT);
			getActivity().startService(serviceIntent);
			connected = false;
		}else {
			this.showSpinner();
			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.setAction(HueService.ACTION_CONNECT);
			serviceIntent.putExtra(HueService.ACCESS_POINT_EXTRA, new PHAccessPointParcelable(accessPoint));
			getActivity().startService(serviceIntent);
		}
	}
}
