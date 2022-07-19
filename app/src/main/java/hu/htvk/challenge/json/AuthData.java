package hu.htvk.challenge.json;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcel;
import android.os.Parcelable;

public class AuthData implements DataObjectIf {

	private String userName;
	private String password;

	public AuthData(String userName,String password) {
		this.userName = userName;
		this.password = password;
	}

	protected AuthData(Parcel in) {
		userName = in.readString();
		password = in.readString();
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userName);
        dest.writeString(password);
    }

    public static final Parcelable.Creator<AuthData> CREATOR = new Parcelable.Creator<AuthData>() {
        @Override
        public AuthData createFromParcel(Parcel in) {
            return new AuthData(in);
        }

        @Override
        public AuthData[] newArray(int size) {
            return new AuthData[size];
        }
    };
    
	@Override
	public String toString() {
		return "AuthData [username=" + userName + ",password=" + password + "]";
	}
	
	public void writeToPreferences(SharedPreferences sp) {
		Editor e = sp.edit();
		e.putString("email", userName);
		e.putString("regcode", password);
		e.commit();
	}
	
	public void readFromPreferences(SharedPreferences sp) {
		userName = sp.getString("email", "");
		password = sp.getString("regcode", "");
	}
	
}
