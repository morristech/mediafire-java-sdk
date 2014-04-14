package com.arkhive.components.uploadmanager.uploaditem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.arkhive.components.api.upload.listeners.UploadListenerDatabase;
import com.arkhive.components.api.upload.listeners.UploadListenerManager;
import com.arkhive.components.api.upload.listeners.UploadListenerUI;

/**
 * This data structure represents an item to be uploaded.
 * The only mandatory parameter that needs to be passed to
 * the constructor is a path, but the implementer of this
 * data structure can also pass specific upload options.
 * @author Chris Najar
 *
 */
public class UploadItem {
    private String path;
    private String shortFileName;
    private String quickkey;
    private String modificationTime;
    private UploadOptions options;
    private FileData fileData;
    private ChunkData chunkData;
    private ResumableBitmap bitmap;
    private String pollUploadKey;
    private String imageId;
    private UploadListenerDatabase dbListener;
    private List<UploadListenerUI> uiListeners;
    private UploadListenerManager mgrListener;    
    private UploadStatus status;
    
    /**
     * Constructor which takes a path and upload attempts.
     * Use this method when you want to customize the upload options for this UploadItem.
     * @param path - file path on the device
     * Should use the single or dual argument constructor for the most part.
     */
    public UploadItem(String path, String imageId, UploadOptions uploadData) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (uploadData == null) {
            options = new UploadOptions();
        } else {
            this.options = uploadData;
        }
        this.path = path;
        this.imageId = imageId;
        setShortFileName(path);
        
        //instantiate ui listener collection
        uiListeners = new ArrayList<UploadListenerUI>();
        
        //set Object fields so they won't be null
        fileData = new FileData(path);
        this.quickkey = "";
        this.modificationTime = "";
        this.pollUploadKey = "";
        this.chunkData = new ChunkData(0, 0);
        this.bitmap = new ResumableBitmap(0, new ArrayList<Integer>());
        this.status = UploadStatus.READY;
    }

    /**
     * Constructor which takes a path and an image id
     * Use this method when you want to use default upload options for this UploadItem.
     * @param path - path to data
     * @param id - unique image id
     */
    public UploadItem(String path, String id) {
        this(path, id, null);
    }

    /*============================
     * public getters
     *============================*/
    /**
     * returns the listener that is set as the database listener.
     * @return
     */
    public UploadListenerDatabase getDatabaseListener() { return dbListener; }
    
    /**
     * returns the listener that is set as the UI listener.
     * @return
     */
    public List<UploadListenerUI> getUiListeners() { return uiListeners; }
    
    /**
     * returns the listener that is set as the upload manager listener.
     * @return
     */
    public UploadListenerManager getUploadManagerListener() { return mgrListener; }
    
    /**
     * Called to get the image id (MediaStore column _ID).
     * @return
     */
    public String getImageId() { return imageId; }
    /**
     * Called to get the quick key.
     * @return
     */
    public String getQuickKey() { return quickkey; }

    /**
     * Called to get the Short file name.
     * @return
     */
    public String getShortFileName() { return shortFileName; }

    /**
     * CAlled to get the UploadItemFileData.
     * @return
     */
    public FileData getFileData() { return fileData; }

    /**
     * Called to get the path.
     * @return
     */
    public String getPath() { return path; }

    /**
     * Called to get the poll upload key.
     * @return
     */
    public String getPollUploadKey() { return pollUploadKey; }

    /**
     * Called to get the UploadItemFileUploadOptions.
     * @return
     */
    public UploadOptions getUploadOptions() { return options; }

    /**
     * Called to get the UploadItemChunkData.
     * @return
     */
    public ChunkData getChunkData() { return chunkData; }

    /**
     * Called to get the ResumableUploadBitmap.
     * @return
     */
    public ResumableBitmap getBitmap() { return bitmap; }

    /**
     * Called to get the Modification Time.
     * @return
     */
    public String getModificationTime() { return modificationTime; }

    /*============================
     * public setters
     *============================*/
    
    /**
     * sets the content provider listener.
     * @param dbListener
     */
    public void setDatabaseListener(UploadListenerDatabase dbListener) { this.dbListener = dbListener; }
    
    /**
     * adds a UI listener.
     * @param uiListener
     */
    public void addUiListener(UploadListenerUI uiListener) { uiListeners.add(uiListener); }
    
    /**
     * removes a ui listener.
     * @param uiListener
     * @return - false if there was no listener to remove, true otherwise.
     */
    public boolean removeUiListener(UploadListenerUI uiListener) {
      return uiListeners.remove(uiListener);
    }
    
    /**
     * sets the upload manager listener. 
     * @param mgrListener
     */
    public void setUploadManagerListener(UploadListenerManager mgrListener) { this.mgrListener = mgrListener; }
    
    /**
     * Sets the quick key.
     * @param quickKey
     */
    public void setQuickKey(String quickKey) { this.quickkey = quickKey; }

    /**
     * Sets the ResumableUploadBitmap.
     * @param bitmap
     */
    public void setBitmap(ResumableBitmap bitmap) { this.bitmap = bitmap; }

    /**
     * Sets the poll upload key.
     * @param pollUploadKey
     */
    public void setPollUploadKey(String pollUploadKey) { this.pollUploadKey = pollUploadKey; }

    /**
     * Sets the modification time. A valid format must be entered.
     * @param modificationTime
     */
    public void setModificationTime(String modificationTime) {
        String timeString = null;
        if (modificationTime == null || modificationTime.isEmpty()) {
            timeString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(new Date());
        } else { timeString = modificationTime; }

        try {
            this.modificationTime = URLEncoder.encode(timeString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalStateException("UTF-8 encoding not available");
        }
    }

    /*============================
     * private methods
     *============================*/
    /**
     * sets the short file name given the path.
     * @param path
     */
    private void setShortFileName(String path) {
        String[] splitName = path.split("/");
        //just throwing the unsupportedcoding exception to whoever creates the upload item
        try {
          this.shortFileName = URLEncoder.encode(splitName[splitName.length - 1], "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new IllegalStateException("UTF-8 not supported");
        }
    }

    /*============================
     * public methods
     *============================*/
    /**
     * determines if this UploadItem has exceeded the maximum attempts as specified by UploadItem creator.
     * @return true if exceeded, false if not.
     */
    public boolean exceedsMaximumUploadAttempts() {
      return options.getCurrentUploadAttempts() > options.getMaximumUploadAttempts();
    }

    /**
     * Increases the number of upload attempts for this UploadItem by 1.
     */
    public void increaseCurrentUploadAttempt() { options.uploadAttempt(); }

    /**
     * Determines whether an Upload Item has the same hash as another Upload Item.
     * @param item
     * @return true if hashes match, false otherwise.
     */
    public boolean equalTo(UploadItem item) { return fileData.getFileHash().equals(item.fileData.getFileHash()); }
    
    /**
     * Gives the status (paused, ready, cancelled) of this item.
     * @return the UploadStatus for this item.
     */
    public UploadStatus getStatus() {
      synchronized (this) {
        return this.status;
      }
    }
    
    /**
     * Sets the status (paused, ready, cancelled) of this item.
     * @param status - status to set this item to.
     */
    public void setStatus(UploadStatus status) {
      synchronized (this) {
        this.status = status;
      }
    }

    /*============================
     * protected methods
     *============================*/
}