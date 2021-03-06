package in.osc.carpool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import in.osc.carpool.utils.PlaceProvider;
import in.osc.carpool.utils.UserEmailFetcher;

/**
 * Created by omerjerk on 5/1/14.
 */
public class LocationChooserFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private static View rootView;
    private GoogleMap mMap;

    private String start_lat = "", start_lon = "", dest_lat = "", dest_lon = "";

    private Context context;

    LocationClient mLocationClient;
    Location mCurrentLocation = null;

    private static final String TAG = "LocationChooserFragment";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LocationChooserFragment newInstance(int sectionNumber) {
        LocationChooserFragment fragment = new LocationChooserFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public LocationChooserFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null)
                parent.removeView(rootView);
        }
        try {
            rootView = inflater.inflate(R.layout.fragment_main, container, false);
        } catch (InflateException e) {
        /* map is already there, just return view as it is */
        }

        context = getActivity();

        mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                String userEmail = UserEmailFetcher.getEmail(getActivity());
                new LocationConfirmDialogFragment(point, userEmail).show(getActivity().getSupportFragmentManager(), "LocationConfirmDialog");
            }
        });

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        mLocationClient = new LocationClient(context, this, this);

        mLocationClient.connect();
        return rootView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.location_chooser, menu);

        MenuItem searchItem = menu.findItem(R.id.search);

        SearchView searchView = (SearchView) searchItem.getActionView();
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        if(null!=searchManager ) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        }
        searchView.setQueryHint("Search...");
        searchView.setIconifiedByDefault(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void performSearch(String query) {
        Log.d(TAG, "performSearch() called");
        Bundle data = new Bundle();
        data.putString("query", query);
        getActivity().getSupportLoaderManager().restartLoader(0, data, this);
    }

    public void getPlace(String query){
        Bundle data = new Bundle();
        data.putString("query", query);
        getActivity().getSupportLoaderManager().restartLoader(1, data, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle query) {
        CursorLoader cLoader = null;
        if(arg0==0) {
            cLoader = new CursorLoader(getActivity(), PlaceProvider.SEARCH_URI, null, null, new String[]{ query.getString("query") }, null);
        } else if(arg0==1) {
            Log.d(TAG, "onCreateLoader() = " + query.getString("query"));
            cLoader = new CursorLoader(getActivity(), PlaceProvider.DETAILS_URI, null, null, new String[]{ query.getString("query") }, null);
        }
        return cLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
        showLocations(c);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
    }

    private void showLocations(Cursor c){
        MarkerOptions markerOptions;
        LatLng position = null;
        mMap.clear();
        while(c.moveToNext()){
            markerOptions = new MarkerOptions();
            position = new LatLng(Double.parseDouble(c.getString(1)),Double.parseDouble(c.getString(2)));
            markerOptions.position(position);
            markerOptions.title(c.getString(0));
            mMap.addMarker(markerOptions);
        }
        if(position!=null){
            CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngZoom(position, 13.0f);
            mMap.animateCamera(cameraPosition);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "CONNECTED");
        mCurrentLocation = mLocationClient.getLastLocation();
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LatLng tempLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tempLatLng, 13.0f));
                }
            });
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "DISCONNECTED");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "CONNECTION FAILED");
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    public class LocationConfirmDialogFragment extends DialogFragment {

        LatLng point;
        String userEmail;
        public LocationConfirmDialogFragment(LatLng point, String userEmail) {
            this.point = point;
            this.userEmail = userEmail;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Set this as your home or destination?")
                    .setPositiveButton("Home", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            start_lat = String.valueOf(point.latitude);
                            start_lon = String.valueOf(point.longitude);
                        }
                    })
                    .setNegativeButton("Destination", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dest_lat = String.valueOf(point.latitude);
                            dest_lon = String.valueOf(point.longitude);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(!(start_lat.equals("") || dest_lat.equals(""))) {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        try{
                            //make an http get request
                            HttpClient client = new DefaultHttpClient();
                            String uri = "http://162.243.238.19:5000/add?";
                            uri += URLEncoder.encode("name=" + "Umairs", "UTF-8");
                            uri += URLEncoder.encode("&start_arr=" + start_lat + "," + start_lon, "UTF-8");;
                            uri += URLEncoder.encode("&dest_arr=" + dest_lat + "," + dest_lon, "UTF-8");
                            uri += URLEncoder.encode("&email=" + userEmail, "UTF-8");
                            Log.d(TAG, "Making request to this = " + uri);
                            URI website = new URI(uri);
                            HttpGet request = new HttpGet();
                            request.setURI(website);
                            HttpResponse response = client.execute(request);
                            HttpEntity entity = response.getEntity();
                            String result = EntityUtils.toString(entity);
                            //Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Response == " + result);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {

                        //write the location coordiantes to SharedPreferences
                        SharedPreferences settings = context.getSharedPreferences("MAIN", 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("start_latitude", start_lat);
                        editor.putString("start_longitude", start_lon);
                        editor.putString("dest_latitude", dest_lat);
                        editor.putString("dest_longitude", dest_lon);
                        editor.commit();
                    }
                }.execute();
            } else {
                if(start_lat.equals("") && dest_lat.equals("")) {
                    Toast.makeText(getActivity(), "Please select you home and destination.", Toast.LENGTH_SHORT).show();
                } else {
                    if(start_lat.equals("")) {
                        Toast.makeText(getActivity(), "Please select your home.", Toast.LENGTH_SHORT).show();
                    }
                    if(dest_lat.equals("")) {
                        Toast.makeText(getActivity(), "Please select your destination.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
