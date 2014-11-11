package com.mediafire.sdk.clients;

import com.mediafire.sdk.http.ApiVersion;
import com.mediafire.sdk.http.Request;

import java.util.HashMap;

/**
 * Created by Chris on 11/9/2014.
 */
public class ApiRequestGenerator {

    private final String mApiVersion;

    public ApiRequestGenerator(String apiVersion) {
        mApiVersion = apiVersion;
    }

    public ApiRequestGenerator() {
        this(ApiVersion.VERSION_CURRENT);
    }

    public Request createRequestObjectFromPath(String path) {
        String fullPath = "api/";

        if (mApiVersion != null) {
            fullPath += mApiVersion + "/";
        }

        fullPath += path;

        Request.Builder builder = new Request.Builder();
        builder.scheme("https").fullDomain("www.mediafire.com").httpMethod("post").path(fullPath);

        if (path.equalsIgnoreCase("upload/resumable.php")) {
            builder.postQuery(false);
        } else {
            builder.postQuery(true);
        }

        return builder.build();
    }
}