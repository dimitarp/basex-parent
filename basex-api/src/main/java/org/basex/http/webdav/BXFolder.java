package org.basex.http.webdav;

import static org.basex.http.webdav.impl.Utils.*;

import java.io.*;
import java.util.*;

import org.basex.util.*;
import org.basex.http.webdav.impl.ResourceMetaData;
import org.basex.http.webdav.impl.WebDAVService;
import org.basex.io.in.*;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.*;

/**
 * WebDAV resource representing a folder within a collection database.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Rositsa Shadura
 * @author Dimitar Popov
 */
public class BXFolder extends BXAbstractResource implements FolderResource,
    DeletableCollectionResource, LockingCollectionResource {
  /**
   * Constructor.
   * @param d resource meta data
   * @param s service implementation
   */
  public BXFolder(final ResourceMetaData d, final WebDAVService<BXAbstractResource> s) {
    super(d, s);
  }

  @Override
  public Long getContentLength() {
    return null;
  }

  @Override
  public String getContentType(final String accepts) {
    return null;
  }

  @Override
  public Date getCreateDate() {
    return null;
  }

  @Override
  public Long getMaxAgeSeconds(final Auth auth) {
    return null;
  }

  @Override
  public void sendContent(final OutputStream out, final Range range,
      final Map<String, String> params, final String contentType) {
  }

  @Override
  public boolean isLockedOutRecursive(final Request request) {
    return new BXCode<Boolean>(this) {
      @Override
      public Boolean get() throws IOException {
        return service.locking.conflictingLocks(meta.db, meta.path);
      }
    }.evalNoEx();
  }

  @Override
  public BXFolder createCollection(final String folder) throws BadRequestException {
    return new BXCode<BXFolder>(this) {
      @Override
      public BXFolder get() throws IOException {
        return (BXFolder) service.createFolder(meta.db, meta.path,  folder);
      }
    }.eval();
  }

  @Override
  public BXAbstractResource child(final String childName) {
    return new BXCode<BXAbstractResource>(this) {
      @Override
      public BXAbstractResource get() throws IOException {
        return service.resource(meta.db, meta.path + SEP + childName);
      }
    }.evalNoEx();
  }

  @Override
  public List<BXAbstractResource> getChildren() {
    return new BXCode<List<BXAbstractResource>>(this) {
      @Override
      public List<BXAbstractResource> get() throws IOException {
        return service.list(meta.db, meta.path);
      }
    }.evalNoEx();
  }

  @Override
  public BXAbstractResource createNew(final String newName, final InputStream input,
      final Long length, final String contentType) throws BadRequestException {
    return new BXCode<BXAbstractResource>(this) {
      @Override
      public BXAbstractResource get() throws IOException {
        return service.createFile(meta.db, meta.path, newName, input);
      }
    }.eval();
  }

  @Override
  public LockToken createAndLock(final String name, final LockTimeout timeout,
      final LockInfo lockInfo) throws NotAuthorizedException {
    try {
      final BXAbstractResource r = createNew(name, new ArrayInput(Token.EMPTY), 0L, null);
      final LockResult lockResult = r.lock(timeout, lockInfo);
      if(lockResult.isSuccessful()) return lockResult.getLockToken();
    } catch(final Exception ex) {
      Util.debug("Cannot lock and create requested resource", ex);
    }
    return null;
  }

  @Override
  protected void copyToRoot(final String name) throws IOException {
    // folder is copied to the root: create new database with it
    final String dbname = dbname(name);
    service.createDb(dbname);
    service.copyAll(meta.db, meta.path, dbname, "");
  }

  @Override
  protected void copyTo(final BXFolder folder, final String name) throws IOException {
    // folder is copied to a folder in a database
    service.copyAll(meta.db, meta.path, folder.meta.db, folder.meta.path + SEP + name);
    service.deleteDummy(folder.meta.db, folder.meta.path);
  }
}
