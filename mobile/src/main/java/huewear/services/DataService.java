package huewear.services;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import huewear.common.MessagePaths;
import huewear.common.PHLightStateParcelable;

/**
 * Created by jacob on 10/28/15.
 */

public class DataService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {
	public static final String TAG = "DataService";
	private GoogleApiClient mGoogleApiClient;

	@Override
	public void onCreate(){
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Wearable.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();
		mGoogleApiClient.connect();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "sending updated light status");
		ArrayList<PHLightStateParcelable> lightStates = intent.getParcelableArrayListExtra(HueService.LIGHTS_STATUS);
		PutDataMapRequest putDataMapReq = PutDataMapRequest.create(MessagePaths.LIGHTS_STATUS);
		putDataMapReq.getDataMap().putDataMapArrayList(MessagePaths.LIGHTS_STATUS, PHLightStateParcelable.getDataMap(lightStates));
		PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
		PendingResult<DataApi.DataItemResult> pendingResult =
				Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d(TAG, "onConnected");
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.d(TAG, "onConnectionSuspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "Failed to connect to Google API Client");
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		Log.i(TAG, "onDataChanged");
		for (DataEvent event : dataEvents) {
			if (event.getType() == DataEvent.TYPE_CHANGED) {
				// DataItem changed
				DataItem item = event.getDataItem();
				if (item.getUri().getPath().compareTo(MessagePaths.LIGHTS_OFF) == 0) {
					DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
					//updateCount(dataMap.getInt(COUNT_KEY));
				}
			} else if (event.getType() == DataEvent.TYPE_DELETED) {
				// DataItem deleted
			}
		}
	}

}
