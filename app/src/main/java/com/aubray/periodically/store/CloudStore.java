package com.aubray.periodically.store;

import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.util.Callback;

import java.util.List;

/**
 * Interfact to cloud storage
 */
public interface CloudStore {
    void lookUpPeriodical(String id, Callback<Periodical> callback);

    void savePeriodical(Periodical periodical);

    void addPeriodicalsListener(User user, Callback<List<Periodical>> callback);

    void deletePeriodical(String id);

    void googleLogin(String token, Callback<User> callback);

    void googleLogout();

    void lookUpUserByEmail(String email, final Callback<User> callback);

    void lookUpUserByUid(String uid, final Callback<User> callback);
}
