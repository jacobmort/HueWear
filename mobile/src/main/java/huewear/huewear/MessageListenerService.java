package huewear.huewear;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by jacob on 7/18/15.
 */
public class MessageListenerService extends WearableListenerService {
	private static final String TAG = "MessageListener";

	@Override
	public void onPeerConnected(Node peer) {
		super.onPeerConnected(peer);

		String id = peer.getId();
		String name = peer.getDisplayName();

		Log.d(TAG, "Connected peer name & ID: " + name + "|" + id);
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		System.out.println("service watch message1");
		if (messageEvent.getPath().equals("randomLights")) {
			Intent serviceIntent = new Intent(MessageListenerService.this, HueService.class);
			serviceIntent.setAction(HueService.ACTION_RANDOM);
			startService(serviceIntent);

		}
	}
}

