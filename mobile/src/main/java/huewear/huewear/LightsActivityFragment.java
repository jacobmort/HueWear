package huewear.huewear;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import huewear.services.HueService;

/**
 * Created by jacob on 7/18/15.
 */
public class LightsActivityFragment extends Fragment {
	public static final String TAG = "LightsActivityFragment";

	public LightsActivityFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_connected, container, false);

		Button randomButton;
		randomButton = (Button) view.findViewById(R.id.buttonRand);
		randomButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent serviceIntent = new Intent(getActivity(), HueService.class);
				serviceIntent.setAction(HueService.ACTION_RANDOM);
				getActivity().startService(serviceIntent);
			}

		});
		return view;
	}
}
