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

    private String start_arr = "", dest_arr = "";
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

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Set this as your home or destination?")
                    .setPositiveButton("Home", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            start_arr = String.valueOf(point.latitude) + "," + String.valueOf(point.longitude);
                        }
                    })
                    .setNegativeButton("Destination", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dest_arr = String.valueOf(point.latitude) + "," + String.valueOf(point.longitude);
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(!(start_arr.equals("") || dest_arr.equals(""))) {
                try {
                    final HttpClient httpclient = new DefaultHttpClient();
                    final HttpPost httppost = new HttpPost("http://162.243.238.19:5000/add");
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    nameValuePairs.add(new BasicNameValuePair("name", "Umair"));
                    nameValuePairs.add(new BasicNameValuePair("start_arr", start_arr));
                    nameValuePairs.add(new BasicNameValuePair("dest_arr", dest_arr));
                    nameValuePairs.add(new BasicNameValuePair("email", userEmail));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    httpclient.execute(httppost);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if(start_arr.equals("") && dest_arr.equals("")) {
                    Toast.makeText(getActivity(), "Please select you home and destination.", Toast.LENGTH_SHORT).show();
                } else {
                    if(start_arr.equals("")) {
                        Toast.makeText(getActivity(), "Please select your home.", Toast.LENGTH_SHORT).show();
                    }
                    if(dest_arr.equals("")) {
                        Toast.makeText(getActivity(), "Please select your destination.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
