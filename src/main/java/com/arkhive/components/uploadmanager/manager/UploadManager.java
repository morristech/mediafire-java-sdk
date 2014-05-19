package com.arkhive.components.uploadmanager.manager;

import com.arkhive.components.api.ApiResponse;
import com.arkhive.components.api.upload.errors.PollFileErrorCode;
import com.arkhive.components.api.upload.errors.PollResultCode;
import com.arkhive.components.api.upload.errors.PollStatusCode;
import com.arkhive.components.api.upload.listeners.UploadListenerManager;
import com.arkhive.components.api.upload.listeners.UploadListenerUI;
import com.arkhive.components.api.upload.process.CheckProcess;
import com.arkhive.components.api.upload.process.InstantProcess;
import com.arkhive.components.api.upload.process.PollProcess;
import com.arkhive.components.api.upload.process.ResumableProcess;
import com.arkhive.components.api.upload.responses.CheckResponse;
import com.arkhive.components.api.upload.responses.PollResponse;
import com.arkhive.components.sessionmanager.SessionManager;
import com.arkhive.components.uploadmanager.uploaditem.UploadItem;
import com.arkhive.components.uploadmanager.uploaditem.UploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UploadManager moves UploadItems from a Collection into Threads.
 * Number of threads that will be started is limited to the maximumThreadCount (default = 5)
 *
 * @author Chris Najar
 */
public class UploadManager implements UploadListenerManager {
    private static final String TAG = UploadManager.class.getSimpleName();
    private int maximumThreadCount = 5;
    private int currentThreadCount = 0;
    private boolean paused;

    private final CopyOnWriteArrayList<UploadItem> backlog = new CopyOnWriteArrayList<UploadItem>();
    private final CopyOnWriteArrayList<UploadItem> pool    = new CopyOnWriteArrayList<UploadItem>();
    private final SessionManager sessionManager;
    private final Logger logger = LoggerFactory.getLogger(UploadManager.class);

    /**
     * Constructor that takes a SessionManager, HttpInterface, and a maximum thread count.
     * <p/>
     * Use this method when you do not want to explicitly set the starting thread count.
     *
     * @param sessionManager  The SessionManager to use for API operations.
     * @param maximumThreadCount  The maximum number of threads to use for uploading.
     */
    public UploadManager(SessionManager sessionManager, int maximumThreadCount) {
        super();
        this.sessionManager = sessionManager;
        this.paused = true;
        this.setMaximumThreadCount(maximumThreadCount);
    }

    /**
     * Constructor that takes a SessionManager, HttpInterface, and a maximum thread count.
     * Use this method when you do not want to explicitly set the starting thread count.
     *
     * @param sessionManager  The SessionManager to use for API operations.
     */
    public UploadManager(SessionManager sessionManager) {
        this(sessionManager, 5);
    }

    /*============================
     * public getters
     *============================*/

    /**
     * Returns the backlog size.
     *
     * @return number of items in the backlog queue.
     */
    public synchronized int getBacklogSize() {
        return backlog.size();
    }

    /**
     * Returns the current thread count.
     *
     * @return number of items in thread pool.
     */
    public synchronized int getCurrentThreadCount() {
        return currentThreadCount;
    }

    public List<UploadItem> getAllItems() {
        logger.info(TAG + " getAllItems() called");
        List<UploadItem> items = new CopyOnWriteArrayList<UploadItem>();
        logger.info(TAG + "    items size: " + items.size());
        synchronized (backlog) { // lock backlog
            synchronized (pool) { // lock pool
                items.addAll(backlog);
                items.addAll(pool);
            }
        }
        return items;
    }

    /*============================
     * public setters
     *============================*/

    /**
     * Sets the maximum thread count for the UploadManager.
     *
     * @param maximumThreadCount  The maximum number of threads to use for uploading.
     */
    public void setMaximumThreadCount(int maximumThreadCount) {
        this.maximumThreadCount = maximumThreadCount;
        moveBacklogToThread();
    }

    /*============================
     * public methods
     *============================*/

    /**
     * adds an UploadItem to the backlog queue.
     * If the UploadItem already exists in the backlog queue then we do not add the item.
     *
     * @param uploadItem The UploadItem to add to the backlog queue.
     */
    public void addUploadRequest(UploadItem uploadItem) {
        logger.info(TAG + " addUploadRequest() called");
        //don't add the item to the backlog queue if it is null or the path is null
        if (uploadItem == null || uploadItem.getPath() == null) {
            logger.info(TAG, "  UploadItem is null");
            return;
        }

        //don't add the item to the backlog queue if max attempts has been exceeded
        if (uploadItem.exceedsMaximumUploadAttempts()) {
            logger.info(TAG, "  UploadItem exceeded it's maximum upload attempts");
            return;
        }

        synchronized (backlog) {
            //don't add the item to the backlog queue if it is already in the backlog queue
            for (UploadItem item : backlog) {
                if (item.equalTo(uploadItem)) {
                    logger.info(TAG, "  UploadItem is already in queue");
                    return;
                }
            }
        }

        //set the upload manager for the item to this class
        uploadItem.setUploadManagerListener(this);

        //adding to the backlog queue means we eventually attempt to upload this item, so increase the upload attempts
        uploadItem.increaseCurrentUploadAttempt();
        synchronized (backlog) {
            backlog.add(uploadItem);
        }
        moveBacklogToThread();
    }

