package com.arkhive.components.test_session_manager_fixes.module_http_processor.request_runnables;

import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.ApiRequestObject;

/**
 * Created by Chris Najar on 6/16/2014.
 */
public interface HttpRequestCallback {
    public void httpRequestStarted(ApiRequestObject apiRequestObject);
    public void httpRequestFinished(ApiRequestObject apiRequestObject);
}
