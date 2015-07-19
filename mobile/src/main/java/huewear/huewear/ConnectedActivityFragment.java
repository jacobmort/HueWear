package huewear.huewear;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by jacob on 7/18/15.
 */
public class ConnectedActivityFragment extends Fragment {
	private PHHueSDK phHueSDK;
	private static final int MAX_HUE=65535;
	public static final String TAG = "ConnectedActivityFragment";

	public ConnectedActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_connected, container, false);

		phHueSDK = PHHueSDK.create();
		Button randomButton;
		randomButton = (Button) view.findViewById(R.id.buttonRand);
		randomButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				randomLights();
			}

		});
		return view;
	}

	public void randomLights() {
		PHBridge bridge = phHueSDK.getSelectedBridge();

		List<PHLight> allLights = bridge.getResourceCache().getAllLights();
		Random rand = new Random();

		for (PHLight light : allLights) {
			PHLightState lightState = new PHLightState();
			lightState.setHue(rand.nextInt(MAX_HUE));
			// To validate your lightstate is valid (before sending to the bridge) you can use:
			// String validState = lightState.validateState();
			bridge.updateLightState(light, lightState, listener);
			//  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
		}
	}

	// If you want to handle the response from the bridge, create a PHLightListener object.
	PHLightListener listener = new PHLightListener() {

		@Override
		public void onSuccess() {
		}

		@Override
		public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
			Log.w(TAG, "Light has updated");
		}

		@Override
		public void onError(int arg0, String arg1) {}

		@Override
		public void onReceivingLightDetails(PHLight arg0) {}

		@Override
		public void onReceivingLights(List<PHBridgeResource> arg0) {}

		@Override
		public void onSearchComplete() {}
	};

	@Override
	public void onDestroy() {
		PHBridge bridge = phHueSDK.getSelectedBridge();
		if (bridge != null) {

			if (phHueSDK.isHeartbeatEnabled(bridge)) {
				phHueSDK.disableHeartbeat(bridge);
			}

			phHueSDK.disconnect(bridge);
			super.onDestroy();
		}
	}
}
