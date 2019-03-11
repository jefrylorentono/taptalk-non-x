package io.taptalk.TapTalk.Manager;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import io.taptalk.TapTalk.API.Api.TAPApiManager;
import io.taptalk.TapTalk.API.RequestBody.ProgressRequestBody;
import io.taptalk.TapTalk.API.Subscriber.TAPBaseSubscriber;
import io.taptalk.TapTalk.API.Subscriber.TAPDefaultSubscriber;
import io.taptalk.TapTalk.API.View.TapDefaultDataView;
import io.taptalk.TapTalk.Data.Message.TAPMessageEntity;
import io.taptalk.TapTalk.Data.RecentSearch.TAPRecentSearchEntity;
import io.taptalk.TapTalk.Listener.TAPDatabaseListener;
import io.taptalk.TapTalk.Model.ResponseModel.TAPAuthTicketResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPBaseResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPCommonResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPContactResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetAccessTokenResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetMessageListByRoomResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetMultipleUserResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetRoomListResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPGetUserResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPSendCustomMessageResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPUpdateMessageStatusResponse;
import io.taptalk.TapTalk.Model.ResponseModel.TAPUploadFileResponse;
import io.taptalk.TapTalk.Model.TAPErrorModel;
import io.taptalk.TapTalk.Model.TAPRoomModel;
import io.taptalk.TapTalk.Model.TAPUserModel;
import okhttp3.ResponseBody;

import static io.taptalk.TapTalk.Const.TAPDefaultConstant.CustomHeaderKey.APP_ID;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.CustomHeaderKey.APP_SECRET;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.CustomHeaderKey.USER_AGENT;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_ACCESS_TOKEN;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_ACCESS_TOKEN_EXPIRY;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_AUTH_TICKET;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_IS_ROOM_LIST_SETUP_FINISHED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_IS_WRITE_STORAGE_PERMISSION_REQUESTED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_LAST_UPDATED;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_RECIPIENT_ID;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_REFRESH_TOKEN;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_REFRESH_TOKEN_EXPIRY;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_USER;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.K_USER_LAST_ACTIVITY;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.Notification.K_FIREBASE_TOKEN;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.Notification.K_NOTIFICATION_MESSAGE_MAP;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.OldDataConst.K_LAST_DELETE_TIMESTAMP;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.UploadBroadcastEvent.UploadCancelled;
import static io.taptalk.TapTalk.Const.TAPDefaultConstant.UploadBroadcastEvent.UploadLocalID;

public class TAPDataManager {
    private static final String TAG = TAPDataManager.class.getSimpleName();
    private static TAPDataManager instance;
    private boolean isNeedToQueryUpdateRoomList;

    public static TAPDataManager getInstance() {
        return instance == null ? (instance = new TAPDataManager()) : instance;
    }

    public boolean isNeedToQueryUpdateRoomList() {
        return isNeedToQueryUpdateRoomList;
    }

    public void setNeedToQueryUpdateRoomList(boolean needToQueryUpdateRoomList) {
        isNeedToQueryUpdateRoomList = needToQueryUpdateRoomList;
    }

    /**
     * =========================================================================================== *
     * GENERIC METHODS FOR PREFERENCE
     * =========================================================================================== *
     */

    private void saveBooleanPreference(boolean bool, String key) {
        Hawk.put(key, bool);
    }

    private void saveStringPreference(String string, String key) {
        Hawk.put(key, string);
    }

    private void saveLongTimestampPreference(Long timestamp, String key) {
        Hawk.put(key, timestamp);
    }

    private String getStringPreference(String key) {
        return Hawk.get(key, "");
    }

    private Long getLongTimestampPreference(String key) {
        return Hawk.get(key, Long.parseLong("0"));
    }

    private Boolean checkPreferenceKeyAvailable(String key) {
        return Hawk.contains(key);
    }

    private void removePreference(String key) {
        Hawk.delete(key);
    }

