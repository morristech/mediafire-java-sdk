package com.arkhive.components.test_session_manager_fixes.module_token_farm.runnables;

import com.arkhive.components.test_session_manager_fixes.Configuration;
import com.arkhive.components.test_session_manager_fixes.module_api.Api;
import com.arkhive.components.test_session_manager_fixes.module_api.ApiUris;
import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.ApiRequestObject;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.HttpPeriProcessor;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.interfaces.HttpProcessor;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.interfaces.HttpRequestCallback;
import com.arkhive.components.test_session_manager_fixes.module_token_farm.interfaces.GetNewActionTokenCallback;
import com.arkhive.components.test_session_manager_fixes.module_token_farm.interfaces.SessionTokenDistributor;
import com.arkhive.components.test_session_manager_fixes.module_api.responses.GetActionTokenResponse;
import com.arkhive.components.test_session_manager_fixes.module_token_farm.tokens.ActionToken;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Chris Najar on 6/19/2014.
 */
public class GetImageActionTokenRunnable implements Runnable, HttpRequestCallback {
    private static final String TAG = GetImageActionTokenRunnable.class.getSimpleName();
    private static final String REQUIRED_PARAMETER_TYPE = "type";
    private static final String OPTIONAL_PARAMETER_LIFESPAN = "lifespan";
    private static final String OPTIONAL_PARAMETER_RESPONSE_FORMAT = "response_format";
    private final HttpProcessor httpPreProcessor;
    private final HttpProcessor httpPostProcessor;
    private final GetNewActionTokenCallback actionTokenCallback;
    private SessionTokenDistributor sessionTokenDistributor;
    private HttpPeriProcessor httpPeriProcessor;
    private final Logger logger = LoggerFactory.getLogger(GetImageActionTokenRunnable.class);

    public GetImageActionTokenRunnable(HttpProcessor httpPreProcessor,
                                       HttpProcessor httpPostProcessor,
                                       SessionTokenDistributor sessionTokenDistributor,
                                       GetNewActionTokenCallback actionTokenCallback,
                                       HttpPeriProcessor httpPeriProcessor) {
        this.httpPreProcessor = httpPreProcessor;
        this.httpPostProcessor = httpPostProcessor;
        this.sessionTokenDistributor = sessionTokenDistributor;
        this.actionTokenCallback = actionTokenCallback;
        this.httpPeriProcessor = httpPeriProcessor;
    }

    @Override
    public void run() {
        logger.info(" sendRequest()");
        synchronized (this) {
            // set up our api request object
            ApiRequestObject apiRequestObject = setupApiRequestObjectForNewImageActionToken();
            // borrow a session token from the TokenFarm
            sessionTokenDistributor.borrowSessionToken(apiRequestObject);
            // send request to http handler
            httpPeriProcessor.sendGetRequest(this, httpPreProcessor, httpPostProcessor, apiRequestObject);
            // wait until we get a response from http handler (notify will be called in callback implementation)
            try {
                wait(Configuration.DEFAULT_HTTP_CONNECTION_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // try to return the session token to the TokenFarm
            sessionTokenDistributor.returnSessionToken(apiRequestObject);
            // try to give action token to the TokenFarm
            GetActionTokenResponse response = new Gson().fromJson(Api.getResponseString(apiRequestObject.getHttpResponseString()), GetActionTokenResponse.class);
            if (response != null && !response.hasError()) {
                ActionToken actionToken = ActionToken.newInstance(ActionToken.Type.IMAGE);
                String actionTokenString = response.getActionToken();
                if (actionTokenString != null) {
                    actionToken.setTokenString(response.getActionToken());
                    actionToken.setExpiration(System.currentTimeMillis() + 86400000);
                    apiRequestObject.setActionToken(actionToken);
                    apiRequestObject.setActionTokenInvalid(false);
                } else {
                    apiRequestObject.setActionTokenInvalid(true);
                }
            } else {
                apiRequestObject.setActionTokenInvalid(true);
            }

            actionTokenCallback.receiveNewImageActionToken(apiRequestObject);
        }
    }

    private ApiRequestObject setupApiRequestObjectForNewImageActionToken() {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_USER_GET_ACTION_TOKEN);
        apiRequestObject.setRequiredParameters(constructRequiredParameters());
        apiRequestObject.setOptionalParameters(constructOptionalParameters());
        return apiRequestObject;
    }

    private Map<String, String> constructRequiredParameters() {
        Map<String, String> requiredParameters = new LinkedHashMap<String, String>();
        requiredParameters.put(REQUIRED_PARAMETER_TYPE, "image");
        return requiredParameters;
    }

    public static Map<String, String> constructOptionalParameters() {
        Map<String, String> optionalParameters = new LinkedHashMap<String, String>();
        optionalParameters.put(OPTIONAL_PARAMETER_LIFESPAN, "1440");
        optionalParameters.put(OPTIONAL_PARAMETER_RESPONSE_FORMAT, "json");
        return optionalParameters;
    }

    @Override
    public void httpRequestStarted(ApiRequestObject apiRequestObject) {
        logger.info(" httpRequestStarted()");
    }

    @Override
    public void httpRequestFinished(ApiRequestObject apiRequestObject) {
        logger.info(" httpRequestFinished()");
        synchronized (this) {
            notify();
        }
    }
}
