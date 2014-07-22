package com.mediafire.sdk.http;

import com.mediafire.sdk.api_responses.ApiResponse;
import com.mediafire.sdk.api_responses.user.GetActionTokenResponse;
import com.mediafire.sdk.api_responses.user.GetSessionTokenResponse;
import com.mediafire.sdk.config.MFConfiguration;
import com.mediafire.sdk.token.MFActionToken;
import com.mediafire.sdk.token.MFSessionToken;
import com.mediafire.sdk.token.MFTokenFarmCallback;

public final class MFHttpClientCleanup extends MFHttp {
    private static final String TAG = MFHttpClientCleanup.class.getCanonicalName();
    private final MFTokenFarmCallback mfTokenFarmCallback;

    public MFHttpClientCleanup(MFTokenFarmCallback mfTokenFarmCallback, MFConfiguration mfConfiguration) {
        super(mfConfiguration);
        this.mfTokenFarmCallback = mfTokenFarmCallback;
    }

    public void returnToken(MFRequester mfRequester, MFResponse mfResponse) throws MFHttpException {
        MFConfiguration.getStaticMFLogger().w(TAG, "returnToken(type: " + mfRequester.getTypeOfTokenToReturn().toString() + ")");
        if (mfResponse == null || mfResponse.getResponseObject(ApiResponse.class) == null) {
            return;
        }
        switch (mfRequester.getTypeOfTokenToReturn()) {
            case NEW:
                GetSessionTokenResponse newSessionTokenResponse = mfResponse.getResponseObject(GetSessionTokenResponse.class);
                MFSessionToken newSessionToken = createNewSessionToken(newSessionTokenResponse);
                mfTokenFarmCallback.receiveNewSessionToken(newSessionToken);
                break;
            case V2:
                ApiResponse apiResponse = mfResponse.getResponseObject(ApiResponse.class);
                if (apiResponse.hasError() && apiResponse.getError() == 105 || apiResponse.getError() == 127) {
                    MFConfiguration.getStaticMFLogger().w(TAG, "not session token was spoiled for request: " + mfRequester.getUri() + mfRequester.getRequestParameters().toString() + " using session token: " + mfRequester.getToken().toString());
                    mfTokenFarmCallback.sessionTokenSpoiled((MFSessionToken) mfResponse.getOriginMFRequester().getToken());
                } else {
                    if (apiResponse.needNewKey()) {
                        MFConfiguration.getStaticMFLogger().w(TAG, "original session token borrowed for request " + mfRequester.getUri() + mfRequester.getRequestParameters().toString() + ": " + mfRequester.getToken().toString());
                        ((MFSessionToken) mfRequester.getToken()).updateSessionToken();
                        MFConfiguration.getStaticMFLogger().w(TAG, "returning updated session token: " + mfRequester.getToken().toString() + " for request: " + mfRequester.getUri() + mfRequester.getRequestParameters().toString());
                    }
                    mfTokenFarmCallback.returnSessionToken(((MFSessionToken) mfRequester.getToken()));
                }
                break;
            case UPLOAD:
                GetActionTokenResponse uploadActionTokenResponse = mfResponse.getResponseObject(GetActionTokenResponse.class);
                if (uploadActionTokenResponse.hasError() && uploadActionTokenResponse.getError() == 105 || uploadActionTokenResponse.getError() == 127) {
                    mfTokenFarmCallback.sessionTokenSpoiled((MFSessionToken) mfResponse.getOriginMFRequester().getToken());
                } else {
                    MFActionToken mfUploadActionToken = createActionToken(MFActionToken.Type.UPLOAD, uploadActionTokenResponse, mfRequester);
                    MFConfiguration.getStaticMFLogger().w(TAG, "returning new upload action token: " + mfUploadActionToken.toString());
                    mfTokenFarmCallback.receiveNewUploadActionToken(mfUploadActionToken);
                }
                break;
            case IMAGE:
                GetActionTokenResponse imageActionTokenResponse = mfResponse.getResponseObject(GetActionTokenResponse.class);
                if (imageActionTokenResponse.hasError() && imageActionTokenResponse.getError() == 105 || imageActionTokenResponse.getError() == 127) {
                    mfTokenFarmCallback.sessionTokenSpoiled((MFSessionToken) mfResponse.getOriginMFRequester().getToken());
                } else {
                    MFActionToken mfImageActionToken = createActionToken(MFActionToken.Type.IMAGE, imageActionTokenResponse, mfRequester);
                    MFConfiguration.getStaticMFLogger().w(TAG, "returning new image action token: " + mfImageActionToken.toString());
                    mfTokenFarmCallback.receiveNewImageActionToken(mfImageActionToken);
                }
                break;
            case NONE:
                // for types NONE
                // there is no need to return a token
                MFConfiguration.getStaticMFLogger().w(TAG, "not returning a token for request: " + mfRequester.getUri() + mfRequester.getRequestParameters().toString());
                break;
        }
    }

    public MFActionToken createActionToken(MFActionToken.Type type, GetActionTokenResponse getActionTokenResponse, MFRequester mfRequester) {
        MFConfiguration.getStaticMFLogger().w(TAG, "createActionToken()");
        if (getActionTokenResponse == null) {
            MFConfiguration.getStaticMFLogger().w(TAG, "response was null, returning null");
            return null;
        }

        if (getActionTokenResponse.hasError()) {
            MFConfiguration.getStaticMFLogger().w(TAG, "response had error, returning null");
            return null;
        }

        String tokenString = getActionTokenResponse.getActionToken();
        long tokenExpiry;
        if (mfRequester.getRequestParameters().containsKey("lifespan")) {
            tokenExpiry = Long.valueOf(mfRequester.getRequestParameters().get("lifespan"));
        } else {
            tokenExpiry = 0;
        }
        MFActionToken actionToken = new MFActionToken(tokenString, type, tokenExpiry);
        MFConfiguration.getStaticMFLogger().w(TAG, "created new upload action token: " + actionToken.toString());
        return actionToken;
    }

    public MFSessionToken createNewSessionToken(GetSessionTokenResponse getSessionTokenResponse) throws MFHttpException {
        MFConfiguration.getStaticMFLogger().w(TAG, "createNewSessionToken()");
        if (getSessionTokenResponse == null) {
            MFConfiguration.getStaticMFLogger().w(TAG, "response was null, returning null");
            return null;
        }

        if (getSessionTokenResponse.hasError()) {
            MFConfiguration.getStaticMFLogger().w(TAG, "response had error, returning error");
            throw new MFHttpException("error: " + getSessionTokenResponse.getMessage());
        }

        String tokenString = getSessionTokenResponse.getSessionToken();
        String secretKey = getSessionTokenResponse.getSecretKey();
        String time = getSessionTokenResponse.getTime();
        String pkey = getSessionTokenResponse.getPkey();
        String ekey = getSessionTokenResponse.getEkey();
        MFSessionToken mfSessionToken = new MFSessionToken(tokenString, secretKey, time, pkey, ekey);
        MFConfiguration.getStaticMFLogger().w(TAG, "created new token: " + mfSessionToken.toString());
        return mfSessionToken;
    }
}