    /**
     * =========================================================================================== *
     * PUBLIC METHODS FOR PREFERENCE (CALLS GENERIC METHODS ABOVE)
     * PUBLIC METHODS MAY NOT HAVE KEY AS PARAMETER
     * =========================================================================================== *
     */

    public void deleteAllPreference() {
        removeActiveUser();
        removeAuthTicket();
        removeAccessToken();
        removeRefreshToken();
        removeLastUpdatedMessageTimestampMap();
        removeUserLastActivityMap();
        removeRoomListSetupFinished();
        removeRecipientID();
        removeWriteStoragePermissionRequested();
        removeLastDeleteTimestamp();
        removeNotificationMap();
    }

    /**
     * ACTIVE USER
     */
    public boolean checkActiveUser() {
        if (null == getActiveUser())
            return false;
        else return true;
    }

    public TAPUserModel getActiveUser() {
        return Hawk.get(K_USER, null);
    }

    public void saveActiveUser(TAPUserModel user) {
        Hawk.put(K_USER, user);
        TAPChatManager.getInstance().setActiveUser(user);
    }

    public void removeActiveUser() {
        Hawk.delete(K_USER);
    }

    /**
     * AUTH TICKET
     */

    public Boolean checkAuthTicketAvailable() {
        return checkPreferenceKeyAvailable(K_AUTH_TICKET);
    }

    public String getAuthTicket() {
        return getStringPreference(K_AUTH_TICKET);
    }

    public void saveAuthTicket(String authTicket) {
        saveStringPreference(authTicket, K_AUTH_TICKET);
    }

    public void removeAuthTicket() {
        removePreference(K_AUTH_TICKET);
    }

    /**
     * ACCESS TOKEN
     */

    public Boolean checkAccessTokenAvailable() {
        return checkPreferenceKeyAvailable(K_ACCESS_TOKEN);
    }

    public String getAccessToken() {
        return getStringPreference(K_ACCESS_TOKEN);
    }

    public void saveAccessToken(String accessToken) {
        saveStringPreference(accessToken, K_ACCESS_TOKEN);
    }

    public void saveAccessTokenExpiry(Long accessTokenExpiry) {
        saveLongTimestampPreference(accessTokenExpiry, K_ACCESS_TOKEN_EXPIRY);
    }

    public void removeAccessToken() {
        removePreference(K_ACCESS_TOKEN);
    }

    /**
     * REFRESH TOKEN
     */

    public Boolean checkRefreshTokenAvailable() {
        return checkPreferenceKeyAvailable(K_REFRESH_TOKEN);
    }

    public String getRefreshToken() {
        return getStringPreference(K_REFRESH_TOKEN);
    }

    public void saveRefreshToken(String refreshToken) {
        saveStringPreference(refreshToken, K_REFRESH_TOKEN);
    }

    public void saveRefreshTokenExpiry(Long refreshTokenExpiry) {
        saveLongTimestampPreference(refreshTokenExpiry, K_REFRESH_TOKEN_EXPIRY);
    }

    public void removeRefreshToken() {
        removePreference(K_REFRESH_TOKEN);
    }

    /**
     * LAST UPDATED MESSAGE
     */

    public Long getLastUpdatedMessageTimestamp(String roomID) {
        return null == getLastUpdatedMessageTimestampMap() ? Long.parseLong("0")
                : !getLastUpdatedMessageTimestampMap().containsKey(roomID) ? Long.parseLong("0") :
                getLastUpdatedMessageTimestampMap().get(roomID);
    }

    public boolean checkKeyInLastMessageTimestamp(String roomID) {
        return Long.parseLong("0") != getLastUpdatedMessageTimestamp(roomID);
    }

    public void saveLastUpdatedMessageTimestamp(String roomID, Long lastUpdated) {
        saveLastUpdatedMessageTimestampMap(roomID, lastUpdated);
    }

    private HashMap<String, Long> getLastUpdatedMessageTimestampMap() {
        return Hawk.get(K_LAST_UPDATED, null);
    }