    public void clearUploadQueue() {
        logger.info(TAG, " clearUploadQueue() called");
        boolean startedPaused = isPaused();
        if (!startedPaused) {
            pause();
        }

        for (UploadItem item : backlog) {
            item.setStatus(UploadStatus.CANCELLED);
            removeUploadRequest(item);
        }

        for (UploadItem item : pool) {
            item.setStatus(UploadStatus.CANCELLED);
            removeUploadFromPool(item);
        }

        if(!startedPaused) {
            resume();
        }
    }

    /**
     * Pause moving backlog items to the thread queue.
     * <p/>
     * This method sets the paused flag to true.
     */
    public void pause() {
        logger.info(TAG + " pause() called");
        this.paused = true;
    }

    /**
     * Resume moving backlog items to the thread queue.
     * <p/>
     * This method sets the paused flag to false and then calls moveBacklogToThread().
     */
    public void resume() {
        logger.info(TAG + " resume() called");
        this.paused = false;
        moveBacklogToThread();
    }

    /**
     * Returns whether or not UploadManager is paused or not.
     *
     * @return true if paused (not moving backlog to queue), false otherwise.
     */
    public boolean isPaused() {
        logger.info(TAG + " isPaused() called");
        return paused;
    }

    /*============================
     * private methods
     *============================*/

    /**
     * decreased the current Thread count.
     * this method should only be called by the Listener
     */
    private synchronized void decreaseCurrentThreadCount(UploadItem uploadItem) {
        logger.info(TAG + " decreaseCurrentThreadCount() called");
        if (getCurrentThreadCount() > 0) {
            currentThreadCount--;
        }

        removeUploadFromPool(uploadItem);

        moveBacklogToThread();
    }

    /**
     * removes an UploadRequest from the pool queue, if it exists. This method will NOT remove items from the thread
     * pool if the status is CANCELLED.
     *
     * @param uploadItem  The UploadItem to remove from the pool.
     */
    private synchronized void removeUploadFromPool(UploadItem uploadItem) {
        logger.info(TAG + " removeUploadFromPool() called");
        if (uploadItem == null) { return; }
        for (UploadItem item : pool) {
            if (item.equalTo(uploadItem)) { // remove item if it is found
                logger.info(TAG + " backlog removing path: " + uploadItem.getPath());
                boolean removed = pool.remove(uploadItem);
                logger.info(TAG + "  remove success: " + removed);
                if (removed) {
                    break;
                }
            }
        }
    }

    /**
     * removes an UploadRequest from the backlog queue, if it exists.
     * This method will also remove any UploadItem which has a UploadStatus of CANCELLED.
     *
     * @param uploadItem  The UploadItem to remove from the queue.
     */
    private synchronized void removeUploadRequest(UploadItem uploadItem) {
        logger.info(TAG + " removeUploadRequest() called");
        if (uploadItem == null) { return; }
        for (UploadItem item : backlog) {
            if (item.equalTo(uploadItem)) { // remove item if it is found
                logger.info(TAG + " backlog removing path: " + uploadItem.getPath());
                boolean removed = backlog.remove(uploadItem);
                logger.info(TAG + "  remove success: " + removed);
                if (removed) {
                    break;
                }
            } else if (item.getStatus() == UploadStatus.CANCELLED) { // remove item anyway if status is CANCELLED
                logger.info(TAG + " backlog removing path: " + uploadItem.getPath());
                boolean removed = backlog.remove(uploadItem);
                logger.info(TAG + "  remove success: " + removed);
            }
        }
    }

    /**
     * Increases the current thread count by 1, and starts the thread passed as a parameter.
     *
     * @param thread  The Thread to process.
     */
    private synchronized void increaseCurrentThreadCount(Thread thread) {
        logger.info(TAG + " increaseCurrentThreadCount() called");
        if (thread != null) {
            currentThreadCount++;
            thread.start();
        }
    }

