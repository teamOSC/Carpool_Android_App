package in.osc.carpool;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by omerjerk on 12/1/14.
 */
public class CarpoolSearchFragment extends Fragment {

    private static final String TAG = "CarpoolSearchFragment";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static CarpoolSearchFragment newInstance(int sectionNumber) {
        CarpoolSearchFragment fragment = new CarpoolSearchFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public CarpoolSearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_carpool, container, false);
        new CalculateCarpools().execute();
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
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        inflater.inflate(R.menu.carpool_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        if(item.getItemId() == R.id.refresh_carpool_database) {
            new RefreshCarpoolDatabase().execute();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    private class CalculateCarpools extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            String jsonString;

            try {
                File cacheFile = new File(getActivity().getFilesDir(), "data.json");

                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                jsonString = br.readLine();
                Log.d(TAG, "json output = " + jsonString);

                JSONArray mJsonArray = new JSONArray(jsonString);

                ArrayList<JSONObject> dataList = new ArrayList<JSONObject>();
                for(int i =0; i < mJsonArray.length(); ++i) {
                    JSONObject mJsonObject = mJsonArray.getJSONObject(i);
                    dataList.add(mJsonObject);
                }

                for (JSONObject mJSONObject : dataList) {
                    String[] friendStartPos = mJSONObject.getString("start_arr").split(",");
                    String[] friendDestPos = mJSONObject.getString("dest_arr").split(",");
                    SharedPreferences settings = getActivity().getSharedPreferences("MAIN", 0);
                    double homeStartLat = Double.parseDouble(settings.getString("start_latitude", "0.0"));
                    double homeStartLon = Double.parseDouble(settings.getString("start_longitude", "0.0"));
                    double homeDestLat = Double.parseDouble(settings.getString("dest_latitude", "0.0"));
                    double homeDestLon = Double.parseDouble(settings.getString("dest_longitude", "0.0"));
                    float[] distanceBetweenStart = new float[1];
                    float[] distanceBetweenDest = new float[1];
                    Location.distanceBetween(Double.parseDouble(friendStartPos[0]),
                            Double.parseDouble(friendStartPos[1]),
                            homeStartLat, homeStartLon, distanceBetweenStart);
                    Location.distanceBetween(Double.parseDouble(friendDestPos[0]),
                            Double.parseDouble(friendDestPos[1]),
                            homeDestLat, homeDestLon, distanceBetweenDest);

                    Log.d(TAG, "distanceBetweenStart = " + distanceBetweenStart[0]/1000 + " km");
                    Log.d(TAG, "distanceBetweenDest = " + distanceBetweenDest[0]/1000 + " km");
                }

            } catch (Exception e) {
                new RefreshCarpoolDatabase() {
                    @Override
                    protected void onPostExecute (String result) {
                        super.onPostExecute(result);
                        new CalculateCarpools().execute();
                    }
                }.execute();
                e.printStackTrace();

            }
            return null;
        }
    }

    private class RefreshCarpoolDatabase extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            String result = "";
            try {
                HttpClient client = new DefaultHttpClient();
                String uri = "http://162.243.238.19/sauravtom/carpool.json";
                URI website = new URI(uri);
                HttpGet request = new HttpGet();
                request.setURI(website);
                HttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("[GET RESPONSE]", result);
            File cacheFile = new File(getActivity().getFilesDir(), "data.json");

            BufferedWriter bw = null;
            try {
                if (!cacheFile.exists()) {
                    cacheFile.createNewFile();
                }

                FileWriter fw = new FileWriter(cacheFile.getAbsoluteFile());
                bw = new BufferedWriter(fw);
                bw.write(result);

                Toast.makeText(getActivity(), "Carpools Refreshed!", Toast.LENGTH_SHORT).show();

            } catch (Exception e){
                e.printStackTrace();
                Toast.makeText(getActivity(), "Sorry! Something went wrong.", Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    bw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    //Should never happen
                }

            }
        }
    }
}
