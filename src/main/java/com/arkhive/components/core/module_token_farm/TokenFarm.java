package com.arkhive.components.core.module_token_farm;

import com.arkhive.components.core.Configuration;
import com.arkhive.components.core.module_api_descriptor.ApiRequestObject;
import com.arkhive.components.core.module_credentials.ApplicationCredentials;
import com.arkhive.components.core.module_http_processor.HttpPeriProcessor;
import com.arkhive.components.core.module_http_processor.pre_and_post_processors.ApiRequestHttpPostProcessor;
import com.arkhive.components.core.module_http_processor.pre_and_post_processors.ApiRequestHttpPreProcessor;
import com.arkhive.components.core.module_http_processor.pre_and_post_processors.NewSessionTokenHttpPostProcessor;
import com.arkhive.components.core.module_http_processor.pre_and_post_processors.NewSessionTokenHttpPreProcessor;
import com.arkhive.components.core.module_token_farm.interfaces.ActionTokenDistributor;
import com.arkhive.components.core.module_token_farm.interfaces.GetNewActionTokenCallback;
import com.arkhive.components.core.module_token_farm.interfaces.GetNewSessionTokenCallback;
import com.arkhive.components.core.module_token_farm.interfaces.SessionTokenDistributor;
import com.arkhive.components.core.module_token_farm.runnables.GetImageActionTokenRunnable;
import com.arkhive.components.core.module_token_farm.runnables.GetSessionTokenRunnable;
import com.arkhive.components.core.module_token_farm.runnables.GetUploadActionTokenRunnable;
import com.arkhive.components.core.module_token_farm.tokens.ActionToken;
import com.arkhive.components.core.module_token_farm.tokens.SessionToken;
import com.arkhive.components.uploadmanager.PausableThreadPoolExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by  on 6/16/2014.
 */
public class TokenFarm implements SessionTokenDistributor, GetNewSessionTokenCallback, ActionTokenDistributor, GetNewActionTokenCallback {
    private static final String TAG = TokenFarm.class.getCanonicalName();
    private final Lock lockBorrowImageToken = new ReentrantLock();
    private final Lock lockBorrowUploadToken = new ReentrantLock();
    private final Condition conditionImageTokenNotExpired = lockBorrowImageToken.newCondition();
    private final Condition conditionUploadTokenNotExpired = lockBorrowUploadToken.newCondition();
    private final ApplicationCredentials applicationCredentials;
    private final HttpPeriProcessor httpPeriProcessor;
    private final PausableThreadPoolExecutor executor;
    private final BlockingQueue<com.arkhive.components.core.module_token_farm.tokens.SessionToken> sessionTokens;
    private ActionToken uploadActionToken;
    private ActionToken imageActionToken;
    private final Object imageTokenLock = new Object();
    private final Object uploadTokenLock = new Object();

    public TokenFarm(Configuration configuration, ApplicationCredentials applicationCredentials, HttpPeriProcessor httpPeriProcessor) {
        int minimumSessionTokens = configuration.getMinimumSessionTokens();
        int maximumSessionTokens = configuration.getMaximumSessionTokens();
        sessionTokens = new LinkedBlockingQueue<com.arkhive.components.core.module_token_farm.tokens.SessionToken>(maximumSessionTokens);
        this.applicationCredentials = applicationCredentials;
        this.httpPeriProcessor = httpPeriProcessor;
        executor = new PausableThreadPoolExecutor(10, 10, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory());
    }

    public void getNewSessionToken(GetNewSessionTokenCallback getNewSessionTokenCallback) {
        Configuration.getErrorTracker().i(TAG, "getNewSessionToken()");
        Configuration.getErrorTracker().i(TAG, "getNewSessionToken() on thread: " + Thread.currentThread().getName());
        GetSessionTokenRunnable getSessionTokenRunnable =
                new GetSessionTokenRunnable(
                        getNewSessionTokenCallback,
                        new NewSessionTokenHttpPreProcessor(),
                        new NewSessionTokenHttpPostProcessor(),
                        httpPeriProcessor,
                        applicationCredentials);
        executor.execute(getSessionTokenRunnable);
    }

    private void getNewImageActionToken(SessionTokenDistributor sessionTokenDistributor, GetNewActionTokenCallback actionTokenCallback) {
        Configuration.getErrorTracker().i(TAG, "getNewImageActionToken()");
        GetImageActionTokenRunnable getImageActionTokenRunnable =
                new GetImageActionTokenRunnable(
                        new ApiRequestHttpPreProcessor(),
                        new ApiRequestHttpPostProcessor(),
                        sessionTokenDistributor,
                        actionTokenCallback,
                        httpPeriProcessor);
        executor.execute(getImageActionTokenRunnable);
    }

