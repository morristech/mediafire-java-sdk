package com.mediafire.sdk.api.clients;

import com.mediafire.sdk.api.ApiRequestGenerator;
import com.mediafire.sdk.api.helpers.Instructions;
import com.mediafire.sdk.api.helpers.NoToken;
import com.mediafire.sdk.config.HttpHandler;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Result;

public class SystemClient {

    private final ApiClient apiClient;
    private final Instructions mInstructions;

    public SystemClient(HttpHandler httpInterface) {
        mInstructions = new NoToken();
        apiClient = new ApiClient(httpInterface);
    }

    public Result getInfo() {
        Request request = ApiRequestGenerator.createRequestObjectFromPath("system/get_info.php");
        return apiClient.doRequest(mInstructions, request);
    }
}
