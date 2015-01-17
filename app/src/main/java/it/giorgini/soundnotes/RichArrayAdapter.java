package it.giorgini.soundnotes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by diego on 16/01/15.
 */
public class RichArrayAdapter<T> extends ArrayAdapter<T> {

    public RichArrayAdapter(Context context, List<T> items) {
        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, items);
    }

    public RichArrayAdapter(Context context, T[] items) {
        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, items);
    }

    public RichArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = super.getView(position, convertView, parent);
        TextView tv = (TextView) v.findViewById(android.R.id.text1);
        tv.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/RobotoSlab-Regular.ttf"));

        return v;
    }
}
