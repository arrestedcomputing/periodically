package com.aubray.periodically.activities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aubray.periodically.R;
import com.aubray.periodically.model.Invitation;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InvitationsArrayAdapter extends ArrayAdapter<Invitation> {

    Context mContext;
    int layoutResourceId;
    List<Invitation> data;
    List<String> periodicalNames;

    CloudStore cloudStore;

    public InvitationsArrayAdapter(Context mContext, int layoutResourceId) {
        super(mContext, layoutResourceId);
        this.mContext = mContext;
        this.layoutResourceId = layoutResourceId;
        cloudStore = new FirebaseCloudStore(mContext);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        // object item based on the position
        final Invitation invitation = data.get(position);
        final TextView inviterNameView = (TextView) convertView.findViewById(R.id.inviter);
        final TextView periodicalNameView = (TextView) convertView.findViewById(R.id.invitation_periodical_name);
        final TextView periodicalDescView = (TextView) convertView.findViewById(R.id.invitation_periodical_description);

        cloudStore.lookUpUserByUid(invitation.getInviterUid(), new Callback<User>() {
            @Override
            public void receive(User user) {
                inviterNameView.setText(user.getGivenName());
            }
        });

        cloudStore.lookUpPeriodical(invitation.getPeriodicalId(), new Callback<Periodical>() {
            @Override
            public void receive(Periodical periodical) {
                periodicalNameView.setText(periodical.getName());
                periodicalDescView.setText("Due every " + periodical.getPeriod());
                periodicalNames.set(position, periodical.getName());
            }
        });

        return convertView;
    }

    public void updateData(List<Invitation> invitations) {
        this.data = invitations;
        this.periodicalNames = new ArrayList<>();
        periodicalNames.addAll(Collections.nCopies(invitations.size(), ""));
        this.clear();
        this.addAll(invitations);
    }

    public String getPeriodicalName(int position) {
        if (position < periodicalNames.size()) {
            return periodicalNames.get(position);
        } else {
            return "";
        }
    }
}