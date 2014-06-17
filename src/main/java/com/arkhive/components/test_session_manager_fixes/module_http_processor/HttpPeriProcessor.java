package com.arkhive.components.test_session_manager_fixes.module_http_processor;

import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.ApiRequestObject;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.request_runnables.HttpGetRequestRunnable;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.request_runnables.HttpPostRequestRunnable;
import com.arkhive.components.test_session_manager_fixes.module_http_processor.request_runnables.HttpRequestCallback;
import com.arkhive.components.uploadmanager.PausableThreadPoolExecutor;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Chris Najar on 6/15/2014.
 */
public final class HttpPeriProcessor {
    private static final String TAG = HttpPeriProcessor.class.getSimpleName();
    private final int connectionTimeout;
    private final int readTimeout;
    private final HttpPreProcessor httpPreProcessor;
    private final HttpPostProcessor httpPostProcessor;
    private BlockingQueue<Runnable> workQueue;
    private PausableThreadPoolExecutor executor;

    public HttpPeriProcessor(int connectionTimeout, int readTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        httpPreProcessor = new HttpPreProcessor();
        httpPostProcessor = new HttpPostProcessor();
        workQueue = new LinkedBlockingQueue<Runnable>();
        executor = new PausableThreadPoolExecutor(20, workQueue);
    }

    public HttpPreProcessor getHttpPreProcessor() {
        return httpPreProcessor;
    }

    public HttpPostProcessor getHttpPostProcessor() {
        return httpPostProcessor;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void sendGetRequest(HttpRequestCallback callback, ApiRequestObject apiRequestObject) {
        System.out.println(TAG + " sendGetRequest()");
        HttpGetRequestRunnable httpGetRequestRunnable = new HttpGetRequestRunnable(callback, apiRequestObject, this);
        executor.execute(httpGetRequestRunnable);
    }

    public void sendPostRequest(HttpRequestCallback callback, ApiRequestObject apiRequestObject) {
        System.out.println(TAG + " sendPostRequest()");
        HttpPostRequestRunnable httpPostRequestRunnable = new HttpPostRequestRunnable(callback, apiRequestObject, this);
        executor.execute(httpPostRequestRunnable);
    }
}
