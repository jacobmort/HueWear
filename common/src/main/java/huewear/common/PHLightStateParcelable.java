package huewear.common;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.wearable.DataMap;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jacob on 10/27/15.
 * Wrapper for PHLight implements Parcelable and only copies data that we care to change
 */
public class PHLightStateParcelable extends PHLightState implements Parcelable {
//	MEMBERS
//	private Boolean on;
//	private Integer brightness = null;
//	private Integer hue = null;
//	private Integer saturation = null;
//	private Integer ct = null;
//	private PHLight.PHLightAlertMode alert;
//	private PHLight.PHLightEffectMode effect;
//
//	private Integer transitionTime;
//	private Integer incrementBri;
//	private Integer incrementCt;
//	private Integer incrementHue;
//	private Integer incrementSat;
//	private Float incrementX;
//	private Float incrementY;
//	private Boolean isReachable;
//	private Float x;
//	private Float y;
//	private PHLight.PHLightColorMode colormode; //String

	public static final String DATA_MAP_KEY = "DATA_MAP_KEY";

	//TODO has to be a better way to do this- can't just cast though?
	public PHLightStateParcelable(PHLightState state){
		this.setOn(state.isOn());
//		this.setBrightness(state.getBrightness());
//		this.setHue(state.getHue());
//		this.setSaturation(state.getSaturation());
//		this.setCt(state.getCt());
//		this.setAlertMode(state.getAlertMode());
//		this.setEffectMode(state.getEffectMode());
//		this.setTransitionTime(state.getTransitionTime());
//		this.setIncrementBri(state.getIncrementBri());
//		this.setIncrementCt(state.getIncrementCt());
//		this.setIncrementSat(state.getIncrementSat());
//		this.setX(state.getX());
//		this.setY(state.getY());
//		this.setColorMode(state.getColorMode());
	}

	private PHLightStateParcelable(Parcel in) {
		List<String> parcel = new ArrayList<String>();
		in.readStringList(parcel);
	}

	public static ArrayList<DataMap> getDataMap(List<PHLightStateParcelable> lights){
		ArrayList<DataMap> lightsDataMap = new ArrayList<DataMap>();
		for (PHLightStateParcelable light : lights){
			DataMap lightMap = new DataMap();
			lightMap.putStringArrayList(PHLightStateParcelable.DATA_MAP_KEY, new ArrayList(light.getSeralized()));
			lightsDataMap.add(lightMap);
		}
		return lightsDataMap;
	}

	private List<String> getSeralized(){
		return Arrays.asList(
				String.valueOf(this.isOn())
//				String.valueOf(this.getBrightness()),
//				String.valueOf(this.getHue()),
//				String.valueOf(this.getSaturation()),
//				String.valueOf(this.getCt()),
//				this.getAlertMode().getValue(),
//				this.getEffectMode().getValue(),
//				String.valueOf(this.getTransitionTime()),
//				String.valueOf(this.getIncrementBri()),
//				String.valueOf(this.getIncrementCt()),
//				String.valueOf(this.getIncrementSat()),
//				String.valueOf(this.getX()),
//				String.valueOf(this.getY()),
//				this.getColorMode().getValue()
		);
	}

	//Parcelable implementation
	public int describeContents() {
		return 0;
	}

	// write your object's data to the passed-in Parcel
	public void writeToParcel(Parcel out, int flags) {
		out.writeStringList(getSeralized());
	}

	// this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
	public static final Parcelable.Creator<PHLightStateParcelable> CREATOR = new Parcelable.Creator<PHLightStateParcelable>() {
		public PHLightStateParcelable createFromParcel(Parcel in) {
			return new PHLightStateParcelable(in);
		}

		public PHLightStateParcelable[] newArray(int size) {
			return new PHLightStateParcelable[size];
		}
	};
}
