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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.philips.lighting.hue.sdk.PHAccessPoint;

import java.util.ArrayList;

import huewear.models.AccessPointListAdapter;
import huewear.models.HueSharedPreferences;
import huewear.models.PHAccessPointParcelable;
import huewear.services.HueService;


public class BridgeActivityFragment extends Fragment implements AdapterView.OnItemClickListener {
	public static final String TAG = "BridgeActivityFragment";
	private AccessPointListAdapter mAdapter;
	private ProgressBar mScanProgressBar;
	private LinearLayout mBridgeList;
	private LinearLayout mLinkPrompt;

	private boolean connected = false;

	public BridgeActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View view =  inflater.inflate(R.layout.fragment_bridge, container, false);
		ListView accessPointList = (ListView) view.findViewById(R.id.bridge_list);
		accessPointList.setOnItemClickListener(this);
		mAdapter = new AccessPointListAdapter(getActivity().getApplicationContext(), new ArrayList<PHAccessPointParcelable>());
		accessPointList.setAdapter(mAdapter);
		mScanProgressBar = (ProgressBar) view.findViewById(R.id.scan_progress);
		mBridgeList = (LinearLayout) view.findViewById(R.id.BridgeList);
		mLinkPrompt = (LinearLayout) view.findViewById(R.id.BridgeConnect);
		setupHue();
		return view;
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			hideSpinner();
			if (action.equals(HueService.CONNECT_AUTH)){
				Toast.makeText(getActivity(), R.string.press_pushlink_button, Toast.LENGTH_SHORT).show();
			}else {
				mLinkPrompt.setVisibility(View.INVISIBLE);
				mBridgeList.setVisibility(View.VISIBLE);
				ArrayList<PHAccessPointParcelable> points = intent.getParcelableArrayListExtra(HueService.POINTS_FOUND);
				mAdapter.updateData(points);
			}
		}
	};

	private void setupHue() {
		// Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
		HueSharedPreferences prefs = HueSharedPreferences.getInstance(getActivity().getApplicationContext());
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
			showPushLinkView();
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
		if (mScanProgressBar != null){
			mScanProgressBar.setVisibility(View.GONE);
		}
	}

	public void showSpinner(){
		if (mScanProgressBar != null){
			mScanProgressBar.setVisibility(View.VISIBLE);
		}
	}

	public void showPushLinkView(){
		mLinkPrompt.setVisibility(View.VISIBLE);
		mBridgeList.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		HueSharedPreferences prefs = HueSharedPreferences.getInstance(getActivity().getApplicationContext());
		PHAccessPointParcelable accessPoint = (PHAccessPointParcelable) mAdapter.getItem(position);
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
