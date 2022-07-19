package hu.htvk.challenge.json;

import android.os.Parcel;
import android.os.Parcelable;

public class Result implements Parcelable {

	private Integer resultCode;
	private FetchData dataObject;

	public Result() {
	}

	protected Result(Parcel in) {
		resultCode = in.readByte() == 0x00 ? null : in.readInt();
		dataObject = (FetchData) in.readValue(FetchData.class.getClassLoader());
	}
	

	public Integer getResultCode() {
		return resultCode;
	}

	public void setResultCode(Integer resultCode) {
		this.resultCode = resultCode;
	}

	public FetchData getFetchData() {
		return dataObject;
	}

	public void setFetchData(FetchData dataObject) {
		this.dataObject = dataObject;
	}


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (resultCode == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(resultCode);
        }
        dest.writeValue(dataObject);
    }

    public static final Parcelable.Creator<Result> CREATOR = new Parcelable.Creator<Result>() {
        @Override
        public Result createFromParcel(Parcel in) {
            return new Result(in);
        }

        @Override
        public Result[] newArray(int size) {
            return new Result[size];
        }
    };
	
	@Override
	public String toString() {
		return "Result [resultCode=" + resultCode + ", FetchData=" + dataObject + "]";
	}

}
