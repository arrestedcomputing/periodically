package com.aubray.periodically.store;

import android.content.Context;
import android.os.AsyncTask;

import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.util.Callback;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * CloudStore back by Firebase
 */
public class FirebaseCloudStore implements CloudStore {
    private static final String FIREBASE_URL = "https://periodically.firebaseio.com/";
    private static final String SCOPES = "oauth2:profile email";
    public static final String PROVIDER = "google";

    private static final String PERIODICALS = "periodicals";
    public static final String EMAIL_TO_UID_INDEX = "emailToUidIndex";
    public static final String USERS = "users";

    private final Firebase fb;
    private final Context context;

    public FirebaseCloudStore(Context context) {
        this.context = context;
        Firebase.setAndroidContext(this.context);
        fb = new Firebase(FIREBASE_URL);
    }

    @Override
    public void lookUpPeriodical(String id, final Callback<Periodical> callback) {
        fb.child(PERIODICALS).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                callback.receive(TO_PERIODICAL.apply(dataSnapshot));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void savePeriodical(Periodical periodical) {
        fb.child(PERIODICALS).child(periodical.getId()).setValue(periodical);
    }

    @Override
    public void addPeriodicalsListener(final User user, final Callback<List<Periodical>> callback) {
        fb.child(PERIODICALS).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.receive(FluentIterable.from(snapshot.getChildren())
                        .transform(TO_PERIODICAL)
                        .filter(new Predicate<Periodical>() {
                            @Override
                            public boolean apply(Periodical periodical) {
                                return periodical.getSubscribers().contains(user.getUid());
                            }
                        })
                        .toList());
            }

            @Override public void onCancelled(FirebaseError firebaseError) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void deletePeriodical(String id) {
        fb.child(PERIODICALS).child(id).removeValue();
    }

    @Override
    public void googleLogin(String email, Callback<User> callback) {
        new FirebaseLoginTask(callback).execute(email);
    }

    @Override
    public void googleLogout() {
        fb.unauth();
    }

    static Map<String, String> emailToUidMap = Maps.newConcurrentMap();
    static Map<String, User> uidToUserMap = Maps.newConcurrentMap();

    @Override
    public void lookUpUserByEmail(final String email, final Callback<User> callback) {
        if (emailToUidMap.containsKey(email)) {
            lookUpUserByUid(emailToUidMap.get(email), callback);
        }

        fb.child(EMAIL_TO_UID_INDEX).child(sanitizeEmail(email))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String uid = dataSnapshot.getValue(String.class);
                        emailToUidMap.put(email, uid);
                        lookUpUserByUid(uid, callback);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("BUSE: " + firebaseError);
                    }
                });
    }

    @Override
    public void lookUpUserByUid(final String uid, final Callback<User> callback) {
        if (uidToUserMap.containsKey(uid)) {
            callback.receive(uidToUserMap.get(uid));
        }

        fb.child(USERS).child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        uidToUserMap.put(uid, user);
                        callback.receive(user);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("BUSE: " + firebaseError);
                    }
                });
    }

    private class FirebaseLoginTask extends AsyncTask<String, Void, Void> {
        private final Callback<User> userCallback;

        public FirebaseLoginTask(Callback<User> userCallback) {
            this.userCallback = userCallback;
        }

        protected Void doInBackground(String... email) {
            String token;

            try {
                token = GoogleAuthUtil.getToken(context, email[0], SCOPES);
            } catch (IOException | GoogleAuthException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            fb.authWithOAuthToken(PROVIDER, token, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    getUser(authData, userCallback);
                }

                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    System.out.println(firebaseError);
                }
            });

            return null;
        }
    }

    private void getUser(final AuthData authData, final Callback<User> userCallback) {
        // See if user already exists
        fb.child(USERS).child(authData.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user;
                        if (dataSnapshot.exists()) {
                            // Already exists, so return it
                            user = dataSnapshot.getValue(User.class);
                        } else {
                            // Create new user
                            String email = (String) authData.getProviderData().get("email");
                            String profileImageURL =
                                    (String) authData.getProviderData().get("profileImageURL");
                            @SuppressWarnings("unchecked")
                            Map<String, String> cachedUserProfile =
                                    (Map<String, String>) authData.getProviderData().get("cachedUserProfile");
                            String given_name = cachedUserProfile.get("given_name");
                            String family_name = cachedUserProfile.get("family_name");
                            user = new User(authData.getUid(), email, given_name, family_name);
                            if (profileImageURL != null) {
                                user.setProfileImageURL(profileImageURL);
                            }
                            fb.child(USERS).child(user.getUid()).setValue(user);
                            fb.child(EMAIL_TO_UID_INDEX)
                                    .child(sanitizeEmail(user.getEmail()))
                                    .setValue(user.getUid());
                        }
                        userCallback.receive(user);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("BUSE: " + firebaseError);
                    }
                });
    }

    private static String sanitizeEmail(String email) {
        return email.replace('.', ',');
    }

    private static final Function<DataSnapshot, Periodical> TO_PERIODICAL =
            new Function<DataSnapshot, Periodical>() {
                @Override
                public Periodical apply(DataSnapshot input) {
                    return input.getValue(Periodical.class);
                }
            };
}
