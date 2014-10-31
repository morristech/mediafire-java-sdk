package com.mediafire.sdk.clients.file;

/**
 * Created by Chris Najar on 10/30/2014.
 */
public enum LinkType {
    VIEW("view"),
    EDIT("edit"),
    NORMAL_DOWNLOAD("normal_download"),
    DIRECT_DOWNLOAD("direct_download"),
    ONE_TIME_DOWNLOAD("one_time"),
    LISTEN("listen"),
    WATCH("watch"),
    STREAMING("streaming"),
    ALL(null);

    private String mLinkType;

    private LinkType(String value) {
        mLinkType = value;
    }

    public String getLinkType() {
        return mLinkType;
    }
}