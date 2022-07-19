package hu.htvk.challenge.json;

import android.os.Parcel;

public class Checkpoint implements DataObjectIf {

	private Integer id;
	private String name;
	private int priority;
	private int type;
	private String location;
	
	public Checkpoint() {
	}
	
    protected Checkpoint(Parcel in) {
        id = in.readByte() == 0x00 ? null : in.readInt();
        name = in.readString();
        priority = in.readByte();
        type = in.readByte();
        location = in.readString();
    }
    
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Checkpoint [id=" + id + ", name=" + name + ", priority="
				+ priority + ", type=" + type + ", location=" + location + "]";
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
        dest.writeString(name);
        dest.writeByte((byte)priority);
        dest.writeByte((byte)type);
        dest.writeString(location);
    }

    public static final Creator<Checkpoint> CREATOR = new Creator<Checkpoint>() {
        @Override
        public Checkpoint createFromParcel(Parcel in) {
            return new Checkpoint(in);
        }

        @Override
        public Checkpoint[] newArray(int size) {
            return new Checkpoint[size];
        }
    };
	
	
	
}