    private void getNewUploadActionToken(GetNewActionTokenCallback actionTokenCallback, SessionTokenDistributor sessionTokenDistributor) {
        Configuration.getErrorTracker().i(TAG, "getNewUploadActionToken()");
        GetUploadActionTokenRunnable getUploadActionTokenRunnable =
                new GetUploadActionTokenRunnable(
                        new ApiRequestHttpPreProcessor(),
                        new ApiRequestHttpPostProcessor(),
                        actionTokenCallback,
                        sessionTokenDistributor,
                        httpPeriProcessor);
        executor.execute(getUploadActionTokenRunnable);
    }

    public void startup() {
        executor.resume();
        Configuration.getErrorTracker().i(TAG, "startup()");
        for (int i = 0; i < sessionTokens.remainingCapacity(); i++) {
            getNewSessionToken(this);
        }
        getNewImageActionToken(this, this);
        getNewUploadActionToken(this, this);
    }

    public void shutdown() {
        Configuration.getErrorTracker().i(TAG, "TokenFarm shutting down");
        executor.pause();
        sessionTokens.clear();
        imageActionToken = null;
        uploadActionToken = null;
    }

    /*
        GetSessionTokenCallback interface methods
     */
    @Override
    public void receiveNewSessionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "receiveNewSessionToken()");
        SessionToken sessionToken = apiRequestObject.getSessionToken();
        if (apiRequestObject != null && !apiRequestObject.isSessionTokenInvalid() && sessionToken != null) {
            try {
                sessionTokens.add(sessionToken);
                Configuration.getErrorTracker().i(TAG, "added " + sessionToken.getTokenString());
            } catch (IllegalStateException e) {
                Configuration.getErrorTracker().i(TAG, "interrupted, not adding: " + sessionToken.getTokenString());
                getNewSessionToken(this);
            }
        } else if (apiRequestObject != null && apiRequestObject.getApiResponse() != null && apiRequestObject.getApiResponse().getError() == 107) {
            Configuration.getErrorTracker().i(TAG, "api message: " + apiRequestObject.getApiResponse().getMessage());
            Configuration.getErrorTracker().i(TAG, "api error: " + apiRequestObject.getApiResponse().getError());
            Configuration.getErrorTracker().i(TAG, "api result: " + apiRequestObject.getApiResponse().getResult());
            Configuration.getErrorTracker().i(TAG, "api time: " + apiRequestObject.getApiResponse().getTime());
        } else {
            Configuration.getErrorTracker().i(TAG, "api request null: " + (apiRequestObject.getApiResponse() == null));
            getNewSessionToken(this);
        }
    }

    /*
        TokenFarmDistributor interface methods
    */
    @Override
    public void borrowSessionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "borrowSessionToken");
        com.arkhive.components.core.module_token_farm.tokens.SessionToken sessionToken = null;
        try {
            sessionToken = sessionTokens.take();
            Configuration.getErrorTracker().i(TAG, "session token borrowed: " + sessionToken.getTokenString());
        } catch (InterruptedException e) {
            e.printStackTrace();
            apiRequestObject.addExceptionDuringRequest(e);
            Configuration.getErrorTracker().i(TAG, "no session token borrowed, interrupted.");
        }
        apiRequestObject.setSessionToken(sessionToken);
    }

    @Override
    public void returnSessionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "returnSessionToken");
        com.arkhive.components.core.module_token_farm.tokens.SessionToken sessionToken = apiRequestObject.getSessionToken();
        boolean needToGetNewSessionToken = false;
        if (sessionToken == null) {
            Configuration.getErrorTracker().i(TAG, "request object did not have a session token, " +
                    "but it should have. need new session token");
            needToGetNewSessionToken = true;
        }

        if (sessionToken == null || apiRequestObject.isSessionTokenInvalid()) {
            Configuration.getErrorTracker().i(TAG, "not returning session token. it is invalid or signature " +
                    "calculation went bad. need new session token");
            needToGetNewSessionToken = true;
        } else {
            Configuration.getErrorTracker().i(TAG, "returning session token: " + sessionToken.getTokenString());
            try {
                sessionTokens.put(sessionToken);
            } catch (InterruptedException e) {
                Configuration.getErrorTracker().i(TAG, "could not return session token, interrupted. need new session token");
                needToGetNewSessionToken = true;
            }
        }

        if (needToGetNewSessionToken) {
            Configuration.getErrorTracker().i(TAG, "fetching a new session token");
            getNewSessionToken(this);
        }
    }

    @Override
    public void borrowImageActionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "borrowImageActionToken");
        // lock and fetch new token if necessary
        Configuration.getErrorTracker().i(TAG, "---starting lock: " + System.currentTimeMillis());
        lockBorrowImageToken.lock();
        if (imageActionToken == null || imageActionToken.isExpired()) {
            getNewImageActionToken(this, this);
        }
        try {
            // wait while we get an image action token, condition is that image
            // action token is null or action token is expired or action token
            // string is null
            while (imageActionToken == null ||
                    imageActionToken.isExpired() ||
                    imageActionToken.getTokenString() == null) {
                conditionImageTokenNotExpired.await(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
          apiRequestObject.addExceptionDuringRequest(e);
        } finally {
            // attach new one to apiRequestObject
            lockBorrowImageToken.unlock();
            Configuration.getErrorTracker().i(TAG, "---unlock lock: " + System.currentTimeMillis());
        }
        apiRequestObject.setActionToken(imageActionToken);
    }


    @Override
    public void borrowUploadActionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "borrowUploadActionToken");
        // lock and fetch new token if necessary
        Configuration.getErrorTracker().i(TAG, "---starting lock: " + System.currentTimeMillis());
        lockBorrowUploadToken.lock();
        if (uploadActionToken == null || uploadActionToken.isExpired()) {
            getNewUploadActionToken(this, this);
        }
        try {
            // wait while we get an image action token, condition is that image
            // action token is null or action token is expired or action token
            // string is null
            while (uploadActionToken == null ||
                    uploadActionToken.isExpired() ||
                    uploadActionToken.getTokenString() == null) {
                conditionUploadTokenNotExpired.await(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            apiRequestObject.addExceptionDuringRequest(e);
        } finally {
            lockBorrowUploadToken.unlock();
            Configuration.getErrorTracker().i(TAG, "---unlock lock: " + System.currentTimeMillis());
        }
        // attach new one to apiRequestObject
        apiRequestObject.setActionToken(uploadActionToken);
    }

    @Override
    public void receiveNewImageActionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "receiveNewImageActionToken()");
        synchronized (imageTokenLock) {
            if (imageActionToken != null &&
                    !imageActionToken.isExpired() &&
                    imageActionToken.getTokenString() != null) {
                Configuration.getErrorTracker().i(TAG, "received action token: " + imageActionToken.getTokenString() +
                        ", type: " + imageActionToken.getType().toString() +
                        ", expired: " + imageActionToken.isExpired());
                return;
            }
            ActionToken actionToken = apiRequestObject.getActionToken();

            if (actionToken == null) {
                Configuration.getErrorTracker().i(TAG, "action token received is null");
            } else if (actionToken.getTokenString() == null) {
                Configuration.getErrorTracker().i(TAG, "action token received is null");
            } else if (apiRequestObject.isActionTokenInvalid()) {
                Configuration.getErrorTracker().i(TAG, "action token received is invalid");
            } else if (actionToken.getType() != ActionToken.Type.IMAGE) {
                Configuration.getErrorTracker().i(TAG, "action token received is not image type");
            } else {
                imageActionToken = actionToken;
                Configuration.getErrorTracker().i(TAG, "received action token: " + imageActionToken.getTokenString() +
                        ", type: " + imageActionToken.getType().toString() +
                        ", expired: " + imageActionToken.isExpired());
            }
        }
    }

    @Override
    public void receiveNewUploadActionToken(ApiRequestObject apiRequestObject) {
        Configuration.getErrorTracker().i(TAG, "receiveNewUploadActionToken()");
        synchronized (uploadTokenLock) {
            if (uploadActionToken != null &&
                    !uploadActionToken.isExpired() &&
                    uploadActionToken.getTokenString() != null) {
                Configuration.getErrorTracker().i(TAG, "received action token: " + uploadActionToken.getTokenString() +
                        ", type: " + uploadActionToken.getType().toString() +
                        ", expired: " + uploadActionToken.isExpired());
                return;
            }
            ActionToken actionToken = apiRequestObject.getActionToken();
            if (actionToken == null) {
                Configuration.getErrorTracker().i(TAG, "action token received is null");
            } else if (actionToken.getTokenString() == null) {
                Configuration.getErrorTracker().i(TAG, "action token received is null");
            } else if (apiRequestObject.isActionTokenInvalid()) {
                Configuration.getErrorTracker().i(TAG, "action token received is invalid");
            } else if (actionToken.getType() != ActionToken.Type.UPLOAD) {
                Configuration.getErrorTracker().i(TAG, "action token received is not upload type");
            } else {
                uploadActionToken = actionToken;
                Configuration.getErrorTracker().i(TAG, "received action token: " + uploadActionToken.getTokenString() +
                        ", type: " + uploadActionToken.getType().toString() +
                        ", expired: " + uploadActionToken.isExpired());
            }
        }
    }
}
