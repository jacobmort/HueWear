package huewear.huewear;

import android.os.Parcel;
import android.os.Parcelable;

import com.philips.lighting.hue.sdk.PHAccessPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 7/19/15.
 */
public class PHAccessPointParcelable extends PHAccessPoint implements Parcelable {
	public PHAccessPointParcelable(PHAccessPoint point){
		super(point.getIpAddress(), point.getUsername(), point.getMacAddress());
	}

	public static ArrayList<PHAccessPointParcelable> convert(List<PHAccessPoint> points){
		ArrayList<PHAccessPointParcelable> pointsConv = new ArrayList<PHAccessPointParcelable>();
		for (PHAccessPoint point : points){
			pointsConv.add(new PHAccessPointParcelable(point));
		}
		return pointsConv;
	}

	public PHAccessPointParcelable(Parcel in){
		String[] data = new String[4];

		in.readStringArray(data);
		this.setIpAddress(data[0]);
		this.setMacAddress(data[1]);
		this.setBridgeId(data[2]);
		this.setUsername(data[3]);
	}

	@Override
	public int describeContents(){
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(new String[] {this.getIpAddress(),
				this.getMacAddress(),
				this.getBridgeId(),
				this.getUsername()});
	}
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public PHAccessPointParcelable createFromParcel(Parcel in) {
			return new PHAccessPointParcelable(in);
		}

		public PHAccessPointParcelable[] newArray(int size) {
			return new PHAccessPointParcelable[size];
		}
	};
}
