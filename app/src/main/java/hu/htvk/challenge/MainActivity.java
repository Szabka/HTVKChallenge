package hu.htvk.challenge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import hu.htvk.challenge.https.HttpConnector;
import hu.htvk.challenge.json.AuthData;
import hu.htvk.challenge.json.Checkpoint;
import hu.htvk.challenge.json.FetchData;
import hu.htvk.challenge.json.ProgramData;
import hu.htvk.challenge.json.Result;
import hu.htvk.challenge.json.Visit;
import hu.htvk.challenge.utils.Constants;
import okhttp3.FormBody;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback  {
    private ProgramData pd;
    private AuthData ad;

    private GoogleMap googleMap;
    private Polyline course;
    private Map<String, Marker> markerMap = new HashMap<>();
    private Map<String, Checkpoint> checkpointMap = new HashMap<>();
    private Set<String> visitedSet;
    private Checkpoint destination;
    private double currentLatitude = 0d;
    private double currentLongitude = 0d;

    private TextView destinationName;
    private TextView destinationDistance;
    private TextView destinationBearings;

    private Button checkin;

    @Override
    @SuppressLint("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMyLocationEnabled(true);

        createMarkers();

        this.googleMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();

            destination = checkpointMap.get(marker.getTitle());

            updateUIElements();

            return true;
        });
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(47.601153, 19.053082)));
        this.googleMap.moveCamera(CameraUpdateFactory.zoomTo(15f)); //10f
    }

    private void updateUIElements() {
        float[] resultArray = new float[3];
        Location.distanceBetween(
                currentLatitude,
                currentLongitude,
                destination.getPositionAsLatLong().latitude,
                destination.getPositionAsLatLong().longitude,
                resultArray
        );

        drawCourseLine();
        updateDestinationInfo(resultArray);
        enableCheckinButton(resultArray);
    }

    private void drawCourseLine() {
        if (course != null) {
            course.remove();
        }
        course = googleMap.addPolyline(new PolylineOptions()
                .clickable(false)
                .color(Color.BLUE)
                .geodesic(true)
                .width(7.0f)
                .add(
                    new LatLng(currentLatitude, currentLongitude),
                    new LatLng(destination.getPositionAsLatLong().latitude, destination.getPositionAsLatLong().longitude))
        );

        destinationName.setText(destination.getName());
    }

    private float[] updateDestinationInfo(float[] resultArray) {
        float dist = resultArray[0];
        if (dist > 1000.0f ){
            destinationDistance.setText(String.format("%.1f km", dist / 1000));
        } else {
            destinationDistance.setText(String.format("%.1f m", dist));
        }

        float bearing = resultArray[1] > 0 ? resultArray[1] : 360 + resultArray[1];
        destinationBearings.setText(String.format("%.0f°", Float.valueOf(bearing)));
        return resultArray;
    }

    private void enableCheckinButton(float[] resultArray) {
        if (resultArray[0] <= 50.0f && !visitedSet.contains(destination.getName())) {
            checkin.setEnabled(true);
        } else {
            checkin.setEnabled(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ad = getIntent().getParcelableExtra(Constants.AUTHDATA_EXTRA);
        pd = getIntent().getParcelableExtra(Constants.PROGRAMDATA_EXTRA);
        FetchData fd = getIntent().getParcelableExtra(Constants.FETCHDATA_EXTRA);

        if (pd != null){
            checkpointMap = pd.getCheckpoints().stream()
                .collect(Collectors.toMap(Checkpoint::getName, element -> element));
        }

        if (fd != null){
            visitedSet = fd.getVisits().stream().map(Visit::getCheckpointName).collect(Collectors.toSet());
        }

        String[] missingPermissions = getMissingPermissions();
        if (missingPermissions.length == 0){
            addMap();
            startLocationCheck();
        } else {
            ActivityCompat.requestPermissions(this, missingPermissions, 0);
        }

        destinationName = findViewById(R.id.destName);
        destinationDistance = findViewById(R.id.destDistance);
        destinationBearings = findViewById(R.id.destBearings);

        checkin = findViewById(R.id.chekin);
        checkin.setEnabled(false);

        checkin.setOnClickListener(v -> checkin());

        Button reload = findViewById(R.id.reload);
        reload.setOnClickListener(listener -> reload());
    }

    private String[] getMissingPermissions(){
        List<String> missingPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission((Context) this, "android.permission.ACCESS_COARSE_LOCATION") == -1){
            missingPermissions.add("android.permission.ACCESS_COARSE_LOCATION");
        }
        if (ContextCompat.checkSelfPermission((Context) this, "android.permission.ACCESS_FINE_LOCATION") == -1){
            missingPermissions.add("android.permission.ACCESS_FINE_LOCATION");
        }
        return missingPermissions.toArray(new String[]{});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if ((requestCode == 0) && (grantResults.length != 0)){
            addMap();
            startLocationCheck();
        } else {
            Toast toast = Toast.makeText(MainActivity.this, getString(R.string.permissionerror), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void addMap(){
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                .tiltGesturesEnabled(false)
                .zoomControlsEnabled(true);
        SupportMapFragment mapFragment = SupportMapFragment.newInstance(options);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.mapView, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    private void startLocationCheck(){
        FusedLocationProviderClient locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(4000L);
        locationRequest.setFastestInterval(2000L);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    currentLatitude = locationResult.getLastLocation().getLatitude();
                    currentLongitude = locationResult.getLastLocation().getLongitude();
                }

                //currentLatitude = 47.601153;
                //currentLongitude = 19.053082;

                if (destination != null) {
                    updateUIElements();
                }

                pd.getCheckpoints().forEach(checkpoint -> {
                    float[] results = new float[3];
                    LatLng latLng = checkpoint.getPositionAsLatLong();
                    Location.distanceBetween(currentLatitude, currentLongitude, latLng.latitude, latLng.longitude, results);

                    if (results[0] < 50.0f && checkpoint.getType() == 2 && !visitedSet.contains(checkpoint.getName())) {
                        String latLong = currentLatitude + "," + currentLongitude;
                        RequestBody body = new FormBody.Builder(StandardCharsets.UTF_8)
                                .add("op", "checkinloc")
                                .add("email", ad.getUserName())
                                .add("regcode", ad.getPassword())
                                .add("loc", latLong)
                                .build();

                        new checkinAsyncTask().execute(body);
                    }
                });
            }
        };

        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void checkin(){
        String latLong = currentLatitude + "," + currentLongitude;
        RequestBody body = new FormBody.Builder(StandardCharsets.UTF_8)
                .add("op", "checkinloc")
                .add("email", ad.getUserName())
                .add("regcode", ad.getPassword())
                .add("loc", latLong)
                .build();

        new checkinAsyncTask().execute(body);
    }

    private void reload() {
        RequestBody body = new FormBody.Builder(StandardCharsets.UTF_8)
                .add("email",ad.getUserName())
                .add("regcode",ad.getPassword())
                .add("op", "fetch")
                .build();

        new reloadDataAsyncTask().execute(body);
    }

    private class checkinAsyncTask extends AsyncTask<RequestBody, Integer, String> {

        @Override
        protected String doInBackground(RequestBody... params) {
            try {
                return HttpConnector.execute(getApplicationContext(), params[0]);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
                e.printStackTrace();
            }
            return "";
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
                    if (result.getResultCode() > 0) {
                        reload();
                        Toast toast = Toast.makeText(MainActivity.this, getString(R.string.upload_success), Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(MainActivity.this,
                                getString(R.string.upload_error),
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

    private class reloadDataAsyncTask extends AsyncTask<RequestBody, Integer, String> {

        @Override
        protected String doInBackground(RequestBody... params) {
            try {
                return HttpConnector.execute(MainActivity.this, params[0]);
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
                e.printStackTrace();
            }
            return "";
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
                    if (result.getResultCode() > 0) {
                        FetchData fd = new FetchData(resultObject);
                        visitedSet = fd.getVisits().stream().map(Visit::getCheckpointName).collect(Collectors.toSet());
                        markerMap.values().forEach(Marker::remove);
                        markerMap = new HashMap<>();
                        createMarkers();
                        Toast toast = Toast.makeText(MainActivity.this, getString(R.string.downloadSuccess), Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        Toast toast = Toast.makeText(MainActivity.this, getString(R.string.downloaderror), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), e.toString());
                e.printStackTrace();
            }
        }
    }

    private void createMarkers() {
        pd.getCheckpoints().forEach(element -> {
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(element.getPositionAsLatLong())
                    .icon(visitedSet.contains(element.getName())
                            ? BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            : BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(element.getName()));
            if (element.getName() != null && marker != null) {
                markerMap.put(element.getName(), marker);
            }
        });
    }
}
