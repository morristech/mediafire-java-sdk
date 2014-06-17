package com.arkhive.components.test_session_manager_fixes.module_token_farm;

import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.ApiRequestObject;

/**
 * Created by Chris Najar on 6/16/2014.
 */
public interface TokenFarmDistributor {
    public void borrowSessionToken(ApiRequestObject apiRequestObject);
    public void returnSessionToken(ApiRequestObject apiRequestObject);
    public void receiveNewSessionToken(ApiRequestObject apiResponseObject);
}
