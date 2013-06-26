package com.quickblox.chat_v2.apis;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.quickblox.chat_v2.core.ChatApplication;
import com.quickblox.chat_v2.interfaces.OnContactRefreshListener;
import com.quickblox.chat_v2.interfaces.OnUserProfileDownloaded;
import com.quickblox.chat_v2.utils.GlobalConsts;
import com.quickblox.module.chat.QBChat;
import com.quickblox.module.chat.listeners.SubscriptionListener;
import com.quickblox.module.chat.model.QBChatRoster.QBRosterListener;
import com.quickblox.module.users.model.QBUser;

import org.jivesoftware.smack.packet.Presence;

import java.util.ArrayList;
import java.util.Collection;

public class RosterManager implements QBRosterListener, SubscriptionListener, OnUserProfileDownloaded {

    private ArrayList<String> subscribes;

    private Context mContext;
    private ChatApplication app;

    private int mUserId;

    private OnContactRefreshListener mOnContactRefreshListener;

    public RosterManager(Context pContext) {
        mContext = pContext;
        subscribes = new ArrayList<String>();
        app = ChatApplication.getInstance();
    }

    @Override
    public void entriesAdded(Collection<Integer> addedEntriesIds) {

        app.getQbm().setUserProfileListener(this);
        for (Integer ae : addedEntriesIds) {
            Log.d("RST-M", "asked user = " + ae);
            app.getQbm().getSingleUserInfo(ae);
        }
        if (mOnContactRefreshListener != null) {
            mOnContactRefreshListener.reFreshCurrentList();
        }

    }

    @Override
    public void entriesDeleted(Collection<Integer> deletedEntriesIds) {

        for (Integer de : deletedEntriesIds) {
            ChatApplication.getInstance().getContactsMap().remove(String.valueOf(de));
        }
        if (mOnContactRefreshListener != null) {
            mOnContactRefreshListener.reFreshCurrentList();
        }
    }

    @Override
    public void entriesUpdated(Collection<Integer> updatedEntriesIds) {

        for (Integer ue : updatedEntriesIds) {
            ChatApplication.getInstance().getContactsCandidateMap().remove(String.valueOf(ue));
            if (!ChatApplication.getInstance().getContactsMap().containsKey(String.valueOf(ue))) {

                ChatApplication.getInstance().getQbm().setUserProfileListener(this);
                ChatApplication.getInstance().getQbm().getSingleUserInfo(ue);

            }

            if (mOnContactRefreshListener != null) {
                mOnContactRefreshListener.reFreshCurrentList();
            }
        }
    }

    @Override
    public void presenceChanged(Presence presence) {
        String[] parts = presence.getFrom().split("-");
        app.getUserNetStatusMap().put(Integer.parseInt(parts[0]), presence.getType().toString());
    }

    @Override
    public void onSubscribe(int userId) {
        if (userId == app.getQbUser().getId() || app.getContactsMap().containsKey(String.valueOf(userId))) {
            return;
        }
        subscribes.add(String.valueOf(userId));
        ((Activity) mContext).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                app.getQbm().getQbUsersFromCollection(subscribes, GlobalConsts.DOWNLOAD_LIST_FOR_CONTACTS_CANDIDATE);

            }
        });

    }

    @Override
    public void onUnSubscribe(int userId) {
    }

    public void sendRequestToSubscribe(int userId) {
        mUserId = userId;
        ((Activity) mContext).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                QBChat.getInstance().subscribe(mUserId);
                refreshContactList();
            }
        });
    }

    public void sendRequestToUnSubscribe(int userId) {
        mUserId = userId;
        ((Activity) mContext).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                QBChat.getInstance().unsubscribe(mUserId);
                refreshContactList();
            }
        });
    }

    public void sendPresence(Context context) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                QBChat.getInstance().startAutoSendPresence(30);
            }
        });
    }


    public void refreshContactList() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                ArrayList<String> userIds = new ArrayList<String>();
                if (app.getQbRoster().getUsersId() != null) {
                    for (Integer in : app.getQbRoster().getUsersId()) {
                        if (in == -1) {
                            continue;
                        }
                        userIds.add(String.valueOf(in));
                    }
                    app.getQbm().getQbUsersFromCollection(userIds, GlobalConsts.DOWNLOAD_LIST_FOR_CONTACTS);
                }
            }
        }, 1000);

    }

    @Override
    public void downloadComlete(QBUser friend) {
        if (friend != null) {
            ChatApplication.getInstance().getContactsMap().put(String.valueOf(friend.getId()), friend);
        }
    }

    public void setOnContactRefreshListener(OnContactRefreshListener pOnContactRefreshListener) {
        mOnContactRefreshListener = pOnContactRefreshListener;
    }
}
