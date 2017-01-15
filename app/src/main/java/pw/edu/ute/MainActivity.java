package pw.edu.ute;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<Status> {

    private static final String API_KEY_DANE_PO_WARSZAWSKU = "0c934d94-a056-4143-babe-09326e3e0383";
    private static final String ID_NIERUCHOMOSC_WYNAJEM_DANE_PO_WARSZAWSKU = "45ba10ab-6562-49ce-b572-6c9b999464d6";


    private static final String LOG_TAG = "MAIN_ACTIVITY_LOG";
    private static final int PERM_ACCESS_FINE_LOCATION = 1;
    private static final int PERM_ACCESS_COARSE_LOCATION = 2;

    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;

    private TextView mLatitudeTxt;
    private TextView mLongitudeTxt;
    private TextView mStatusTxt;
    protected TextView mLatitudeLabel;
    protected TextView mLongitudeLabel;

    private Button mRequestActivityBtn;
    private Button mRemoveActivityBtn;
    private Button button;

    private int permissionFineLocation;
    private int permissionCoarseLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        accessPermission();
        buildGoogleApiClient();
        buildView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "Successfully connected");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null) {
            mLatitudeTxt.setText(String.valueOf(mLastLocation.getLatitude()));
            mLongitudeTxt.setText(String.valueOf(mLastLocation.getLongitude()));
        }

        //TODO dlaczego nie wyświetla wyników dla PRIORITY_BALANCED_POWER_ACCURACY - battery friendly
        // TODO uzależnić interwał od activity recognition
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 60 * 1000)
                .setFastestInterval(60 * 1000); // passive listen from other apps in smartphone - not cost extra power

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "Connection failed: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, "Location changed: " + location.toString());

        mLatitudeTxt.setText(Double.toString(location.getLatitude()));
        mLongitudeTxt.setText(Double.toString(location.getLongitude()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERM_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO permission was granted, yay! Do the task you need to do.
                } else {
                    // TODO ermission denied, boo! Disable the functionality that depends on this permission.
                }
                return;
            }
            case PERM_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

    //TODO synchronized?
    //TODO wybrać jaka dokładność jest nam potrzebna, wystarczy chyba Coarse Location
    private void accessPermission() {
        permissionFineLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissionCoarseLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);

       // Cell, WIFI and GPS
        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERM_ACCESS_FINE_LOCATION);
        }

        //Cell and WIFI
        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERM_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onResult(Status status) {
        if(status.isSuccess()){
            Log.e(LOG_TAG, "Successfully requesting/removing activity detection.");
        } else {
            Log.e(LOG_TAG, "Error requesting or removing activity detection: " + status.getStatusMessage());
        }
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {

        private static final String TAG = "RECEIVER";

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> detectedActivities = intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);

            String status = "";
            for (DetectedActivity activity : detectedActivities) {
                int type = activity.getType();
                int confidence = activity.getConfidence();
                status += getActivityString(type) + confidence + "%\n";
            }

            mStatusTxt.setText(status);
        }
    }

    public void requestActivityUpdatesButtonHandler(View view) {
        if(!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                1000,
                getActivityDetectionPendingIntent()).setResultCallback(this);
        mRequestActivityBtn.setEnabled(false);
        mRemoveActivityBtn.setEnabled(true);
    }

    public void removeActivityUpdatesButtonHandler(View view) {
        if(!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()).setResultCallback(this);
        mRequestActivityBtn.setEnabled(true);
        mRemoveActivityBtn.setEnabled(false);
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public String getActivityString(int detectedActivityType) {
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on bicicle";
            case DetectedActivity.ON_FOOT:
                return "on foot";
            case DetectedActivity.RUNNING:
                return "running";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            case DetectedActivity.WALKING:
                return "walking";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            default:
                return "unidentifiable activity";
        }
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void buildView() {
        mLatitudeTxt = (TextView) findViewById(R.id.latitude_txt);
        mLongitudeTxt = (TextView) findViewById(R.id.longitude_txt);
        mLatitudeLabel = (TextView) findViewById(R.id.latitude_label);
        mLongitudeLabel = (TextView) findViewById(R.id.longitude_label);

        mStatusTxt = (TextView) findViewById(R.id.status_txt);
        mRequestActivityBtn = (Button) findViewById(R.id.request_activity_updates_button);
        mRemoveActivityBtn = (Button) findViewById(R.id.remove_activity_updates_button);
        button = (Button) findViewById(R.id.button);
    }

    //longitude = 20.xx, latitude = 52.xx mniej więcej
    public String getJSONObjectFromURL(String longitude, String latitude, String radius) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "https://api.um.warszawa.pl/api/action/wfsstore_get/?" +
                "id=" + ID_NIERUCHOMOSC_WYNAJEM_DANE_PO_WARSZAWSKU +
                "&circle=" + longitude + "," + latitude + "," + radius +
                "&apikey=" + API_KEY_DANE_PO_WARSZAWSKU;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONObject result = obj.getJSONObject("result");

                            JSONArray arr = result.getJSONArray("featureMemberCoordinates");
                            for (int i = 0; i < arr.length(); i++) {
                                String latitude = arr.getJSONObject(i).getString("latitude");
                                System.out.println("latitude = " + latitude);
                                String longitude = arr.getJSONObject(i).getString("longitude");
                                System.out.println("longitude = " + longitude);
                            }
                        } catch (JSONException exc) {
                            Log.e(LOG_TAG, "That didn't work!", exc);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        return "";
    }

    //Tylko do testu na przycisk Button
    public String getJSONObjectFromURL_Button(View view) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "https://api.um.warszawa.pl/api/action/wfsstore_get/?" +
                "id=" + ID_NIERUCHOMOSC_WYNAJEM_DANE_PO_WARSZAWSKU +
                "&circle=20.970,52.276,2000" +
                "&apikey=" + API_KEY_DANE_PO_WARSZAWSKU;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONObject result = obj.getJSONObject("result");

                            JSONArray arr = result.getJSONArray("featureMemberCoordinates");
                            for (int i = 0; i < arr.length(); i++) {
                                String latitude = arr.getJSONObject(i).getString("latitude");
                                System.out.println("latitude = " + latitude);
                                String longitude = arr.getJSONObject(i).getString("longitude");
                                System.out.println("longitude = " + longitude);
                            }
                        } catch (JSONException exc) {
                            Log.e(LOG_TAG, "That didn't work!", exc);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        return "";
    }
}


