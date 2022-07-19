package hu.htvk.challenge.json;

import android.os.Parcel;
import android.os.Parcelable;

public class Visit implements DataObjectIf {

	private Integer id;
	private String checkpointName;
	private String visitorName;
	private String when;
	private String created;
	private String leave;
	private String location;
	private int checkpointId;

	public Visit() {
	}
	
    protected Visit(Parcel in) {
        id = in.readByte() == 0x00 ? null : in.readInt();
        checkpointName = in.readString();
        visitorName = in.readString();
        when = in.readString();
        created = in.readString();
        leave = in.readString();
        location = in.readString();
        checkpointId = in.readInt();
    }
    
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getCheckpointName() {
		return checkpointName;
	}
	public void setCheckpointName(String checkpointName) {
		this.checkpointName = checkpointName;
	}
	public String getVisitorName() {
		return visitorName;
	}
	public void setVisitorName(String visitorName) {
		this.visitorName = visitorName;
	}
	public String getWhen() {
		return when;
	}
	public void setWhen(String when) {
		this.when = when;
	}
	public String getCreated() {
		return created;
	}
	public void setCreated(String created) {
		this.created = created;
	}
	public String getLeave() {
		return leave;
	}
	public void setLeave(String leave) {
		this.leave = leave;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public int getCheckpointId() {
		return checkpointId;
	}
	public void setCheckpointId(int checkpointId) {
		this.checkpointId = checkpointId;
	}

	@Override
	public String toString() {
		return "Visit [id=" + id + ", checkpointName=" + checkpointName + ", visitorName="
				+ visitorName + ", when=" + when + ", created=" + created+ ", leave=" + leave+ ", location=" + location+ ", checkpointId=" + checkpointId + "]";
	}
	@Override
	public int describeContents() {
		return 0;
	}
	
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(id);
        }
        dest.writeString(checkpointName);
        dest.writeString(visitorName);
        dest.writeString(when);
        dest.writeString(created);
        dest.writeString(leave);
        dest.writeString(location);
        dest.writeInt(checkpointId);
    }

    public static final Parcelable.Creator<Visit> CREATOR = new Parcelable.Creator<Visit>() {
        @Override
        public Visit createFromParcel(Parcel in) {
            return new Visit(in);
        }

        @Override
        public Visit[] newArray(int size) {
            return new Visit[size];
        }
    };
	
	
	
}
