package com.aubray.periodically.activities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aubray.periodically.R;
import com.aubray.periodically.logic.Periodicals;
import com.aubray.periodically.model.Event;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.Subscription;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.ui.PeriodicalFormatter;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.aubray.periodically.ui.PeriodicalFormatter.printFriendlyDate;
import static org.joda.time.Instant.now;

public class PeriodicalArrayAdapter extends ArrayAdapter<Periodical> {

    Context mContext;
    int layoutResourceId;
    SortedSet<Periodical> data = new TreeSet<>(Periodicals.NEXT_DUE_FIRST);
    CloudStore cloudStore;
    LocalStore localStore;

    public PeriodicalArrayAdapter(Context mContext, int layoutResourceId) {
        super(mContext, layoutResourceId);
        cloudStore = new FirebaseCloudStore(mContext);
        localStore = new PreferencesLocalStore(mContext);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        // object item based on the position
        Periodical periodical = getItem(position);

        // get the TextView and then set the text (item name) and tag (item ID) values
        TextView periodicalName = (TextView) convertView.findViewById(R.id.periodical_name_text);
        periodicalName.setText(periodical.getName());
        periodicalName.setTag(periodical.getId());

        TextView period = (TextView) convertView.findViewById(R.id.period_text);
        period.setText(periodical.getPeriod().toString());

        TextView due = (TextView) convertView.findViewById(R.id.due_text);
        Instant dueInstant = Periodicals.getDueInstant(periodical);
        if (dueInstant.isBefore(now())) {
            due.setText("Due Now");
        } else {
            due.setText(PeriodicalFormatter.printFriendlyDate(dueInstant, now()));
        }

        final TextView lastUser = (TextView) convertView.findViewById(R.id.last_user_text_view);
        final TextView lastUserLabel = (TextView) convertView.findViewById(R.id.last_user_label);
        final Optional<Event> event = Periodicals.getLastEvent(periodical);

        if (event.isPresent()) {
            cloudStore.lookUpUserByUid(event.get().getUser(), new Callback<User>() {
                @Override
                public void receive(User user) {
                    lastUserLabel.setVisibility(VISIBLE);
                    lastUser.setVisibility(VISIBLE);
                    lastUser.setText(user.getGivenName() + " " +
                            printFriendlyDate(new Instant(event.get().getMillis()), now()));
                }
            });
        } else {
            lastUserLabel.setVisibility(GONE);
            lastUser.setVisibility(GONE);
        }

        ImageView muteImage = (ImageView) convertView.findViewById(R.id.mute_image);

        Optional<Subscription> sub = periodical.getSubscriptionFor(localStore.getUser().get().getUid());

        if (sub.isPresent()) {
            muteImage.setVisibility(sub.get().isMuted() ? VISIBLE : INVISIBLE);
        }

        return convertView;
    }

    public void addOrUpdate(Periodical periodical) {
        data.add(periodical);

        remove(periodical);
        add(periodical);
        sort(Periodicals.NEXT_DUE_FIRST);

        notifyDataSetChanged();
    }

    public void removePeriodicalsNotIn(List<String> periodicals) {
        List<Periodical> toRemove = new ArrayList<>();
        for (Periodical periodical : data) {
            if (!periodicals.contains(periodical.getId())) {
                toRemove.add(periodical);
                remove(periodical);
            }
        }
        data.removeAll(toRemove);
        notifyDataSetChanged();
    }
}