    private void saveLastUpdatedMessageTimestampMap(String roomID, long lastUpdated) {
        HashMap<String, Long> tempLastUpdated;
        if (null != getLastUpdatedMessageTimestampMap())
            tempLastUpdated = getLastUpdatedMessageTimestampMap();
        else tempLastUpdated = new LinkedHashMap<>();

        tempLastUpdated.put(roomID, lastUpdated);
        Hawk.put(K_LAST_UPDATED, tempLastUpdated);
    }

    private void removeLastUpdatedMessageTimestampMap() {
        Hawk.delete(K_LAST_UPDATED);
    }

    /**
     * USER LAST ACTIVITY
     */

    public HashMap<String, Long> getUserLastActivityMap() {
        return Hawk.get(K_USER_LAST_ACTIVITY);
    }

    public void saveUserLastActivityMap(HashMap<String, Long> userLastActivityMap) {
        Hawk.put(K_USER_LAST_ACTIVITY, userLastActivityMap);
    }

    public void removeUserLastActivityMap() {
        Hawk.delete(K_USER_LAST_ACTIVITY);
    }

    /**
     * ROOM LIST FIRST SETUP
     */

    public void setRoomListSetupFinished() {
        saveLongTimestampPreference(System.currentTimeMillis(), K_IS_ROOM_LIST_SETUP_FINISHED);
    }

    public Boolean isRoomListSetupFinished() {
        return checkPreferenceKeyAvailable(K_IS_ROOM_LIST_SETUP_FINISHED);
    }

    private void removeRoomListSetupFinished() {
        removePreference(K_IS_ROOM_LIST_SETUP_FINISHED);
    }

    /**
     * WRITE STORAGE PERMISSION REQUEST ON OPEN CHAT ROOM
     */

    public Boolean isWriteStoragePermissionRequested() {
        return checkPreferenceKeyAvailable(K_IS_WRITE_STORAGE_PERMISSION_REQUESTED);
    }

    public void setWriteStoragePermissionRequested(boolean isRequested) {
        saveBooleanPreference(isRequested, K_IS_WRITE_STORAGE_PERMISSION_REQUESTED);
    }

    public void removeWriteStoragePermissionRequested() {
        removePreference(K_IS_WRITE_STORAGE_PERMISSION_REQUESTED);
    }

    // TODO: 14/09/18 TEMP
    public String getRecipientID() {
        return Hawk.get(K_RECIPIENT_ID, "0");
    }

    // TODO: 14/09/18 TEMP
    public void saveRecipientID(String recipientID) {
        Hawk.put(K_RECIPIENT_ID, recipientID);
    }

    public void removeRecipientID() {
        removePreference(K_RECIPIENT_ID);
    }

    /**
     * Firebase Token
     */
    public void saveFirebaseToken(String firebaseToken) {
        saveStringPreference(firebaseToken, K_FIREBASE_TOKEN);
    }

    public String getFirebaseToken() {
        return getStringPreference(K_FIREBASE_TOKEN);
    }

