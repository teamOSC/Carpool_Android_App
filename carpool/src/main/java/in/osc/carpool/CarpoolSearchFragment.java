package in.osc.carpool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.audiofx.BassBoost;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;

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
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import in.osc.carpool.utils.UserEmailFetcher;

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
     * The root view of the fragment shown
     */
    View rootView;

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
        rootView = inflater.inflate(R.layout.fragment_search_carpool, container, false);
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
            new RefreshCarpoolDatabase() {
                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    new CalculateCarpools().execute();
                }
            }.execute();
            return true;
        }

        if (item.getItemId() == R.id.action_set_range) {
            setUpRangeDialog();
            return true;
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

        List<Float> startDistances = new ArrayList<Float>();
        List<Float> endDistances = new ArrayList<Float>();
        List<String> emailList = new ArrayList<String>();
        ArrayList<JSONObject> dataList = new ArrayList<JSONObject>();

        @Override
        protected Void doInBackground(Void... voids) {

            String jsonString;

            try {
                File cacheFile = new File(getActivity().getFilesDir(), "data.json");

                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                jsonString = br.readLine();
                Log.d(TAG, "json output = " + jsonString);

                JSONArray mJsonArray = new JSONArray(jsonString);

                for(int i =0; i < mJsonArray.length(); ++i) {
                    JSONObject mJsonObject = mJsonArray.getJSONObject(i);
                    dataList.add(mJsonObject);
                }

                for (JSONObject mJSONObject : dataList) {
                    if (!mJSONObject.getString("email").equals(UserEmailFetcher.getEmail(getActivity()))) {
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
                        if (distanceBetweenStart[0] < 10 && distanceBetweenDest[0] < 10) {
                            startDistances.add(distanceBetweenStart[0] / 1000);
                            endDistances.add(distanceBetweenDest[0]/1000);
                            emailList.add(mJSONObject.getString("email"));
                        }
                        Log.d(TAG, "distanceBetweenStart = " + distanceBetweenStart[0]/1000 + " km");
                        Log.d(TAG, "distanceBetweenDest = " + distanceBetweenDest[0]/1000 + " km");
                    }
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

        @Override
        protected void onPostExecute (Void v) {
            ListView cardsList = (ListView) rootView.findViewById(R.id.googlecards_listview);
            SwingBottomInAnimationAdapter swingBottomInAnimationAdapter =
                    new SwingBottomInAnimationAdapter(new GoogleCardsAdapter(getActivity(), startDistances, endDistances, emailList));
            swingBottomInAnimationAdapter.setInitialDelayMillis(300);
            swingBottomInAnimationAdapter.setAbsListView(cardsList);
            cardsList.setAdapter(swingBottomInAnimationAdapter);
        }
    }

    private class RefreshCarpoolDatabase extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            String result = "";
            try {
                HttpClient client = new DefaultHttpClient();
                String uri = "http://tosc.in/sauravtom/carpool.json";
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

    private static class GoogleCardsAdapter extends BaseAdapter {

        private Context mContext;
        private List<Float> startDistances;
        private List<Float> endDistances;
        private List<String> emailList;


        public GoogleCardsAdapter(Context context, List<Float> startDistances, List<Float> endDistances, List<String> emailList) {
            mContext = context;
            this.startDistances = startDistances;
            this.endDistances = endDistances;
            this.emailList = emailList;
        }

        @Override
        public long getItemId (int pos) {
            return pos;
        }

        @Override
        public Object getItem (int pos) {
            return null;
        }

        @Override
        public int getCount () {
            return startDistances.size() != 0 ? startDistances.size() : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.googlecards_card, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.startdistanceTextView = (TextView) view.findViewById(R.id.start_distance_textview);
                viewHolder.endDistanceTextView = (TextView) view.findViewById(R.id.end_distance_textview);
                viewHolder.emailTextView = (TextView) view.findViewById(R.id.email_textview);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            if (getCount() == 0) {
                viewHolder.startdistanceTextView.setText("We couldn't find any carpool near your location. You try to update the range.");
                return view;
            }
            if (this.startDistances.size() != 0) {
                viewHolder.startdistanceTextView.setText("Start Diff : " + startDistances.get(position) + " km");
                viewHolder.endDistanceTextView.setText("End Diff : " + endDistances.get(position) + " km");
                viewHolder.emailTextView.setText("Email : " + emailList.get(position));
            } else {
                viewHolder.startdistanceTextView.setText("Sorry, we couldn't find any carpool around your place.");
            }

            return view;
        }

        private static class ViewHolder {
            TextView startdistanceTextView;
            TextView endDistanceTextView;
            TextView emailTextView;
        }
    }

    private void setUpRangeDialog () {
        final SeekBar sb;
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_set_range, null);
        sb = (SeekBar)layout.findViewById(R.id.seekbar_set_range);
        final TextView rangeText = (TextView) layout.findViewById(R.id.text_range);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                rangeText.setText(Float.valueOf(((float) i) / 10.0f) + " km");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        final SharedPreferences settings = getActivity().getSharedPreferences("MAIN", 0);
        int progress = settings.getInt("SEARCH_RANGE", 0);
        sb.setProgress(progress);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(layout);

        builder.setPositiveButton("Update Range", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                int progress = sb.getProgress();
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("SEARCH_RANGE", progress);
                editor.commit();
                Toast.makeText(getActivity(), "Range changed to " + (((float) progress) / 10.0f ) , Toast.LENGTH_SHORT).show();
                new CalculateCarpools().execute();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