    /**
     * attempts to move all backlog UploadItems into active threads up to the maximumThreadCount we iterate over the
     * items in the UploadItem collection and then for each item we check whether or not we have exceeded the
     * maximumThreadCount. Items that do not have an UploadStatus of READY will not be added to the thread pool because
     * they are either in status CANCELLED or PAUSED.
     * If we have not, we start upload.
     * If we have, then we return out of the method
     */
    private synchronized void moveBacklogToThread() {
        logger.info(TAG + " moveBacklogToThread() called");
        if (isPaused()) {
            return;
        }

        for (UploadItem item : backlog) {
            switch (item.getStatus()) {
                case CANCELLED: // cancelled, don't add it to thread queue and also drop it from the backlog queue.
                    removeUploadRequest(item);
                    break;
                case PAUSED: // paused, don't add it to the thread queue
                    break;
                case READY: // ready to continue, so try to add it to the thread pool
                    if (getCurrentThreadCount() < maximumThreadCount) {
                        CheckProcess runnable = new CheckProcess(sessionManager, item);
                        Thread thread = new Thread(runnable);
                        // also let's add the item to the thread pool collection.
                        synchronized (pool) {
                            pool.add(item);
                        }

                        removeUploadRequest(item);
                        increaseCurrentThreadCount(thread);
                    } else {
                        return;
                    }
                    break;
                default: // this should never happen
                    break;
            }
        }
    }

    /*============================
     * interface methods
     *============================*/

    @Override
    public void onCheckCompleted(UploadItem uploadItem, CheckResponse response) {
        logger.info(TAG + " onCheckCompleted() called");
        //check the item status first to see if the item status was changed.
        switch (uploadItem.getStatus()) {
            case CANCELLED: // cancelled, don't add it to thread queue and also drop it from the backlog queue.
                removeUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case PAUSED: // paused, add the item to the backlog queue.
                addUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case READY: // ready to continue, continue code execution
                break;
            default: // this should never happen.
                break;
        }

        if (!response.getStorageLimitExceeded()) { //storage limit not exceeded
            System.out.println("--storage limit not exceeded");
            if (response.getHashExists()) { //hash does exist for the file
                System.out.println("--hash exists");
                if (!response.getInAccount()) { // hash which exists is not in the account
                    System.out.println("--hash not in account");
                    InstantProcess process = new InstantProcess(sessionManager, uploadItem);
                    Thread thread = new Thread(process);
                    thread.start();
                } else { // hash exists and is in the account
                    System.out.println("--hash in account");
                    boolean inFolder = response.getInFolder();
                    InstantProcess process = new InstantProcess(sessionManager, uploadItem);
                    Thread thread = new Thread(process);
                    System.out.println("***ACTIONONINACCOUNT: " + uploadItem.getUploadOptions().getActionOnInAccount());
                    switch (uploadItem.getUploadOptions().getActionOnInAccount()) {
                        case UPLOAD_ALWAYS:
                            System.out.println("***ACTION IN ACCOUNT VIA SWITCH STMT case UPLOAD_ALWAYS");
                            thread.start();
                            break;
                        case UPLOAD_IF_NOT_IN_FOLDER:
                            System.out.println("***ACTION IN ACCOUNT VIA SWITCH STMT case UPLOAD_ALWAYS");
                            if (!inFolder) {
                                System.out.println("***NOT IN FOLDER SO UPLOADING");
                                thread.start();
                            } else {
                                System.out.println("***NOT IN FOLDER SO NOT UPLOADING");
                            }
                            break;
                        case DO_NOT_UPLOAD:
                        default:
                            System.out.println("***ACTION IN ACCOUNT VIA SWITCH STMT case do_not_upload/default");
                            removeUploadRequest(uploadItem);
                            decreaseCurrentThreadCount(uploadItem);
                            for (UploadListenerUI listener : uploadItem.getUiListeners()) {
                                listener.onCompleted(uploadItem);
                            }
                            if (uploadItem.getDatabaseListener() != null) {
                                uploadItem.getDatabaseListener().onCompleted(uploadItem);
                            }
                            break;
                    }
                }
            } else { // hash does not exist. call resumable.
                System.out.println("--hash does not exist");
                if (response.getResumableUpload().getAllUnitsReady() && !uploadItem.getPollUploadKey().isEmpty()) {
                    System.out.println("--all units ready and have a poll upload key");
                    // all units are ready and we have the poll upload key. start polling.
                    uploadItem.getChunkData().setNumberOfUnits(response.getResumableUpload().getNumberOfUnits());
                    uploadItem.getChunkData().setUnitSize(response.getResumableUpload().getUnitSize());
                    PollProcess process = new PollProcess(sessionManager, uploadItem);
                    Thread thread = new Thread(process);
                    thread.start();
                } else {
                    System.out.println("--all units not ready or do not have poll upload key");
                    // either we don't have the poll upload key or all units are not ready
                    uploadItem.getChunkData().setNumberOfUnits(response.getResumableUpload().getNumberOfUnits());
                    uploadItem.getChunkData().setUnitSize(response.getResumableUpload().getUnitSize());
                    ResumableProcess process = new ResumableProcess(sessionManager, uploadItem);
                    Thread thread = new Thread(process);
                    thread.start();
                }
            }
        } else { //user exceeded storage space.
            System.out.println("--storage limit is exceeded");
            removeUploadRequest(uploadItem);
            decreaseCurrentThreadCount(uploadItem);
        }
    }

