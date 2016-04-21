package com.aubray.periodically.store;

import com.aubray.periodically.model.Invitation;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import java.util.List;

/**
 * Interfact to cloud storage
 */
public interface CloudStore {
    void lookUpPeriodical(String pid, Callback<Periodical> callback);

    void addPeriodicalListener(String pid, final Callback<Periodical> callback);

    void savePeriodical(Periodical periodical);

    void addPeriodicalsListener(User user, Callback<List<String>> callback);

    void lookUpPeriodicals(User user, Callback<List<String>> callback);

    void deletePeriodical(Periodical periodical);

    void googleLogin(String token, Callback<User> callback);

    void googleLogout();

    void lookUpUserByEmail(String email, final Callback<Optional<User>> callback);

    void lookUpUserByUid(String uid, final Callback<User> callback);

    void invite(String inviterUid, String inviteeUid, String pid);

    void addInvitationsListener(String uid, final Callback<List<Invitation>> callback);

    void clearInvitation(String inviteeUid, String pid);

    void unsubscribe(User user, Periodical periodical);

    void subscribe(String uid, String periodicalId);
}
