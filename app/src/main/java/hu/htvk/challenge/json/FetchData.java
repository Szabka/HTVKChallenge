package hu.htvk.challenge.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

public class FetchData implements DataObjectIf {

	private List<Visit> visits = new ArrayList<Visit>();

	private String shipName;
	private String userName;

	public FetchData() {
	}
	
    protected FetchData(Parcel in) {
        if (in.readByte() == 0x01) {
            visits = new ArrayList<Visit>();
            in.readList(visits, Visit.class.getClassLoader());
        } else {
            visits = null;
        }
        shipName = in.readString();
        userName = in.readString();
    }

	public FetchData(JSONObject resultObject) throws JSONException {
		setUserName(resultObject.getString("user"));
		setShipName(resultObject.getString("ship"));
		JSONArray eventsJson = resultObject.getJSONArray("visits");
		for (int i = 0; i < eventsJson.length(); i++) {
			JSONArray eventObject = eventsJson.getJSONArray(i);
			Visit v = new Visit();
			v.setId(eventObject.getInt(0));
			v.setCheckpointName(eventObject.getString(1));
			v.setVisitorName(eventObject.getString(2));
			v.setWhen(eventObject.getString(3));
			v.setCreated(eventObject.getString(4));
			v.setLeave(eventObject.getString(5));
			v.setLocation(eventObject.getString(6));
			v.setCheckpointId(eventObject.getInt(7));
			addVisit(v);
		}
	}

	public String getShipName() {
		return shipName;
	}
	public void setShipName(String shipName) {
		this.shipName = shipName;
	}

	public List<Visit> getVisits() {
		return visits;
	}

	public void setVisits(List<Visit> events) {
		this.visits = events;
	}
	
	public void addVisit(Visit event){
		visits.add(event);
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	@Override
	public String toString() {
		return "FetchData [visits=" + visits + ", shipName=" + shipName + ", userName=" + userName + "]";
	}
	
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (visits == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(visits);
        }
        dest.writeString(shipName);
        dest.writeString(userName);
    }

    public static final Parcelable.Creator<FetchData> CREATOR = new Parcelable.Creator<FetchData>() {
        @Override
        public FetchData createFromParcel(Parcel in) {
            return new FetchData(in);
        }

        @Override
        public FetchData[] newArray(int size) {
            return new FetchData[size];
        }
    };
}