    public Boolean checkFirebaseToken(String newFirebaseToken) {
        if (!checkPreferenceKeyAvailable(K_FIREBASE_TOKEN))
            return false;
        else if (newFirebaseToken.equals(getFirebaseToken())) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean checkFirebaseToken() {
        if (!checkPreferenceKeyAvailable(K_FIREBASE_TOKEN) || null == getFirebaseToken() || "0".equals(getFirebaseToken()))
            return false;
        else {
            return true;
        }
    }

    /**
     * Old Data (Auto Clean) Last Delete Timestamp
     */
    public void saveLastDeleteTimestamp(Long lastDeleteTimestamp) {
        saveLongTimestampPreference(lastDeleteTimestamp, K_LAST_DELETE_TIMESTAMP);
    }

    public Long getLastDeleteTimestamp() {
        return getLongTimestampPreference(K_LAST_DELETE_TIMESTAMP);
    }

    public Boolean checkLastDeleteTimestamp() {
        if (!checkPreferenceKeyAvailable(K_LAST_DELETE_TIMESTAMP) || null == getLastDeleteTimestamp())
            return false;
        else return 0 != getLastDeleteTimestamp();
    }

    private void removeLastDeleteTimestamp() {
        removePreference(K_LAST_DELETE_TIMESTAMP);
    }

    /**
     * Notification Message Map
     */
    public void saveNotificationMessageMap(String notifMessagesMap) {
        Hawk.put(K_NOTIFICATION_MESSAGE_MAP, notifMessagesMap);
    }

    public String getNotificationMessageMap() {
        return Hawk.get(K_NOTIFICATION_MESSAGE_MAP, null);
    }

    public void clearNotificationMessageMap() {
        Hawk.delete(K_NOTIFICATION_MESSAGE_MAP);
    }

    public boolean checkNotificationMap() {
        return checkPreferenceKeyAvailable(K_NOTIFICATION_MESSAGE_MAP);
    }

    private void removeNotificationMap() {
        removePreference(K_NOTIFICATION_MESSAGE_MAP);
    }

    /**
     * API HEADER
     */
    public void saveApplicationID(String applicationID) {
        saveStringPreference(applicationID, APP_ID);
    }

    public String getApplicationID() {
        return getStringPreference(APP_ID);
    }

    public boolean checkApplicationIDAvailability() {
        return checkPreferenceKeyAvailable(APP_ID);
    }

    public void removeApplicationID() {
        removePreference(APP_ID);
    }

    public void saveApplicationSecret(String applicationSecret) {
        saveStringPreference(applicationSecret, APP_SECRET);
    }

    public String getApplicationSecret() {
        return getStringPreference(APP_SECRET);
    }

    public boolean checkApplicationSecretAvailability() {
        return checkPreferenceKeyAvailable(APP_SECRET);
    }

    public void removeApplicationSecret() {
        removePreference(APP_SECRET);
    }

    public void saveUserAgent(String userAgent) {
        saveStringPreference(userAgent, USER_AGENT);
    }

    public String getUserAgent() {
        return getStringPreference(USER_AGENT);
    }

    public boolean checkUserAgentAvailability() {
        return checkPreferenceKeyAvailable(USER_AGENT);
    }

    public void removeUserAgent() {
        removePreference(USER_AGENT);
    }

    /**
     * =========================================================================================== *
     * DATABASE METHODS
     * =========================================================================================== *
     */

    // initialized Database Managernya yang di panggil di class Homing Pigeon
    public void initDatabaseManager(String databaseType, Application application) {
        TAPDatabaseManager.getInstance().setRepository(databaseType, application);
    }

    // Message
    public void deleteMessage(List<TAPMessageEntity> messageEntities, TAPDatabaseListener listener) {
        TAPDatabaseManager.getInstance().deleteMessage(new ArrayList<>(messageEntities), listener);
    }

    public void insertToDatabase(TAPMessageEntity messageEntity) {
        TAPDatabaseManager.getInstance().insert(messageEntity);
    }

    public void insertToDatabase(List<TAPMessageEntity> messageEntities, boolean isClearSaveMessages) {
        TAPDatabaseManager.getInstance().insert(new ArrayList<>(messageEntities), isClearSaveMessages);
    }

    public void insertToDatabase(List<TAPMessageEntity> messageEntities, boolean isClearSaveMessages, TAPDatabaseListener listener) {
        TAPDatabaseManager.getInstance().insert(new ArrayList<>(messageEntities), isClearSaveMessages, listener);
    }

    public void deleteFromDatabase(String messageLocalID) {
        TAPDatabaseManager.getInstance().delete(messageLocalID);
    }

    public void updateSendingMessageToFailed() {
        TAPDatabaseManager.getInstance().updatePendingStatus();
    }

    public void updateSendingMessageToFailed(String localID) {
        TAPDatabaseManager.getInstance().updatePendingStatus(localID);
    }

    public void updateFailedMessageToSending(String localID) {
        TAPDatabaseManager.getInstance().updateFailedStatusToSending(localID);
    }

    public LiveData<List<TAPMessageEntity>> getMessagesLiveData() {
        return TAPDatabaseManager.getInstance().getMessagesLiveData();
    }

    public void getMessagesFromDatabaseDesc(String roomID, TAPDatabaseListener<TAPMessageEntity> listener) {
        TAPDatabaseManager.getInstance().getMessagesDesc(roomID, listener);
    }

    public void getMessagesFromDatabaseDesc(String roomID, TAPDatabaseListener listener, long lastTimestamp) {
        TAPDatabaseManager.getInstance().getMessagesDesc(roomID, listener, lastTimestamp);
    }

    public void getMessagesFromDatabaseAsc(String roomID, TAPDatabaseListener<TAPMessageEntity> listener) {
        TAPDatabaseManager.getInstance().getMessagesAsc(roomID, listener);
    }

    public void searchAllMessagesFromDatabase(String keyword, TAPDatabaseListener<TAPMessageEntity> listener) {
        TAPDatabaseManager.getInstance().searchAllMessages(keyword, listener);
    }

    public void getRoomList(List<TAPMessageEntity> saveMessages, boolean isCheckUnreadFirst, TAPDatabaseListener<TAPMessageEntity> listener) {
        if (null == getActiveUser()) {
            return;
        }
        TAPDatabaseManager.getInstance().getRoomList(getActiveUser().getUserID(), saveMessages, isCheckUnreadFirst, listener);
    }

    public void getRoomList(boolean isCheckUnreadFirst, TAPDatabaseListener<TAPMessageEntity> listener) {
        if (null == getActiveUser()) {
            return;
        }
        TAPDatabaseManager.getInstance().getRoomList(getActiveUser().getUserID(), isCheckUnreadFirst, listener);
    }

    public void searchAllRoomsFromDatabase(String keyword, TAPDatabaseListener<TAPMessageEntity> listener) {
        if (null == getActiveUser()) {
            return;
        }
        TAPDatabaseManager.getInstance().searchAllRooms(getActiveUser().getUserID(), keyword, listener);
    }

    public void getRoomModel(TAPUserModel userModel, TAPDatabaseListener<TAPRoomModel> listener) {
        if (null == getActiveUser()) {
            return;
        }
        TAPDatabaseManager.getInstance().getRoom(getActiveUser().getUserID(), userModel, listener);
    }

    public void getUnreadCountPerRoom(String roomID, final TAPDatabaseListener<TAPMessageEntity> listener) {
        if (null == getActiveUser()) {
            return;
        }
        TAPDatabaseManager.getInstance().getUnreadCountPerRoom(getActiveUser().getUserID(), roomID, listener);
    }

    public void getUnreadCount(final TAPDatabaseListener<TAPMessageEntity> listener) {
        if (null == getActiveUser()) {
            return;
        }
        TAPDatabaseManager.getInstance().getUnreadCount(getActiveUser().getUserID(), listener);
    }

    public void deleteAllMessage() {
        TAPDatabaseManager.getInstance().deleteAllMessage();
    }

    // Recent Search
    public void insertToDatabase(TAPRecentSearchEntity recentSearchEntity) {
        TAPDatabaseManager.getInstance().insert(recentSearchEntity);
    }

    public void deleteFromDatabase(TAPRecentSearchEntity recentSearchEntity) {
        TAPDatabaseManager.getInstance().delete(recentSearchEntity);
    }

    public void deleteFromDatabase(List<TAPRecentSearchEntity> recentSearchEntities) {
        TAPDatabaseManager.getInstance().delete(recentSearchEntities);
    }

    public void deleteAllRecentSearch() {
        TAPDatabaseManager.getInstance().deleteAllRecentSearch();
    }

    public LiveData<List<TAPRecentSearchEntity>> getRecentSearchLive() {
        return TAPDatabaseManager.getInstance().getRecentSearchLive();
    }

    // My Contact
    public void getMyContactList(TAPDatabaseListener<TAPUserModel> listener) {
        TAPDatabaseManager.getInstance().getMyContactList(listener);
    }

    public LiveData<List<TAPUserModel>> getMyContactList() {
        return TAPDatabaseManager.getInstance().getMyContactList();
    }

    public void searchAllMyContacts(String keyword, TAPDatabaseListener<TAPUserModel> listener) {
        TAPDatabaseManager.getInstance().searchAllMyContacts(keyword, listener);
    }

    public void insertMyContactToDatabase(TAPUserModel... userModels) {
        TAPDatabaseManager.getInstance().insertMyContact(userModels);
    }

    public void insertMyContactToDatabase(TAPDatabaseListener<TAPUserModel> listener, TAPUserModel... userModels) {
        TAPDatabaseManager.getInstance().insertMyContact(listener, userModels);
    }

    public void insertMyContactToDatabase(List<TAPUserModel> userModels) {
        TAPDatabaseManager.getInstance().insertMyContact(userModels);
    }

    // Set isContact value to 0 or 1 then insert user model to database
    public void checkContactAndInsertToDatabase(TAPUserModel userModel) {
        TAPDatabaseManager.getInstance().checkContactAndInsert(userModel);
    }

    public void getUserWithXcUserID(String xcUserID, TAPDatabaseListener<TAPUserModel> listener) {
        TAPDatabaseManager.getInstance().getUserWithXcUserID(xcUserID, listener);
    }

    public void insertAndGetMyContact(List<TAPUserModel> userModels, TAPDatabaseListener<TAPUserModel> listener) {
        TAPDatabaseManager.getInstance().insertAndGetMyContact(userModels, listener);
    }

    public void deleteMyContactFromDatabase(TAPUserModel... userModels) {
        TAPDatabaseManager.getInstance().deleteMyContact(userModels);
    }

    public void deleteMyContactFromDatabase(List<TAPUserModel> userModels) {
        TAPDatabaseManager.getInstance().deleteMyContact(userModels);
    }

    public void deleteAllContact() {
        TAPDatabaseManager.getInstance().deleteAllContact();
    }

    public void updateMyContact(TAPUserModel userModels) {
        TAPDatabaseManager.getInstance().updateMyContact(userModels);
    }

    // FIXME: 25 October 2018 MAKE FUNCTION RETURN BOOLEAN OR GET FRIEND STATUS FROM API
    public void checkUserInMyContacts(String userID, TAPDatabaseListener<TAPUserModel> listener) {
        TAPDatabaseManager.getInstance().checkUserInMyContacts(userID, listener);
    }

    public void getAllUserData(TAPDatabaseListener<TAPUserModel> listener) {
        TAPDatabaseManager.getInstance().getAllUserData(listener);
    }

    //General
    public void deleteAllFromDatabase() {
        deleteAllMessage();
        deleteAllRecentSearch();
        deleteAllContact();
    }

    /**
     * =========================================================================================== *
     * API CALLS
     * =========================================================================================== *
     */

    public void getAuthTicket(String ipAddress, String userAgent, String userPlatform, String userDeviceID, String xcUserID
            , String fullname, String email, String phone, String username, TapDefaultDataView<TAPAuthTicketResponse> view) {
        TAPApiManager.getInstance().getAuthTicket(ipAddress, userAgent, userPlatform, userDeviceID, xcUserID,
                fullname, email, phone, username, new TAPDefaultSubscriber<>(view));
    }

    public void sendCustomMessage(Integer messageType, String body, String filterID, String senderUserID, String recipientUserID, TapDefaultDataView<TAPSendCustomMessageResponse> view) {
        TAPApiManager.getInstance().sendCustomMessage(messageType, body, filterID, senderUserID, recipientUserID, new TAPDefaultSubscriber<>(view));
    }

    public void getAccessTokenFromApi(TapDefaultDataView<TAPGetAccessTokenResponse> view) {
        TAPApiManager.getInstance().getAccessToken(new TAPDefaultSubscriber<>(view));
    }

    public void refreshAccessToken(TapDefaultDataView<TAPGetAccessTokenResponse> view) {
        TAPApiManager.getInstance().refreshAccessToken(new TAPDefaultSubscriber<>(view));
    }

    public void validateAccessToken(TapDefaultDataView<TAPErrorModel> view) {
        if (TAPDataManager.getInstance().checkAccessTokenAvailable())
            TAPApiManager.getInstance().validateAccessToken(new TAPDefaultSubscriber<>(view));
    }

    public void registerFcmTokenToServer(String fcmToken, TapDefaultDataView<TAPCommonResponse> view) {
        TAPApiManager.getInstance().registerFcmTokenToServer(fcmToken, new TAPDefaultSubscriber<>(view));
    }

    public void getMessageRoomListAndUnread(String userID, TapDefaultDataView<TAPGetRoomListResponse> view) {
        TAPApiManager.getInstance().getRoomList(userID, new TAPDefaultSubscriber<>(view));
    }

    public void getNewAndUpdatedMessage(TapDefaultDataView<TAPGetRoomListResponse> view) {
        TAPApiManager.getInstance().getPendingAndUpdatedMessage(new TAPDefaultSubscriber<>(view));
    }

    public void getMessageListByRoomAfter(String roomID, Long minCreated, Long lastUpdated, TapDefaultDataView<TAPGetMessageListByRoomResponse> view) {
        TAPApiManager.getInstance().getMessageListByRoomAfter(roomID, minCreated, lastUpdated, new TAPDefaultSubscriber<>(view));
    }

    public void getMessageListByRoomBefore(String roomID, Long maxCreated, TapDefaultDataView<TAPGetMessageListByRoomResponse> view) {
        TAPApiManager.getInstance().getMessageListByRoomBefore(roomID, maxCreated, new TAPDefaultSubscriber<>(view));
    }

    public void updateMessageStatusAsDelivered(List<String> messageIDs, TapDefaultDataView<TAPUpdateMessageStatusResponse> view) {
        TAPApiManager.getInstance().updateMessageStatusAsDelivered(messageIDs, new TAPDefaultSubscriber<>(view));
    }

    public void updateMessageStatusAsRead(List<String> messageIDs, TapDefaultDataView<TAPUpdateMessageStatusResponse> view) {
        TAPApiManager.getInstance().updateMessageStatusAsRead(messageIDs, new TAPDefaultSubscriber<>(view));
    }

    public void getMyContactListFromAPI(TapDefaultDataView<TAPContactResponse> view) {
        TAPApiManager.getInstance().getMyContactListFromAPI(new TAPDefaultSubscriber<>(view));
    }

    public void addContactApi(String userID, TapDefaultDataView<TAPCommonResponse> view) {
        TAPApiManager.getInstance().addContact(userID, new TAPDefaultSubscriber<>(view));
    }

    public void removeContactApi(String userID, TapDefaultDataView<TAPCommonResponse> view) {
        TAPApiManager.getInstance().removeContact(userID, new TAPDefaultSubscriber<>(view));
    }

    // Search User
    private TAPDefaultSubscriber<TAPBaseResponse<TAPGetUserResponse>, TapDefaultDataView<TAPGetUserResponse>, TAPGetUserResponse> searchUserSubscriber;

    public void getUserByIdFromApi(String id, TapDefaultDataView<TAPGetUserResponse> view) {
        TAPApiManager.getInstance().getUserByID(id, searchUserSubscriber = new TAPDefaultSubscriber<>(view));
    }

    public void getUserByXcUserIdFromApi(String xcUserID, TapDefaultDataView<TAPGetUserResponse> view) {
        TAPApiManager.getInstance().getUserByXcUserID(xcUserID, searchUserSubscriber = new TAPDefaultSubscriber<>(view));
    }

    public void getUserByUsernameFromApi(String username, TapDefaultDataView<TAPGetUserResponse> view) {
        TAPApiManager.getInstance().getUserByUsername(username, searchUserSubscriber = new TAPDefaultSubscriber<>(view));
    }

    public void getMultipleUsersByIdFromApi(List<String> ids, TapDefaultDataView<TAPGetMultipleUserResponse> view) {
        TAPApiManager.getInstance().getMultipleUserByID(ids, new TAPDefaultSubscriber<>(view));
    }

    // Upload File
    private HashMap<String, TAPDefaultSubscriber<TAPBaseResponse<TAPUploadFileResponse>, TapDefaultDataView<TAPUploadFileResponse>, TAPUploadFileResponse>> uploadSubscribers;

    public void uploadImage(String localID, File imageFile, String roomID, String caption, String mimeType,
                            ProgressRequestBody.UploadCallbacks uploadCallback,
                            TapDefaultDataView<TAPUploadFileResponse> view) {
        TAPApiManager.getInstance().uploadImage(imageFile, roomID, caption, mimeType, uploadCallback, getUploadSubscriber(roomID, localID, view));
    }

    public void uploadFile(String localID, File file, String roomID, String mimeType,
                           ProgressRequestBody.UploadCallbacks uploadCallback,
                           TapDefaultDataView<TAPUploadFileResponse> view) {
        TAPApiManager.getInstance().uploadFile(file, roomID, mimeType, uploadCallback, getUploadSubscriber(roomID, localID, view));
    }

    private HashMap<String, TAPDefaultSubscriber<TAPBaseResponse<TAPUploadFileResponse>, TapDefaultDataView<TAPUploadFileResponse>, TAPUploadFileResponse>>
    getUploadSubscribers() {
        return null == uploadSubscribers ? uploadSubscribers = new HashMap<>() : uploadSubscribers;
    }

    private TAPDefaultSubscriber<TAPBaseResponse<TAPUploadFileResponse>, TapDefaultDataView<TAPUploadFileResponse>, TAPUploadFileResponse>
    getUploadSubscriber(String roomID, String localID, TapDefaultDataView<TAPUploadFileResponse> view) {
        getUploadSubscribers().put(roomID, new TAPDefaultSubscriber<>(view, localID));
        return getUploadSubscribers().get(roomID);
    }

    public void unSubscribeToUploadImage(String roomID) {
        if (null == getUploadSubscribers().get(roomID)) {
            return;
        }
        getUploadSubscribers().get(roomID).unsubscribe();
    }

    public void cancelUploadImage(Context context, String localID) {
        Intent intent = new Intent(UploadCancelled);
        intent.putExtra(UploadLocalID, localID);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    // File Download
    private HashMap<String, TAPBaseSubscriber<TapDefaultDataView<ResponseBody>>> downloadSubscribers; // Key is message local ID

    public void downloadFile(String roomID, String localID, String fileID, TapDefaultDataView<ResponseBody> view) {
        TAPApiManager.getInstance().downloadFile(roomID, localID, fileID, getNewDownloadSubscriber(localID, view));
    }

    public void cancelFileDownload(String localID) {
        TAPBaseSubscriber downloadSubscriber = getDownloadSubscribers().get(localID);
        if (null != downloadSubscriber) {
            downloadSubscriber.unsubscribe();
            removeDownloadSubscriber(localID);
        }
    }

    public void removeDownloadSubscriber(String localID) {
        getDownloadSubscribers().remove(localID);
    }

    private HashMap<String, TAPBaseSubscriber<TapDefaultDataView<ResponseBody>>> getDownloadSubscribers() {
        return null == downloadSubscribers ? downloadSubscribers = new HashMap<>() : downloadSubscribers;
    }

    private TAPBaseSubscriber<TapDefaultDataView<ResponseBody>> getNewDownloadSubscriber(String localID, TapDefaultDataView<ResponseBody> view) {
        getDownloadSubscribers().put(localID, new TAPBaseSubscriber<>(view));
        return getDownloadSubscribers().get(localID);
    }

    // FIXME: 25 October 2018
    public void cancelUserSearchApiCall() {
        if (null != searchUserSubscriber) {
            searchUserSubscriber.unsubscribe();
        }
    }
}
