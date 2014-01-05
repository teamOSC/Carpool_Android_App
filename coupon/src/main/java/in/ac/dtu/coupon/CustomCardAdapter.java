package in.ac.dtu.coupon;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.cardsui.Card;
import com.afollestad.cardsui.CardAdapter;
import com.afollestad.silk.images.SilkImageManager;
import com.afollestad.silk.views.image.SilkImageView;
import com.afollestad.silk.views.text.SilkTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by omerjerk on 26/12/13.
 */
public class CustomCardAdapter extends CardAdapter<Card> {

    private ArrayList<JSONObject> couponList;
    private Context context;

    public CustomCardAdapter(Context context, ArrayList<JSONObject> couponList) {
        super(context, R.layout.card_layout); // the custom card layout is passed to the super constructor instead of every individual card
        this.couponList = couponList;
        this.context = context;
        setAccentColor(Color.parseColor("#ff0099cc"));
    }

    public Drawable getVendorIcon (String vendorName) {
        vendorName = vendorName.toLowerCase();
        int vendorIconId = context.getResources().getIdentifier(vendorName, "drawable", "in.ac.dtu.coupon");
        Drawable vendorIcon = context.getResources().getDrawable(vendorIconId);
        return vendorIcon;
    }

    public boolean haveVendorIcon (String vendorName) {
        String[] vendorArray = context.getResources().getStringArray(R.array.vendors);
        for (int i = 0; i < vendorArray.length; i++) {
            if (vendorArray[i].equalsIgnoreCase(vendorName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onProcessThumbnail(ImageView icon, Card card) {
        // Optional, you can modify properties of the icon ImageView here.
        return super.onProcessThumbnail(icon, card);
    }

    @Override
    protected boolean onProcessTitle(TextView title, Card card, int accentColor) {
        // Optional, you can modify properties of the title textview here.
        return super.onProcessTitle(title, card, accentColor);
    }

    @Override
    protected boolean onProcessContent(TextView content, Card card) {
        // Optional, you can modify properties of the content textview here.
        content.setTextColor(Color.DKGRAY);
        return super.onProcessContent(content, card);

    }

    @Override
    protected boolean onProcessMenu(View view, final Card card) {
        // Sets up a card's menu (custom_card.xml makes this a star)
        return super.onProcessMenu(view, card);
    }

    @Override
    public View onViewCreated(final int index, View recycled, Card item) {

        TextView couponName = (TextView) recycled.findViewById(R.id.name_coupon);
        LinearLayout cardLayout = (LinearLayout) recycled.findViewById(R.id.card_layout);
        ImageView vendorIcon = (ImageView) recycled.findViewById(R.id.vendor_icon);

        if (recycled == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            recycled = inflater.inflate(R.layout.card_layout, null);
        }
        try{
            if(couponName != null) {
                String vendorName = couponList.get(index - 1).getString("name");
                couponName.setText("WEBSITE : " + vendorName);
                couponName.setTextColor(Color.parseColor("#444444"));
                cardLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast toast = Toast.makeText(context,
                                "Long press this card to go to website to redeem coupon",
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
                cardLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Uri uri = null;
                        try {
                            uri = Uri.parse(couponList.get(index - 1).getString("url"));
                        } catch (JSONException e) {
                            uri = Uri.parse("http://google.com");
                            e.printStackTrace();
                        }
                        Intent webIntent = new Intent(Intent.ACTION_VIEW, uri);
                        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(webIntent);
                        return false;
                    }
                });


                if (haveVendorIcon(vendorName)) {
                    vendorIcon.setImageDrawable(getVendorIcon(vendorName));
                } else {
                    vendorIcon.setImageDrawable(getVendorIcon("none"));
                }
            }



        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Optional, you can modify properties of other views that you add to the card layout that aren't the icon, title, content...
        return super.onViewCreated(index, recycled, item);
    }

}
