package com.aubray.periodically.activities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aubray.periodically.R;
import com.aubray.periodically.logic.Periodicals;
import com.aubray.periodically.model.Event;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.ui.PeriodicalFormatter;
import com.aubray.periodically.util.LoggingCallback;
import com.google.common.base.Optional;

import org.joda.time.Instant;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.aubray.periodically.ui.PeriodicalFormatter.printFriendlyDate;

// here's our beautiful adapter
public class PeriodicalArrayAdapter extends ArrayAdapter<Periodical> {

    Context mContext;
    int layoutResourceId;
    List<Periodical> data = null;
    CloudStore cloudStore;

    public PeriodicalArrayAdapter(Context mContext, int layoutResourceId, List<Periodical> data) {
        super(mContext, layoutResourceId, data);
        cloudStore = new FirebaseCloudStore(mContext);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
        if(convertView == null){
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }

        // object item based on the position
        Periodical periodical = data.get(position);

        // get the TextView and then set the text (item name) and tag (item ID) values
        TextView periodicalName = (TextView) convertView.findViewById(R.id.periodical_name_text);
        periodicalName.setText(periodical.getName());
        periodicalName.setTag(periodical.getId());

        TextView period = (TextView) convertView.findViewById(R.id.period_text);
        period.setText(periodical.getPeriod().toString());

        TextView due = (TextView) convertView.findViewById(R.id.due_text);
        due.setText(PeriodicalFormatter.printFriendlyDate(Periodicals.getDueInstant(periodical), Instant.now()));

        final TextView lastUser = (TextView) convertView.findViewById(R.id.last_user_text_view);
        final TextView lastUserLabel = (TextView) convertView.findViewById(R.id.last_user_label);
        final Optional<Event> event = Periodicals.getLastEvent(periodical);

        if (event.isPresent()) {
            cloudStore.lookUpUserByEmail(event.get().getUser(), new LoggingCallback<User>("userByEmail") {
                @Override
                public void receive(User user) {
                    log();
                    lastUserLabel.setVisibility(VISIBLE);
                    lastUser.setVisibility(VISIBLE);
                    lastUser.setText(user.getGivenName() + " " +
                            printFriendlyDate(new Instant(event.get().getMillis()), Instant.now()));
                }
            });
        } else {
            lastUserLabel.setVisibility(GONE);
            lastUser.setVisibility(GONE);
        }

        return convertView;
    }
}