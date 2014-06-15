package com.arkhive.components.test_session_manager_fixes.module_api_response;

/**
 * Created by Chris Najar on 6/15/2014.
 */
public class ApiResponse {
    private String action;
    private String message;
    private String result;
    private String error;
    private String current_api_version;

    public final String getAction() {
        return action;
    }

    public final String getMessage() {
        return message;
    }

    public final String getError() {
        return error;
    }

    public final String getResult() {
        return result;
    }

    public final String getCurrentApiVersion() {
        return current_api_version;
    }

    public final boolean hasError() {
        return error == null;
    }
}
