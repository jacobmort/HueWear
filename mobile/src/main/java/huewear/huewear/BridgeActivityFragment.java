package huewear.huewear;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;

import java.util.List;


public class BridgeActivityFragment extends Fragment implements AdapterView.OnItemClickListener {
	public static final String TAG = "BridgeActivityFragment";
	private AccessPointListAdapter adapter;
	private ProgressBar scanProgressBar;
	private PHHueSDK pHueSdk;

	public BridgeActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view =  inflater.inflate(R.layout.fragment_bridge, container, false);


		scanProgressBar = (ProgressBar) view.findViewById(R.id.scan_progress);
		pHueSdk = ((MainActivity)getActivity()).getHueSdk();
		adapter = new AccessPointListAdapter(getActivity().getApplicationContext(), pHueSdk.getAccessPointsFound());

		ListView accessPointList = (ListView) view.findViewById(R.id.bridge_list);
		accessPointList.setOnItemClickListener(this);
		accessPointList.setAdapter(adapter);
		return view;
	}

	public void hideSpinner(){
		scanProgressBar.setVisibility(View.GONE);
	}

	public void showSpinner(){
		if (scanProgressBar != null){
			scanProgressBar.setVisibility(View.VISIBLE);
		}
	}

	public void foundAccessPoints(List<PHAccessPoint> points){
		if (scanProgressBar != null) {
			adapter.updateData(points);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		HueSharedPreferences prefs = HueSharedPreferences.getInstance(getActivity().getApplicationContext());
		PHAccessPoint accessPoint = (PHAccessPoint) adapter.getItem(position);
		accessPoint.setUsername(prefs.getUsername());

		PHBridge connectedBridge = pHueSdk.getSelectedBridge();

		if (connectedBridge != null) {
			String connectedIP = connectedBridge.getResourceCache().getBridgeConfiguration().getIpAddress();
			if (connectedIP != null) {   // We are already connected here:-
				pHueSdk.disableHeartbeat(connectedBridge);
				pHueSdk.disconnect(connectedBridge);
			}
		}
		scanProgressBar.setVisibility(View.VISIBLE);
		pHueSdk.connect(accessPoint);
	}
}
