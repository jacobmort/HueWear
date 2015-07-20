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

import java.util.ArrayList;


public class BridgeActivityFragment extends Fragment implements AdapterView.OnItemClickListener {
	public static final String TAG = "BridgeActivityFragment";
	private AccessPointListAdapter adapter;
	private ProgressBar scanProgressBar;
	private ListView accessPointList;

	private boolean connected = false;

	public BridgeActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPointsFound,
				new IntentFilter(HueService.POINTS_FOUND));

		View view =  inflater.inflate(R.layout.fragment_bridge, container, false);
		accessPointList = (ListView) view.findViewById(R.id.bridge_list);
		accessPointList.setOnItemClickListener(this);
		adapter = new AccessPointListAdapter(getActivity().getApplicationContext(), new ArrayList<PHAccessPointParcelable>());
		accessPointList.setAdapter(adapter);
		scanProgressBar = (ProgressBar) view.findViewById(R.id.scan_progress);
		return view;
	}

	private BroadcastReceiver mPointsFound = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ArrayList<PHAccessPointParcelable> points = intent.getParcelableArrayListExtra(HueService.POINTS_FOUND);
			adapter.updateData(points);
			hideSpinner();
		}
	};

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPointsFound);
		super.onDestroy();
	}

	public void hideSpinner(){
		scanProgressBar.setVisibility(View.GONE);
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
			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.putExtra(HueService.COMMAND, "disconnect");
			getActivity().startService(serviceIntent);
			connected = false;
		}else {
			scanProgressBar.setVisibility(View.VISIBLE);
			Intent serviceIntent = new Intent(getActivity(), HueService.class);
			serviceIntent.putExtra(HueService.COMMAND, "connect");
			serviceIntent.putExtra(HueService.ACCESS_POINT_EXTRA, new PHAccessPointParcelable(accessPoint));
			getActivity().startService(serviceIntent);
		}
	}
}
