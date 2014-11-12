package com.mediafire.sdk.clients.user;

import com.mediafire.sdk.clients.ApiClient;
import com.mediafire.sdk.clients.ApiRequestGenerator;
import com.mediafire.sdk.clients.ClientHelperNewSessionToken;
import com.mediafire.sdk.clients.ClientHelperNoToken;
import com.mediafire.sdk.config.CredentialsInterface;
import com.mediafire.sdk.config.HttpWorkerInterface;
import com.mediafire.sdk.config.SessionTokenManagerInterface;
import com.mediafire.sdk.http.ApiVersion;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Result;

/**
 * Created by Chris on 11/11/2014.
 */
public class SessionTokenClient {
    private static final String PARAM_TOKEN_VERSION = "token_version";

    private final ApiRequestGenerator mApiRequestGenerator;
    private final HttpWorkerInterface mHttpWorker;
    private final ApiClient apiClient;

    public SessionTokenClient(HttpWorkerInterface httpWorkerInterface, CredentialsInterface userCredentials, CredentialsInterface developerCredentials, SessionTokenManagerInterface sessionTokenManager) {
        mHttpWorker = httpWorkerInterface;
        mApiRequestGenerator = new ApiRequestGenerator(ApiVersion.VERSION_1_2);

        ClientHelperNewSessionToken clientHelper = new ClientHelperNewSessionToken(userCredentials, developerCredentials, sessionTokenManager);
        apiClient = new ApiClient(clientHelper, mHttpWorker);
    }

    public Result getSessionTokenV2() {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("user/get_session_token.php");
        // add application_id and relative parameters are added by ApiClientHelper
        request.addQueryParameter(PARAM_TOKEN_VERSION, 2);

        return apiClient.doRequest(request);
    }
}
