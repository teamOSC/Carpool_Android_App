package in.osc.carpool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import in.osc.carpool.utils.UserEmailFetcher;

/**
 * Created by omerjerk on 5/1/14.
 */
public class LocationChooserFragment extends Fragment {

    private static View rootView;
    private GoogleMap mMap;
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

        mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                //TODO: Send selected latlong to server
                Toast.makeText(getActivity(), "Map clicked!", Toast.LENGTH_SHORT).show();
                String userEmail = UserEmailFetcher.getEmail(getActivity());
                new FireMissilesDialogFragment(point, userEmail).show(getActivity().getSupportFragmentManager(), "LocationConfirmDialog");

            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public class FireMissilesDialogFragment extends DialogFragment {

        LatLng point;
        String userEmail;
        public FireMissilesDialogFragment(LatLng point, String userEmail) {
            this.point = point;
            this.userEmail = userEmail;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final HttpClient httpclient = new DefaultHttpClient();
            final HttpPost httppost = new HttpPost("http://www.yoursite.com/script.php");

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Set this as your home or destination?")
                    .setPositiveButton("Home", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                                nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(point.latitude)));
                                nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(point.longitude)));
                                nameValuePairs.add(new BasicNameValuePair("type", "home"));
                                nameValuePairs.add(new BasicNameValuePair("email", userEmail));
                                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                                // Execute HTTP Post Request
                                HttpResponse response = httpclient.execute(httppost);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    })
                    .setNegativeButton("Destination", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                                nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(point.latitude)));
                                nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(point.longitude)));
                                nameValuePairs.add(new BasicNameValuePair("type", "destination"));
                                nameValuePairs.add(new BasicNameValuePair("email", userEmail));
                                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                                // Execute HTTP Post Request
                                HttpResponse response = httpclient.execute(httppost);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
}
