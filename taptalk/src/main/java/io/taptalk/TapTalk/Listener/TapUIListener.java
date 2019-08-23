package io.taptalk.TapTalk.Listener;

import android.app.Activity;

import java.util.HashMap;

import io.taptalk.TapTalk.Interface.TapTalkChatRoomInterface;
import io.taptalk.TapTalk.Manager.TapUI;
import io.taptalk.TapTalk.Model.TAPMessageModel;
import io.taptalk.TapTalk.Model.TAPUserModel;

public abstract class TapUIListener implements TapTalkChatRoomInterface {
    @Override
    public void onTapTalkChatRoomProfileButtonTapped(Activity activity, TAPUserModel user) {
        TapUI.openTapTalkUserProfile(activity, user);
    }

    @Override
    public void onTapTalkMessageQuoteTapped(Activity activity, TAPMessageModel message, HashMap<String, Object> userInfo) {
    }

}
