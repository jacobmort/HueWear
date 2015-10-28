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

	private BroadcastReceiver  mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(HueService.CONNECT_AUTH)){
			hideSpinner();
			connected = true;
		}else if(action.equals(HueService.POINTS_FOUND)) {
			hideSpinner();
			mLinkPrompt.setVisibility(View.INVISIBLE);
			mBridgeList.setVisibility(View.VISIBLE);
			ArrayList<PHAccessPointParcelable> points = intent.getParcelableArrayListExtra(HueService.POINTS_FOUND);
			mAdapter.updateData(points);
		}else if (action.equals(HueService.DISCONNECT)) {
			connected = false;
			showPushLinkView();
			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.setAction(HueService.ACTION_SEARCH);
			getActivity().startService(serviceIntent);
		}
		}
	};

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

	private void setupHue() {
		// Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
		this.showSpinner();
		Intent serviceIntent = new Intent(getActivity(), HueService.class);
		serviceIntent.setAction(HueService.ACTION_RECONNECT);
		getActivity().startService(serviceIntent);
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
		PHAccessPointParcelable accessPoint = (PHAccessPointParcelable) mAdapter.getItem(position);
		HueSharedPreferences prefs = HueSharedPreferences.getInstance(getActivity().getApplicationContext());
		accessPoint.setUsername(prefs.getUsername());
		this.showSpinner();

		Intent serviceIntent = new Intent(getActivity(), HueService.class);
		serviceIntent.setAction(HueService.ACTION_CONNECT);
		serviceIntent.putExtra(HueService.ACCESS_POINT_EXTRA, new PHAccessPointParcelable(accessPoint));
		getActivity().startService(serviceIntent);
	}
}
