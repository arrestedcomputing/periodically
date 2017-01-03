package com.aubray.periodically.activities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aubray.periodically.R;
import com.aubray.periodically.model.Subscription;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.util.Callback;

import java.util.List;

public class SubscriberArrayAdapter extends ArrayAdapter<Subscription> {

    Context mContext;
    private final List<Subscription> subscribers;
    private final String owner;
    int layoutResourceId;

    CloudStore cloudStore;

    public SubscriberArrayAdapter(Context mContext, int layoutResourceId, List<Subscription> subscribers, String owner) {
        super(mContext, layoutResourceId, subscribers);
        cloudStore = new FirebaseCloudStore(mContext);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.subscribers = subscribers;
        this.owner = owner;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        final TextView textView = (TextView) convertView.findViewById(R.id.subscriber_text);

        // object item based on the position
        final String subscriberUid = subscribers.get(position).getUser();

        cloudStore.lookUpUserByUid(subscriberUid, new Callback<User>() {
            @Override
            public void receive(User user) {
                String name = user.getGivenName();

                if (subscriberUid.equals(owner)) {
                    name = name + " (owner)";
                }

                textView.setText(name);
            }
        });

        return convertView;
    }
}