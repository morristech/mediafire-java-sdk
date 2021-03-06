package com.mediafire.sdk.response_models.file;

import com.mediafire.sdk.response_models.ApiResponse;
import com.mediafire.sdk.response_models.data_models.MyFilesRevisionModel;

public class FileDeleteResponse extends ApiResponse {
    private MyFilesRevisionModel myfiles_revision;
    private long device_revision;

    public long getDeviceRevision() {
        return device_revision;
    }

    public MyFilesRevisionModel getMyFilesRevision() {
        return this.myfiles_revision;
    }

}
