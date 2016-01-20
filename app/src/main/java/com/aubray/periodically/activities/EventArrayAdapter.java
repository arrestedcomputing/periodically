package com.aubray.periodically.activities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aubray.periodically.R;
import com.aubray.periodically.model.Event;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.ui.PeriodicalFormatter;
import com.aubray.periodically.util.Callback;

import org.joda.time.Instant;

import java.util.List;

public class EventArrayAdapter extends ArrayAdapter<Event> {

    Context mContext;
    int layoutResourceId;
    List<Event> data = null;

    CloudStore cloudStore;

    public EventArrayAdapter(Context mContext, int layoutResourceId, List<Event> data) {
        super(mContext, layoutResourceId, data);
        cloudStore = new FirebaseCloudStore(mContext);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        // object item based on the position
        final Event event = data.get(position);
        final TextView userNameView = (TextView) convertView.findViewById(R.id.user_text);

        cloudStore.lookUpUserByUid(event.getUser(), new Callback<User>() {
            @Override
            public void receive(User user) {
                userNameView.setText(user.getGivenName());
            }
        });

        TextView time = (TextView) convertView.findViewById(R.id.time_text);
        time.setText(PeriodicalFormatter.printFriendlyDate(new Instant(event.getMillis()), Instant.now()));

        return convertView;
    }

}