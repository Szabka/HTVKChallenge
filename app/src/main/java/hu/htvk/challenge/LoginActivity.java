package hu.htvk.challenge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import androidx.appcompat.app.AppCompatActivity;
import hu.htvk.challenge.https.HttpConnector;
import hu.htvk.challenge.json.AuthData;
import hu.htvk.challenge.json.FetchData;
import hu.htvk.challenge.json.ProgramData;
import hu.htvk.challenge.json.Result;
import hu.htvk.challenge.utils.Constants;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class LoginActivity extends AppCompatActivity  {

	private ProgramData programdata;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		TextView username = findViewById(R.id.usernameinput);
		TextView password = findViewById(R.id.passwordinput);

		Button btnLogin = findViewById(R.id.btnLogin);
		btnLogin.setOnClickListener(v -> {
			AuthData authData = new AuthData(
					username.getText().toString(),
					password.getText().toString());

			new FetchDataAsyncTask(programdata, authData, this)
					.execute();
		});

		new TitleLoadAsyncTask(this).execute();
	}

	private static class TitleLoadAsyncTask extends AsyncTask<Object, Integer, String> {
		@SuppressLint("StaticFieldLeak")
		private final LoginActivity caller;
		
		public TitleLoadAsyncTask(LoginActivity caller) {
			this.caller=caller;
		}

		@Override
		protected String doInBackground(Object... params) {
			try{
				RequestBody body = new FormBody.Builder(StandardCharsets.UTF_8)
						.add("op", "title").build();

				return HttpConnector.execute(caller.getApplicationContext(), body);
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			return "";
		}
		
		@Override
		protected void onPostExecute(String ret) {
			try {
				Log.d(getClass().getName(), "Response is: " + ret);
				if (ret.length() > 0) {
					JSONObject resultObject = new JSONObject(ret);
					if (resultObject.getInt(Constants.TAG_RESULTCODE) >0) {
 					    ProgramData programdata = new ProgramData(resultObject);
    					Log.d(getClass().getName(), "pd: " + programdata);
						TextView tv = (TextView) caller.findViewById(R.id.TextView01);
						if (tv!=null) tv.setText(programdata.getTitle());
						caller.programdata = programdata;
					}
				}
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
		}
	}

	private static class FetchDataAsyncTask extends AsyncTask<Object, Integer, String> {
		private final ProgramData pd;
	    private final AuthData ad;
		@SuppressLint("StaticFieldLeak")
		private final Activity caller;
		
		public FetchDataAsyncTask(ProgramData pd,AuthData authData,Activity caller) {
		    this.pd = pd;
			this.ad = authData;
			this.caller=caller;
		}

		@Override
		protected String doInBackground(Object... params) {
			try{
				RequestBody body = new FormBody.Builder(StandardCharsets.UTF_8)
						.add("email",ad.getUserName())
						.add("regcode",ad.getPassword())
						.add("op", "fetch")
						.build();

				return HttpConnector.execute(caller.getApplicationContext(), body);
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			return "";
		}
		
		@Override
		protected void onPostExecute(String ret) {
			try {
				Result result = new Result();
				Log.d(getClass().getName(), "Response is: "+ret);
				if (ret.length() > 0) {
					JSONObject resultObject = new JSONObject(ret);
					result.setResultCode(resultObject.getInt(Constants.TAG_RESULTCODE));
					Log.d(getClass().getName(), "" + result.getResultCode());
					if (result.getResultCode() >0) {
						ad.writeToPreferences(caller.getSharedPreferences("auth", MODE_PRIVATE));
						TextView tv = caller.findViewById(R.id.authresult);
						if (tv != null) tv.setText("");
						
						FetchData fd = new FetchData(resultObject);
						result.setFetchData(fd);
	
						Intent intent = new Intent(caller.getApplicationContext(),
								MainActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra(Constants.FETCHDATA_EXTRA,fd);
						intent.putExtra(Constants.AUTHDATA_EXTRA, ad);
						intent.putExtra(Constants.PROGRAMDATA_EXTRA, pd);
						caller.startActivity(intent);
					} else {
						TextView tv = caller.findViewById(R.id.authresult);
						if (tv != null) tv.setText(R.string.autherror);
					}
				}

			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			
		}
	}
}
