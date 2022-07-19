package hu.htvk.challenge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.*;
import com.google.android.gms.location.*;

import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hu.htvk.challenge.https.HttpConnector;
import hu.htvk.challenge.json.AuthData;
import hu.htvk.challenge.json.Checkpoint;
import hu.htvk.challenge.json.FetchData;
import hu.htvk.challenge.json.ProgramData;
import hu.htvk.challenge.json.Result;
import hu.htvk.challenge.json.Visit;
import hu.htvk.challenge.utils.CheckpointComparator;
import hu.htvk.challenge.utils.Constants;
import hu.htvk.challenge.utils.ScreenUtils;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class CheckpointListActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {
    public static final int REQUEST_START_LOCATION_FIX = 10001;
    public static final int REQUEST_CHECK_SETTINGS = 20001;
    private static final String GMS_ID = "GMS";

    private ProgramData pd;
    private AuthData ad;
    private FetchData fd;


    private GoogleApiClient client;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ad = getIntent().getParcelableExtra(Constants.AUTHDATA_EXTRA);
        pd = getIntent().getParcelableExtra(Constants.PROGRAMDATA_EXTRA);
        fd = getIntent().getParcelableExtra(Constants.FETCHDATA_EXTRA);

        setContentView(R.layout.activity_checkpoint_list);

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(4000L);
        mLocationRequest.setFastestInterval(2000L);

        client = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
        client.connect();
        Log.d(getClass().getName(), "init end.");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(getClass().getName(), "connected.");
        LocationSettingsRequest request = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).build();
        LocationServices.SettingsApi.checkLocationSettings(client, request).setResultCallback(settingsResultCallback);
    }

    private ResultCallback<LocationSettingsResult> settingsResultCallback = new ResultCallback<LocationSettingsResult>() {
        @Override
        public void onResult(LocationSettingsResult locationSettingsResult) {
            final Status status = locationSettingsResult.getStatus();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    Log.d(getClass().getName(), "All location settings are satisfied.");
                    if (ActivityCompat.checkSelfPermission(CheckpointListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(CheckpointListActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(CheckpointListActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},0);
                        return;
                    }
                    LocationServices.FusedLocationApi.requestLocationUpdates(client, mLocationRequest, CheckpointListActivity.this).setResultCallback(CheckpointListActivity.this);
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    Log.e(getClass().getName(),"Location settings are not satisfied. Show the user a dialog to" +
                            "upgrade location settings. You should hook into the Activity onActivityResult and call this provider onActivityResult method for continuing this call flow. ");

                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        status.startResolutionForResult((Activity) CheckpointListActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        Log.d(getClass().getName(),"PendingIntent unable to execute request.");
                    }

                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    Log.d(getClass().getName(),"Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                    break;
            }
        }
    };

    // client status handle
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(getClass().getName(),"Connection suspended. "+i);
    }

    // client status handle
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(getClass().getName(),"Connection failed. "+connectionResult);
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.d(getClass().getName(),"Locations update request successful");

        } else if (status.hasResolution()) {
            Log.e(getClass().getName(),
                    "Unable to register, but we can solve this - will startActivityForResult. You should hook into the Activity onActivityResult and call this provider onActivityResult method for continuing this call flow.");
            try {
                status.startResolutionForResult(this, REQUEST_START_LOCATION_FIX);
            } catch (IntentSender.SendIntentException e) {
                Log.e(getClass().getName(), "problem with startResolutionForResult",e);
            }
        } else {
            // No recovery. Weep softly or inform the user.
            Log.e(getClass().getName(),"Registering failed: " + status.getStatusMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(getClass().getName(),"onLocationChanged to:"+location.getLatitude()+", "+location.getLongitude());
        TextView ltv = ((TextView)findViewById(R.id.tvcplheader));
        ltv.setText(location.getLatitude()+", "+location.getLongitude());
        ImageView iv = (ImageView) findViewById(R.id.tvcplarrow);

        Set<Integer> vcpid = new HashSet<Integer>();

        for (Visit cpv : fd.getVisits()) {
            vcpid.add(cpv.getCheckpointId());
        }

        ArrayList<Checkpoint> gcpl = new ArrayList<Checkpoint>();
        for (Checkpoint cp: pd.getCheckpoints()) {
            if (cp.getType()==2&&cp.getLocation()!=null&&!vcpid.contains(cp.getId())) {
                // gps tipusu, van koordinataja es meg nem latogatott
                gcpl.add(cp);
            }
        }
        Collections.sort(gcpl,new CheckpointComparator(location));

        if (gcpl.size()>0) {
            if (ScreenUtils.calcDistance(gcpl.get(0).getLocation(),location)<50) {
                // needs check in.
                checkLocation(location);
                LocationServices.FusedLocationApi.removeLocationUpdates(client, this);
            } else { // van elem.
                Location nearest = ScreenUtils.getCpLocation(gcpl.get(0));
                float bearingdegree = location.bearingTo(nearest);
                if (location.hasBearing()) {
                    float diffBearing = location.getBearing()-bearingdegree;
                    iv.setRotation(diffBearing);
                }
                refreshListView(gcpl,location);
            }
        } else {
            refreshListView(gcpl,location);
        }
    }

    protected void refreshListView(ArrayList<Checkpoint> gcpl,Location l) {
        LinearLayout ll = (LinearLayout) findViewById(R.id.cpll1);
        int cc = ll.getChildCount();
        int icc=0;
        for (Checkpoint v : gcpl) {
            DecimalFormat decFormat = new DecimalFormat("#.00");
            String diststr = decFormat.format(ScreenUtils.calcDistance(v.getLocation(),l));
            if (icc<cc) {
                View childAt = ll.getChildAt(icc);
                if (childAt.getId()!=v.getId()) {
                    Log.d(getClass().getName(), "View child id mismatch!! "+childAt.getId()+"<>"+v.getId() );
                } else if (childAt instanceof TextView) {
                    TextView tv = (TextView) childAt;
                    tv.setText(v.getName()+" \n"+v.getLocation()+" \n"+diststr+" m\n");
                }
            } else {
                TextView tv = new TextView(this);
                tv.setText(v.getName()+" \n"+v.getLocation()+" \n"+diststr+" m\n");
                tv.setId(v.getId());
                tv.setTextColor(getResources().getColor(R.color.black));

//                DisplayMetrics metrics = getResources().getDisplayMetrics();
//				float dp = 15f;
//				float fpixels = metrics.density * dp;
//				int pixels = (int) (fpixels + 0.5f);
                tv.setTextSize(30);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 20);
//				params.gravity=Gravity.LEFT;
                tv.setLayoutParams(params);
                ll.addView(tv);
            }
            icc++;
        }
    }
    private void checkLocation(Location l) {
        RequestBody body = new FormBody.Builder(StandardCharsets.UTF_8)
                .add("op", "checkinloc")
                .add("email", ad.getUserName())
                .add("regcode", ad.getPassword())
                .add("loc", l.getLatitude()+", "+l.getLongitude())
                .build();

        try {
            new Comm1AsynchTask().execute(body);
        } catch (Exception e) {
            Log.e(getClass().getName(), e.toString());
            e.printStackTrace();
        }
    }

    private class Comm1AsynchTask extends AsyncTask<RequestBody , Integer, String> {

        @Override
        protected String doInBackground(RequestBody... params) {
            try{
                String ret = HttpConnector.execute(getApplicationContext(),
                        params[0]);

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
                hu.htvk.challenge.json.Result result = new Result();
                if ( ret != null && ret.length() > 0 ) {
                    JSONObject resultObject = new JSONObject(ret);
                    result.setResultCode(resultObject.getInt(Constants.TAG_RESULTCODE));
                    Log.d(getClass().getName(), "" + result.getResultCode());
                    if (result.getResultCode() >0) {
                        FetchData fd = new FetchData(resultObject);
                        result.setFetchData(fd);

                        Intent intent = new Intent(getApplicationContext(),SzekcioListaActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(Constants.FETCHDATA_EXTRA,fd);
                        intent.putExtra(Constants.AUTHDATA_EXTRA, ad);
                        startActivity(intent);
                    } else {
                        Toast toast = Toast.makeText(CheckpointListActivity.this,
                                getString(R.string.upload_error),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
                e.printStackTrace();
//			} finally {
//				scanButton.setVisibility(View.VISIBLE);
            }

        }

    }
}
