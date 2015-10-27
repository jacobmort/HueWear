package huewear.models;

import android.os.Bundle;

/**
 * Created by jacob on 10/27/15.
 */
public class HueCommand {
	private int mStartId;
	private String mCommand;
	private Bundle mArgs;

	public HueCommand(int serviceId, String hueCommand, Bundle hueArgs){
		mStartId = serviceId;
		mCommand = hueCommand;
		mArgs = hueArgs;
	}

	public int getStartId(){
		return mStartId;
	}

	public String getCommand(){
		return mCommand;
	}

	public Bundle getArgs(){
		return mArgs;
	}

}
