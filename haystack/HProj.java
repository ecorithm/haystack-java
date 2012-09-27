//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   11 Jul 2011  Brian Frank  Creation
//   26 Sep 2012  Brian Frank  Revamp original code
//
package haystack;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import haystack.*;
import haystack.io.*;

/**
 * HProj is the common interface for HClient and HServer to provide
 * access to a database tagged entity records.
 */
public abstract class HProj
{

//////////////////////////////////////////////////////////////////////////
// Operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the summary "about" information.
   */
  public abstract HDict about();

//////////////////////////////////////////////////////////////////////////
// Read by id
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for "readById(id, true)"
   */
  public final HDict readById(HRef id)
  {
    return readById(id, true);
  }

  /**
   * Call "read" to lookup an entity record by it's unique identifier.
   * If not found then return null or throw an UnknownRecException based
   * on checked.
   */
  public final HDict readById(HRef id, boolean checked)
  {
    HDict rec = onReadById(id);
    if (rec != null) return rec;
    if (checked) throw new UnknownRecException(id);
    return null;
  }

  /**
   * Convenience for "readByIds(ids, true)"
   */
  public final HGrid readByIds(HRef[] ids)
  {
    return readByIds(ids, true);
  }

  /**
   * Read a list of entity records by their unique identifier.
   * Return a grid where each row of the grid maps to the respective
   * id array (indexes line up).  If checked is true and any one of the
   * ids cannot be resolved then raise UnknownRecException for first id
   * not resolved.  If checked is false, then each id not found has a
   * row where every cell is null.
   */
  public final HGrid readByIds(HRef[] ids, boolean checked)
  {
    HGrid grid = onReadByIds(ids);
    if (checked)
    {
      for (int i=0; i<grid.numRows(); ++i)
        if (grid.row(i).missing("id")) throw new UnknownRecException(ids[i]);
    }
    return grid;
  }

  /**
   * Subclass hook for readById, return null if not found.
   */
  protected abstract HDict onReadById(HRef id);

  /**
   * Subclass hook for readByIds, return rows with nulls cells
   * for each id not found.
   */
  protected abstract HGrid onReadByIds(HRef[] ids);

//////////////////////////////////////////////////////////////////////////
// Read by filter
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for "read(filter, true)".
   */
  public final HDict read(String filter)
  {
    return read(filter, true);
  }

  /**
   * Query one entity record that matches the given filter.  If
   * there is more than one record, then it is undefined which one is
   * returned.  If there are no matches than return null or raise
   * UnknownRecException based on checked flag.
   */
  public final HDict read(String filter, boolean checked)
  {
    HGrid grid = readAll(filter, 1);
    if (grid.numRows() > 0) return grid.row(0);
    if (checked) throw new UnknownRecException(filter);
    return null;
  }

  /**
   * Convenience for "readAll(filter, max)".
   */
  public final HGrid readAll(String filter)
  {
    return readAll(filter, Integer.MAX_VALUE);
  }

  /**
   * Call "read" to query every entity record that matches given filter.
   * Clip number of results by "limit" parameter.
   */
  public final HGrid readAll(String filter, int limit)
  {
    return onReadAll(filter, limit);
  }

  /**
   * Subclass hook for read and readAll.
   */
  protected abstract HGrid onReadAll(String filter, int limit);

}