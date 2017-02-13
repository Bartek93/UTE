package pw.edu.ute;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String LOG_TAG = "MAP_ACTIVITY_LOG";

    private static final String API_KEY_DANE_PO_WARSZAWSKU = "0c934d94-a056-4143-babe-09326e3e0383";
    private static final String ID_NIERUCHOMOSC_WYNAJEM_DANE_PO_WARSZAWSKU = "45ba10ab-6562-49ce-b572-6c9b999464d6";
    private  static final int RADIUS = 2000; // in meter

    private GoogleMap mMap;

    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        Log.d(LOG_TAG, "GOT latitude = " + latitude);
        Log.d(LOG_TAG, "GOT longitude = " + longitude);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        RequestQueue queue = Volley.newRequestQueue(this);

        System.out.println("ON MAP READY latitude = " + latitude);
        System.out.println("ON MAP READY longitude = " + longitude);

        /*
        String url = "https://api.um.warszawa.pl/api/action/wfsstore_get/?" +
                "id=" + ID_NIERUCHOMOSC_WYNAJEM_DANE_PO_WARSZAWSKU +
                "&circle=20.970,52.276,2000" +
                "&apikey=" + API_KEY_DANE_PO_WARSZAWSKU;*/

        String url = "https://api.um.warszawa.pl/api/action/wfsstore_get/?" +
                "id=" + ID_NIERUCHOMOSC_WYNAJEM_DANE_PO_WARSZAWSKU +
                "&circle=" + longitude + "," + latitude + "," + RADIUS +
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
                                String longitude = arr.getJSONObject(i).getString("longitude");

                                LatLng pos = new LatLng(Float.parseFloat(latitude),Float.parseFloat(longitude));
                                mMap.addMarker(new MarkerOptions().position(pos));
                            }
                        } catch (JSONException exc) {
                            Log.e(LOG_TAG, "JSON Escpetion", exc);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Error Response", error);
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        LatLng myPosition = new LatLng(52.276,20.970);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
        mMap.animateCamera( CameraUpdateFactory.zoomTo( 12.0f ) );

//        LatLng pos = new LatLng(52.277, 20.957);
//        mMap.addMarker(new MarkerOptions().position(pos));
//        pos = new LatLng(52.270, 20.930);
//        mMap.addMarker(new MarkerOptions().position(pos));
//        LatLng myPosition = new LatLng(52.276,20.970);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
//        mMap.animateCamera( CameraUpdateFactory.zoomTo( 12.0f ) );
    }
}
