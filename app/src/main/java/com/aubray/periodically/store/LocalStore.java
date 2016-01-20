package com.aubray.periodically.store;

import com.aubray.periodically.model.User;
import com.google.common.base.Optional;

public interface LocalStore {
    void clearUser();

    Optional<User> getUser();

    void setUser(User user);
}