    @Override
    public void onInstantCompleted(UploadItem uploadItem) {
        logger.info(TAG + " onInstantCompleted() called");
        // if everything is ok with the response we want to decrease the current thread count
        decreaseCurrentThreadCount(uploadItem);
    }

    @Override
    public void onResumableCompleted(UploadItem uploadItem) {
        logger.info(TAG + " onResumableCompleted() called");
        //check the item status first to see if the item status was changed.
        switch (uploadItem.getStatus()) {
            case CANCELLED: // cancelled, don't add it to thread queue and also drop it from the backlog queue.
                removeUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case PAUSED: // paused, add the item to the backlog queue.
                addUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case READY: // ready to continue, continue code execution
                break;
            default: // this should never happen.
                break;
        }

        // if the item status isn't cancelled or paused then call upload/check to make sure all units are ready
        CheckProcess process = new CheckProcess(sessionManager, uploadItem);
        Thread thread = new Thread(process);
        thread.start();
    }

    @Override
    public void onPollCompleted(UploadItem uploadItem, PollResponse response) {
        logger.info(TAG + " onPollCompleted() called");
        //check the item status first to see if the item status was changed.
        switch (uploadItem.getStatus()) {
            case CANCELLED: // cancelled, don't add it to thread queue and also drop it from the backlog queue.
                removeUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case PAUSED: // paused, continue code execution
                break;
            case READY: // ready to continue, continue code execution
                break;
            default: // this should never happen.
                break;
        }

        // if this method is called then filerror and result codes are fine,
        // but we may not have received status 99 so check status code and
        // then possibly senditem to the backlog queue.
        if (response.getDoUpload().getStatusCode() != PollStatusCode.NO_MORE_REQUESTS_FOR_THIS_KEY) {
            addUploadRequest(uploadItem);
        }

        decreaseCurrentThreadCount(uploadItem);
    }

    @Override
    public void onProcessException(UploadItem uploadItem, Exception exception) {
        logger.info(TAG + " onProcessException() called");
        //decrease the thread count
        logger.error(TAG, "received exception: " + exception);
        decreaseCurrentThreadCount(uploadItem);
    }

    @Override
    public void onLostConnection(UploadItem uploadItem) {
        logger.info(TAG + " onLostConnection() called");
        //check the item status first to see if the item status was changed.
        switch (uploadItem.getStatus()) {
            case CANCELLED: // cancelled, don't add it to thread queue and also drop it from the backlog queue.
                removeUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case PAUSED: // paused, continue code execution
                break;
            case READY: // ready to continue, continue code execution
                break;
            default: // this should never happen.
                break;
        }

        //pause upload manager
        //pause(); STARS TASK#25330 - upload manager won't be pausing anymore. up to end user.

        //add item to backlog
        addUploadRequest(uploadItem);

        //decrease current thread count
        decreaseCurrentThreadCount(uploadItem);
    }

    @Override
    public void onCancelled(UploadItem uploadItem, ApiResponse response) {
        //check the item status first to see if the item status was changed.
        switch (uploadItem.getStatus()) {
            case CANCELLED: // cancelled, don't add it to thread queue and also drop it from the backlog queue.
                removeUploadRequest(uploadItem);
                decreaseCurrentThreadCount(uploadItem);
                return;
            case PAUSED: // paused, continue code execution
                break;
            case READY: // ready to continue, continue code execution
                break;
            default: // this should never happen.
                break;
        }

        // if there is an api error then add this item to the backlog queue and decrease current thread count
        if (response.hasError()) {
            addUploadRequest(uploadItem);
            decreaseCurrentThreadCount(uploadItem);
            return;
        }

        // if the response does not have an api error, then onCancelled was called by PollProcess or ResumableProcess
        if (response instanceof PollResponse) {
            PollResponse castResponse = (PollResponse) response;
            // if poll upload called onCancelled() because it ran past its attempts, add this to the backlog queue
            // if poll upload called onCancelled() because of a file error code or a result error code then drop it from
            // the queue (via not adding it back to the queue)
            if (castResponse.getDoUpload().getFileErrorCode() == PollFileErrorCode.NO_ERROR &&
                    castResponse.getDoUpload().getResultCode() == PollResultCode.SUCCESS) {
                addUploadRequest(uploadItem);
            }
        }

        decreaseCurrentThreadCount(uploadItem);
    }
}
