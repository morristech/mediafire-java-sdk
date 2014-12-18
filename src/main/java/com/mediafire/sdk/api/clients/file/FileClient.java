package com.mediafire.sdk.api.clients.file;

import com.mediafire.sdk.api.ApiRequestGenerator;
import com.mediafire.sdk.api.clients.ApiClient;
import com.mediafire.sdk.client_helpers.ClientHelperApi;
import com.mediafire.sdk.config.IHttp;
import com.mediafire.sdk.config.ITokenManager;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Result;

/**
 * Created by Chris Najar on 10/30/2014.
 */
public class FileClient {
    private static final String PARAM_QUICK_KEY = "quick_key";
    private static final String PARAM_FOLDER_KEY = "folder_key";
    private static final String PARAM_FILE_NAME = "filename";
    private static final String PARAM_DESCRIPTION = "description";
    private static final String PARAM_MTIME = "mtime";
    private static final String PARAM_PRIVACY = "privacy";
    private static final String PARAM_LINK_TYPE = "link_type";

    private final ApiRequestGenerator mApiRequestGenerator;
    private final ApiClient apiClient;

    public FileClient(IHttp httpWorker, ITokenManager ITokenManager) {
        mApiRequestGenerator = new ApiRequestGenerator();

        ClientHelperApi clientHelper = new ClientHelperApi(ITokenManager);
        apiClient = new ApiClient(clientHelper, httpWorker);
    }

    public Result getInfo(String quickKey) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/get_info.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);

        return apiClient.doRequest(request);
    }

    public Result delete(String quickKey) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/delete.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);

        return apiClient.doRequest(request);
    }

    public Result copy(String quickKey, String folderKey) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/copy.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);
        request.addQueryParameter(PARAM_FOLDER_KEY, folderKey);

        return apiClient.doRequest(request);
    }

    public Result copy(String quickKey) {
        return copy(quickKey, null);
    }

    public Result getVersion(String quickKey) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/get_version.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);

        return apiClient.doRequest(request);
    }

    public Result move(String quickKey, String folderKey) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/move.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);
        request.addQueryParameter(PARAM_FOLDER_KEY, folderKey);

        return apiClient.doRequest(request);
    }

    public Result move(String quickKey) {
        return move(quickKey, null);
    }

    public Result update(String quickKey, UpdateParameters params) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/update.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);
        request.addQueryParameter(PARAM_FILE_NAME, params.getFileName());
        request.addQueryParameter(PARAM_DESCRIPTION, params.getDescription());
        request.addQueryParameter(PARAM_PRIVACY, params.getPrivacy());
        request.addQueryParameter(PARAM_MTIME, params.getMtime());

        return apiClient.doRequest(request);
    }

    public Result rename(String quickKey, String newName) {
        UpdateParameters params = new UpdateParameters.Builder().fileName(newName).build();
        return update(quickKey, params);
    }

    public Result makePublic(String quickKey) {
        UpdateParameters params = new UpdateParameters.Builder().privacy(UpdateParameters.Builder.Privacy.PUBLIC).build();
        return update(quickKey, params);
    }

    public Result makePrivate(String quickKey) {
        UpdateParameters params = new UpdateParameters.Builder().privacy(UpdateParameters.Builder.Privacy.PRIVATE).build();
        return update(quickKey, params);
    }

    public Result getLinks(String quickKey, LinkType linkType) {
        Request request = mApiRequestGenerator.createRequestObjectFromPath("file/get_links.php");

        request.addQueryParameter(PARAM_QUICK_KEY, quickKey);
        if (linkType == null) {
            linkType = LinkType.ALL;
        }

        request.addQueryParameter(PARAM_LINK_TYPE, linkType.getLinkType());

        return apiClient.doRequest(request);
    }

    public Result getLinks(String quickKey) {
        return getLinks(quickKey, LinkType.ALL);
    }

}
