package com.aubray.periodically.store;

import com.aubray.periodically.model.Account;
import com.aubray.periodically.model.User;
import com.google.common.base.Optional;

/**
 * Created by buse on 12/21/15.
 */
public interface LocalStore {
    void setAccount(String displayName, String email, String photoUrl);

    void clearAccount();

    Optional<Account> getAccount();

    void setUser(User user);
}
