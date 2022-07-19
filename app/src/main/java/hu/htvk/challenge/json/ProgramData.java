package hu.htvk.challenge.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;

public class ProgramData implements DataObjectIf {

	private List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();

	private String title;
	private Date startDate;

	public ProgramData() {
	}
	
    protected ProgramData(Parcel in) {
        title = in.readString();
        startDate = new Date(in.readLong());

        if (in.readByte() == 0x01) {
            checkpoints = new ArrayList<Checkpoint>();
            in.readList(checkpoints, Checkpoint.class.getClassLoader());
        } else {
            checkpoints = null;
        }
    }

	public ProgramData(JSONObject resultObject) throws JSONException,ParseException	 {
		setTitle(resultObject.getString("title"));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		setStartDate(sdf.parse(resultObject.getString("startdate")));
		JSONArray eventsJson = resultObject.getJSONArray("checkpoints");
		for (int i = 0; i < eventsJson.length(); i++) {
			JSONArray eventObject = eventsJson.getJSONArray(i);
			Checkpoint v = new Checkpoint();
			v.setId(eventObject.getInt(0));
			v.setName(eventObject.getString(1));
			v.setPriority(eventObject.getInt(2));
			v.setType(eventObject.getInt(3));
			v.setLocation(eventObject.getString(4));
			addCheckpoint(v);
		}
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String _title) {
		this.title = _title;
	}

	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}

	public void setCheckpoints(List<Checkpoint> events) {
		this.checkpoints = events;
	}
	
	public void addCheckpoint(Checkpoint event){
		checkpoints.add(event);
	}
	
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	@Override
	public String toString() {
		return "ProgramData [checkpoints=" + checkpoints + ", title=" + title + ", startDate=" + startDate + "]";
	}
	
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeLong(startDate.getTime());
        if (checkpoints == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(checkpoints);
        }
    }

    public static final Creator<ProgramData> CREATOR = new Creator<ProgramData>() {
        @Override
        public ProgramData createFromParcel(Parcel in) {
            return new ProgramData(in);
        }

        @Override
        public ProgramData[] newArray(int size) {
            return new ProgramData[size];
        }
    };
}
