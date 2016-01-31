package com.aubray.periodically.store;

import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import java.util.List;

/**
 * Interfact to cloud storage
 */
public interface CloudStore {
    void lookUpPeriodical(String id, Callback<Periodical> callback);

    void addPeriodicalListener(String id, final Callback<Periodical> callback);

    void savePeriodical(Periodical periodical);

    void addPeriodicalsListener(User user, Callback<List<String>> callback);

    void lookUpPeriodicals(User user, Callback<List<String>> callback);

    void deletePeriodical(String id);

    void googleLogin(String token, Callback<User> callback);

    void googleLogout();

    void lookUpUserByEmail(String email, final Callback<Optional<User>> callback);

    void lookUpUserByUid(String uid, final Callback<User> callback);

    void invite(User inviter, User invitee, Periodical periodical);
}
