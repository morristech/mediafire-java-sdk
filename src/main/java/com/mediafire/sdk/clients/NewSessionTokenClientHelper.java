package com.mediafire.sdk.clients;

import com.mediafire.sdk.api_responses.user.GetSessionTokenResponse;
import com.mediafire.sdk.config.CredentialsInterface;
import com.mediafire.sdk.config.SessionTokenManagerInterface;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Response;
import com.mediafire.sdk.token.SessionToken;

import java.util.Map;

/**
 * Created by Chris on 11/9/2014.
 * BaseClientHelper used with ApiClient to get new session tokens.
 */
public class NewSessionTokenClientHelper extends BaseClientHelper {
    private CredentialsInterface mUserCredentials;
    private CredentialsInterface mDeveloperCredentials;
    private SessionTokenManagerInterface mSessionTokenManagerInterface;

    public NewSessionTokenClientHelper(CredentialsInterface userCredentials, CredentialsInterface developerCredentials, SessionTokenManagerInterface sessionTokenManagerInterface) {
        super();
        mUserCredentials = userCredentials;
        mDeveloperCredentials = developerCredentials;
        mSessionTokenManagerInterface = sessionTokenManagerInterface;
    }
    
    @Override
    public void borrowToken(Request request) {
        // no token needs to be borrowed for new session tokens
    }

    @Override
    public void addSignatureToRequestParameters(Request request) {
        addRequiredParametersForNewSessionToken(request);
        String signature = makeSignatureForNewSessionToken();
        request.addSignature(signature);
    }

    @Override
    public void returnToken(Response response, Request request) {
        GetSessionTokenResponse newSessionTokenResponse = getResponseObject(response, GetSessionTokenResponse.class);
        SessionToken newSessionToken = createNewSessionToken(newSessionTokenResponse);
        if (newSessionToken != null) {
            mSessionTokenManagerInterface.receiveSessionToken(newSessionToken);
        }
    }

    private String makeSignatureForNewSessionToken() {
        // email + password + app id + api key
        // fb access token + app id + api key
        // tw oauth token + tw oauth token secret + app id + api key
        String userInfoPortionOfHashTarget = mUserCredentials.getConcatenatedCredentials();

        // apiKey is not required, but may be passed into the MFConfiguration object
        // Note: If the app does not have the "Require Secret Key" option checked,
        // then the API key may be omitted from the signature.
        // However, this should only be done when sufficient domain and/or network restrictions are in place.
        String hashTarget = userInfoPortionOfHashTarget + mDeveloperCredentials.getConcatenatedCredentials();

        System.out.println(TAG + " - makeSignatureForNewSessionToken - pre-hash: " + hashTarget);
        String signature = hashString(hashTarget, "SHA-1");
        System.out.println(TAG + " - makeSignatureForNewSessionToken - hashed: " + signature);
        return signature;
    }

    private void addRequiredParametersForNewSessionToken(Request request) {
        System.out.println(TAG + " - addRequiredParametersForNewSessionToken");
        Map<String, String> credentialsMap = mUserCredentials.getCredentials();
        for (String key : credentialsMap.keySet()) {
            request.addQueryParameter(key, credentialsMap.get(key));
        }

        request.addQueryParameter("application_id", mDeveloperCredentials.getCredentials().get("application_id"));

        System.out.println(TAG + " - Request parameters: " + request.getQueryParameters());
    }

    /**
     * Creates a SessionToken Object from a GetSessionTokenResponse
     * @param getSessionTokenResponse the response to create a SessionToken from
     * @return a new SessionToken Object
     */
    private SessionToken createNewSessionToken(GetSessionTokenResponse getSessionTokenResponse) {

        if (getSessionTokenResponse == null) {
            System.out.println(TAG + " - createNewSessionToken - response null, returning null");
            return null;
        }

        if (getSessionTokenResponse.hasError()) {
            System.out.println(TAG + " - createNewSessionToken - response has error, returning null");
            return null;
        }

        String tokenString = getSessionTokenResponse.getSessionToken();
        String secretKey = getSessionTokenResponse.getSecretKey();
        String time = getSessionTokenResponse.getTime();
        String pkey = getSessionTokenResponse.getPkey();
        String ekey = getSessionTokenResponse.getEkey();

        return new SessionToken(tokenString, secretKey, time, pkey, ekey);
    }
}