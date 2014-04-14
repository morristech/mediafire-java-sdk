package com.arkhive.components.api.filesystem.device;

import java.util.HashMap;
import java.util.Map;

//CHECKSTYLE:OFF
import com.google.gson.Gson;
//CHECKSTYLE:ON

import com.arkhive.components.api.ApiRequest;
import com.arkhive.components.api.ApiRequestBuilder;
import com.arkhive.components.api.Utility;
import com.arkhive.components.api.filesystem.FileSystemItem;
import com.arkhive.components.sessionmanager.SessionManager;

/**
 * class for making api calls to Device apis.
 * @author Chris Najar
 *
 */
public class Device {
  private static final String GET_CHANGES_URI = "/api/1.0/device/get_changes.php";
  public static DeviceGetChangesResponse getChanges(FileSystemItem folder, SessionManager sm) {
    Map<String, String> map = new HashMap<String, String>();
    map.put("revision", String.valueOf(folder.getRevision()));
    
    ApiRequestBuilder builder = new ApiRequestBuilder();
    builder.domain(sm.getDomain());
    builder.uri(GET_CHANGES_URI);
    builder.sessionManager(sm);
    builder.httpInterface(sm.getHttpInterface());
    builder.parameters(map);
    
    ApiRequest request = builder.build();
    
    String responseString = request.submitRequestSync();
    
    Gson gson = new Gson();
    DeviceGetChangesResponse response = 
        gson.fromJson(Utility.getResponseString(responseString), DeviceGetChangesResponse.class);
    
    return response;
  }
}
