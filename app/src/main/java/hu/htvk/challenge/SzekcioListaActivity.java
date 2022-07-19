package hu.htvk.challenge;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import hu.htvk.challenge.https.HttpConnector;
import hu.htvk.challenge.json.AuthData;
import hu.htvk.challenge.json.ProgramData;
import hu.htvk.challenge.json.FetchData;
import hu.htvk.challenge.json.Result;
import hu.htvk.challenge.json.Visit;
import hu.htvk.challenge.utils.Constants;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class SzekcioListaActivity extends Activity {

	private ProgramData pd;
	private AuthData ad;
	private FetchData fd;
//	ProgressDialog progress;
//	Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.szekcioeloadaslista);
		
		ad = getIntent().getParcelableExtra(Constants.AUTHDATA_EXTRA);
		pd = getIntent().getParcelableExtra(Constants.PROGRAMDATA_EXTRA);
		fd = getIntent().getParcelableExtra(Constants.FETCHDATA_EXTRA);

		Button btnLogout = (Button) findViewById(R.id.buttonexit);
		btnLogout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getSharedPreferences("auth", MODE_PRIVATE).edit().clear().apply();
				SzekcioListaActivity.this.finish();
//				Intent intent = new Intent(getApplicationContext(),	LoginActivity.class);
//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				startActivity(intent);

				//Process.killProcess(Process.myPid());
			}
		});
		Button btnRefresh = (Button) findViewById(R.id.refresh);
		btnRefresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fireFetchData();
			}
		});
		Button btnScan = (Button) findViewById(R.id.scan);
		btnScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(),	QrReaderActivity.class);
				intent.putExtra(Constants.AUTHDATA_EXTRA, ad);
				intent.putExtra(Constants.PROGRAMDATA_EXTRA, pd);
				startActivity(intent);
			}
		});
		
		Button btnCPList = (Button) findViewById(R.id.cplist);
		btnCPList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(),	CheckpointListActivity.class);
				intent.putExtra(Constants.AUTHDATA_EXTRA, ad);
				intent.putExtra(Constants.PROGRAMDATA_EXTRA, pd);
				intent.putExtra(Constants.FETCHDATA_EXTRA,fd);
				startActivity(intent);
			}
		});


		TextView headtv = ((TextView)findViewById(R.id.tvheader));
		headtv.setText(fd.getShipName()+" \n"+fd.getUserName());
		
		refreshListView(fd);
	}

	protected void refreshListView(FetchData fd) {
		LinearLayout ll = (LinearLayout) findViewById(R.id.ll1);
		int cc = ll.getChildCount();
		int icc=0;
		for (Visit v : fd.getVisits()) {
			if (icc<cc) {
				View childAt = ll.getChildAt(icc);
				if (childAt.getId()!=v.getId()) {
					Log.d(getClass().getName(), "View child id mismatch!! "+childAt.getId()+"<>"+v.getId() );
				}
			} else {
				TextView tv = new TextView(this);
				tv.setText(v.getCheckpointName()+" \n"+v.getVisitorName()+" \n"+v.getWhen());
				tv.setId(v.getId());
				tv.setTextColor(getResources().getColor(R.color.black));
				
				DisplayMetrics metrics = getResources().getDisplayMetrics();
//				float dp = 15f;
//				float fpixels = metrics.density * dp;
//				int pixels = (int) (fpixels + 0.5f);
				tv.setTextSize(30);
				LayoutParams params = new LayoutParams(
						LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT);
				params.setMargins(0, 0, 0, 20);
//				params.gravity=Gravity.LEFT;
				tv.setLayoutParams(params);
				ll.addView(tv);
			}
			icc++;	
		}
	}
	
	public void fireFetchData() {
		RequestBody body = new FormBody.Builder(Charset.forName("UTF-8"))
				.add("email",ad.getUserName())
				.add("regcode",ad.getPassword())
				.add("op", "fetch")
				.build();

		new Comm1AsynchTask().execute(body);
	}

	private class Comm1AsynchTask extends
			AsyncTask<RequestBody, Integer, String> {
		

		@Override
		protected String doInBackground(RequestBody... params) {
			try {
				String ret = HttpConnector.execute(getApplicationContext(),	params[0]);

				return ret;
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String ret) {
			try{
				Log.d(getClass().getName(), ret);
				Result result = new Result();
				if (ret.length() > 0) {
					JSONObject resultObject = new JSONObject(ret);
					result.setResultCode(resultObject.getInt(Constants.TAG_RESULTCODE));
					Log.d(getClass().getName(), "" + result.getResultCode());
					if (result.getResultCode() >0) {
						FetchData fd = new FetchData(resultObject);
						refreshListView(fd);
					} else {
						Toast toast = Toast.makeText(SzekcioListaActivity.this,
								getString(R.string.downloaderror),
								Toast.LENGTH_SHORT);
						toast.show();
					}
				}
			} catch (Exception e) {
				Log.e(getClass().getName(), e.toString());
				e.printStackTrace();
			}
		}

	}


}
