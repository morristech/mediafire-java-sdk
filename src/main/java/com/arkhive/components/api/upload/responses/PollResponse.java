package com.arkhive.components.api.upload.responses;

import com.arkhive.components.api.ApiResponse;
import com.arkhive.components.api.upload.errors.PollFileErrorCode;
import com.arkhive.components.api.upload.errors.PollResultCode;
import com.arkhive.components.api.upload.errors.PollStatusCode;

/**
 * This class represents the data structure received (response)
 * by a call to /api/upload/poll_upload.php.
 * @author Chris Najar
 *
 */
public class PollResponse extends ApiResponse {
  private DoUpload doupload;

  /**This represents the doupload portion of the poll upload response data structure.
   * @author Chris Najar
   */
  public class DoUpload {
    private String result;
    private String status;
    private String description;
    private String fileerror;
    private String quickkey;
    private String size;
    private String revision;
    private String created;
    private String filename;
    private String hash;

    public PollResultCode getResultCode() {
      if (result == null || result.equals("")) { return PollResultCode.fromInt(0); }
      return PollResultCode.fromInt(Integer.parseInt(result));
    }
    public PollStatusCode getStatusCode() {
      if (status == null || status.equals("")) { return PollStatusCode.fromInt(0); }
      return PollStatusCode.fromInt(Integer.parseInt(status));
    }
    public String getDescription() {
      if (description == null)  { return ""; }
      return description;
    }
    public PollFileErrorCode getFileErrorCode() {
      if (fileerror == null || fileerror.equals(""))  { return PollFileErrorCode.fromInt(0); }
      return PollFileErrorCode.fromInt(Integer.parseInt(fileerror));
    }
    public String getQuickKey() {
      if (quickkey == null) { return ""; }
      return quickkey;
    }
    public long getSize() {
      if (size == null || size.equals("")) { return 0; }
      return Long.parseLong(size);
    }
    public String getRevision() {
      if (revision == null) { return ""; }
      return revision;
    }
    public String getCreated() {
      if (created == null) { return ""; }
      return created;
    }
    public String getFilename() {
      if (filename == null) { return ""; }
      return filename;
    }
    public String getHash() {
      if (hash == null) { return ""; }
      return hash;
    }
  }

  public DoUpload getDoUpload() {
    if (doupload == null) { return new DoUpload(); }
    return doupload;
  }
}