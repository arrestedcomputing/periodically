package com.aubray.periodically.store;

import android.content.Context;
import android.os.AsyncTask;

import com.aubray.periodically.model.Invitation;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.Subscription;
import com.aubray.periodically.model.User;
import com.aubray.periodically.util.Callback;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
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
    public static final String USERS = "users";
    public static final String EMAIL_TO_UID_INDEX = "emailToUidIndex";
    public static final String SUBSCRIPTIONS_INDEX = "uidToSubscriptionsIndex";
    public static final String INVITATIONS = "invitations";

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
                Periodical changedPeriodical = TO_PERIODICAL.apply(dataSnapshot);

                // Will be null if deleted
                if (changedPeriodical != null) {
                    callback.receive(TO_PERIODICAL.apply(dataSnapshot));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void addPeriodicalListener(String pid, final Callback<Periodical> callback) {
        fb.child(PERIODICALS).child(pid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Periodical changedPeriodical = TO_PERIODICAL.apply(dataSnapshot);

                // Will be null if deleted
                if (changedPeriodical != null) {
                    callback.receive(changedPeriodical);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void savePeriodical(final Periodical periodical) {
        fb.child(PERIODICALS).child(periodical.getId()).setValue(periodical);

        for (final Subscription subscription : periodical.getSubscriptions()) {
            fb.child(SUBSCRIPTIONS_INDEX).child(subscription.getUser()).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    List<String> subscriptions =
                            mutableData.getValue(new GenericTypeIndicator<List<String>>() {});

                    if (subscriptions == null) {
                        subscriptions = new ArrayList<>();
                    }

                    if (!subscriptions.contains(periodical.getId())) {
                        subscriptions.add(periodical.getId());
                        mutableData.setValue(subscriptions);
                        return Transaction.success(mutableData);
                    }

                    return Transaction.abort();
                }

                @Override
                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                    System.out.println("firebaseError = " + firebaseError);
                    System.out.println("Complete = " + dataSnapshot);
                }
            });
        }
    }

    @Override
    public void addPeriodicalsListener(final User user, final Callback<List<String>> callback) {
        fb.child(SUBSCRIPTIONS_INDEX).child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<String> subscriptions =
                                snapshot.getValue(new GenericTypeIndicator<List<String>>() {});

                        callback.receive(subscriptions);
                    }

                    @Override public void onCancelled(FirebaseError firebaseError) {
                        System.err.println(firebaseError);
                    }
                });
    }

    @Override
    public void lookUpPeriodicals(final User user, final Callback<List<String>> callback) {
        fb.child(SUBSCRIPTIONS_INDEX).child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<String> subscriptions =
                                snapshot.getValue(new GenericTypeIndicator<List<String>>() {});

                        callback.receive(subscriptions);
                    }

                    @Override public void onCancelled(FirebaseError firebaseError) {
                        System.err.println(firebaseError);
                    }
                });
    }

    @Override
    public void deletePeriodical(final Periodical periodical) {
        fb.child(PERIODICALS).child(periodical.getId()).removeValue();

        // Remove from index of all subscribers
        for (Subscription subscriber : periodical.getSubscriptions()) {
            fb.child(SUBSCRIPTIONS_INDEX).child(subscriber.getUser()).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    List<String> subscriptions =
                            mutableData.getValue(new GenericTypeIndicator<List<String>>() {});

                    if (subscriptions != null) {
                        subscriptions.remove(periodical.getId());
                        mutableData.setValue(subscriptions);
                        return Transaction.success(mutableData);
                    }

                    return Transaction.abort();
                }

                @Override
                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                    System.err.println(firebaseError);
                }
            });
        }
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
    public void lookUpUserByEmail(final String email, final Callback<Optional<User>> callback) {
        final Callback<User> userCallback = new Callback<User>() {
            @Override
            public void receive(User user) {
                callback.receive(Optional.of(user));
            }
        };

        if (emailToUidMap.containsKey(email)) {
            lookUpUserByUid(emailToUidMap.get(email), userCallback);
        } else {
            fb.child(EMAIL_TO_UID_INDEX).child(sanitizeEmail(email))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String uid = dataSnapshot.getValue(String.class);
                                emailToUidMap.put(email, uid);
                                lookUpUserByUid(uid, userCallback);
                            } else {
                                callback.receive(Optional.<User>absent());
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            System.out.println(firebaseError);
                        }
                    });
        }
    }

    @Override
    public void lookUpUserByUid(final String uid, final Callback<User> callback) {
        if (uidToUserMap.containsKey(uid)) {
            callback.receive(uidToUserMap.get(uid));
        } else {

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
                            System.out.println(firebaseError);
                        }
                    });
        }
    }

    @Override
    public void invite(final String inviterUid, final String inviteeUid, final String pid) {
        fb.child(INVITATIONS).child(inviteeUid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                List<Invitation> invitations =
                        mutableData.getValue(new GenericTypeIndicator<List<Invitation>>() {});

                if (invitations == null) {
                    invitations = new ArrayList<>();
                }

                invitations.add(new Invitation(inviterUid, inviteeUid, pid));
                mutableData.setValue(invitations);

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                System.out.println(firebaseError);
            }
        });
    }

    @Override
    public void addInvitationsListener(String uid, final Callback<List<Invitation>> callback) {
        fb.child(INVITATIONS).child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Invitation> invitations =
                        dataSnapshot.getValue(new GenericTypeIndicator<List<Invitation>>() {});
                if (invitations == null) {
                    invitations = new ArrayList<>();
                }
                callback.receive(invitations);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void lookupInvitations(User user, final Callback<List<Invitation>> callback) {
        fb.child(INVITATIONS).child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Invitation> invitations =
                        dataSnapshot.getValue(new GenericTypeIndicator<List<Invitation>>() {});
                if (invitations == null) {
                    invitations = new ArrayList<>();
                }
                callback.receive(invitations);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void clearInvitation(final String inviteeUid, final String pid) {
        fb.child(INVITATIONS).child(inviteeUid).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                List<Invitation> invitations =
                        mutableData.getValue(new GenericTypeIndicator<List<Invitation>>() {});
                if (invitations != null) {
                    Optional<Invitation> invitation =
                            Iterables.tryFind(invitations, new Predicate<Invitation>() {
                                @Override
                                public boolean apply(Invitation invitation) {
                                    return invitation.getInviteeUid().equals(inviteeUid)
                                            && invitation.getPeriodicalId().equals(pid);
                                }
                            });

                    if (invitation.isPresent()) {
                        invitations.remove(invitation.get());
                        mutableData.setValue(invitations);
                        return Transaction.success(mutableData);
                    }
                }

                return Transaction.abort();
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                if (firebaseError != null) {
                    System.err.println("firebaseError = " + firebaseError);
                }
            }
        });
    }

    @Override
    public void unsubscribe(User user, final Periodical periodical) {
        periodical.removeSubscriber(user);
        savePeriodical(periodical);

        // update index
        fb.child(SUBSCRIPTIONS_INDEX).child(user.getUid()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                List<String> subscriptions =
                        mutableData.getValue(new GenericTypeIndicator<List<String>>() {});

                if (subscriptions != null) {
                    subscriptions.remove(periodical.getId());
                    mutableData.setValue(subscriptions);
                    return Transaction.success(mutableData);
                }

                return Transaction.abort();
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                System.err.println(firebaseError);
            }
        });
    }

    @Override
    public void subscribe(final String uid, final String pid) {
        lookUpPeriodical(pid, new Callback<Periodical>() {
            @Override
            public void receive(Periodical periodical) {
                periodical.addSubscriber(uid);
                savePeriodical(periodical);
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
