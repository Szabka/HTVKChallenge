package hu.htvk.challenge;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;
import hu.htvk.challenge.https.HttpConnector;
import hu.htvk.challenge.json.AuthData;
import hu.htvk.challenge.json.FetchData;
import hu.htvk.challenge.json.Result;
import hu.htvk.challenge.json.ProgramData;
import hu.htvk.challenge.utils.Constants;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class LoginActivity extends Activity {

	
	public static int SELECTED_EVENT_ID = -1;

//	LayoutInflater inflater;
//	ExpandableListAdapter adapter;
	Button btnLogin;
	ProgramData programdata;
//	FetchData events;
	
	static LoginActivity INSTANCE;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		INSTANCE = this;

		setContentView(R.layout.login);

		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnLogin.setOnClickListener(new OnClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onClick(View v) {
				new Comm2AsynchTask(programdata,new AuthData(((TextView) findViewById(R.id.usernameinput)).getText()
						.toString(),((TextView) findViewById(R.id.passwordinput)).getText()
						.toString()),LoginActivity.this).execute();
				
			}
		});

		new TitleLoadAsynchTask(LoginActivity.this).execute();

	}

	public static class TitleLoadAsynchTask extends AsyncTask<Object, Integer, String> {
		LoginActivity caller;
		
		public TitleLoadAsynchTask(LoginActivity caller) {
			this.caller=caller;
		}

		@Override
		protected String doInBackground(Object... params) {
			try{
				RequestBody body = new FormBody.Builder(Charset.forName("UTF-8"))
						.add("op", "title").build();

				String ret = HttpConnector.execute(caller.getApplicationContext(),
						body);
				
				return ret;
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String ret) {
			try {
				Log.d(getClass().getName(), "Response is: "+ret);
				if (ret!=null&&ret.length() > 0) {
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
	
	
	public static class Comm2AsynchTask extends AsyncTask<Object, Integer, String> {
		ProgramData pd;
	    AuthData ad;
		Activity caller;
		
		public Comm2AsynchTask(ProgramData pd,AuthData authData,Activity caller) {
		    this.pd = pd;
			ad = authData;
			this.caller=caller;
		}

		@Override
		protected String doInBackground(Object... params) {
			try{
				RequestBody body = new FormBody.Builder(Charset.forName("UTF-8"))
						.add("email",ad.getUserName())
						.add("regcode",ad.getPassword())
						.add("op", "fetch")
						.build();

				String ret = HttpConnector.execute(caller.getApplicationContext(),
						body);
				
				return ret;
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String ret) {
			try {
				Result result = new Result();
				Log.d(getClass().getName(), "Response is: "+ret);
				if (ret!=null&&ret.length() > 0) {
					JSONObject resultObject = new JSONObject(ret);
					result.setResultCode(resultObject.getInt(Constants.TAG_RESULTCODE));
					Log.d(getClass().getName(), "" + result.getResultCode());
					if (result.getResultCode() >0) {
						ad.writeToPreferences(caller.getSharedPreferences("auth", MODE_PRIVATE));
						TextView tv = (TextView) caller.findViewById(R.id.authresult);
						if (tv!=null) tv.setText("");
						
						FetchData fd = new FetchData(resultObject);
						result.setFetchData(fd);
	
						Intent intent = new Intent(caller.getApplicationContext(),
								SzekcioListaActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						intent.putExtra(Constants.FETCHDATA_EXTRA,fd);
						intent.putExtra(Constants.AUTHDATA_EXTRA, ad);
						intent.putExtra(Constants.PROGRAMDATA_EXTRA, pd);
						caller.startActivity(intent);
					} else {
						TextView tv = (TextView) caller.findViewById(R.id.authresult);
						if (tv!=null) tv.setText(R.string.autherror);
					}
				}

			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			
		}

		
	}
	
	static class CustomArrayAdapter<T> extends ArrayAdapter<T>
	{
	    public CustomArrayAdapter(Context ctx, T [] objects)
	    {
	        super(ctx, android.R.layout.simple_spinner_item, objects);
	    }



	    @Override
	    public View getDropDownView(int position, View convertView, ViewGroup parent)
	    {
	        View view = super.getView(position, convertView, parent);

	   
	            TextView text = (TextView)view.findViewById(android.R.id.text1);
	            text.setTextColor(Color.BLACK);         

	        return view;

	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    
	    	
	    	View v =  super.getView(position, convertView, parent);
	    	TextView text = (TextView)v.findViewById(android.R.id.text1);
	        text.setTextColor(Color.BLACK);       
	        return v;
	    }
	    
	}
	

}
