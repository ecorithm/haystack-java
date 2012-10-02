//
// Copyright (c) 2012, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Sep 2012  Brian Frank  Creation
//
package haystack.server;

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import haystack.*;
import haystack.io.*;

/**
 * HStdOps defines the standard operations available.
 *
 * @see <a href='http://project-haystack.org/doc/Ops'>Project Haystack</a>
 */
public class HStdOps
{
  /** List the registered operations. */
  public static final HOp about = new AboutOp();

  /** List the registered operations. */
  public static final HOp ops = new OpsOp();

  /** List the registered grid formats. */
  public static final HOp formats = new FormatsOp();

  /** Read entity records in database. */
  public static final HOp read = new ReadOp();

  /** Watch subscription. */
  public static final HOp watchSub = new WatchSubOp();

  /** Watch unsubscription. */
  public static final HOp watchUnsub = new WatchUnsubOp();

  /** Watch poll cov or refresh. */
  public static final HOp watchPoll = new WatchPollOp();

  /** Read time series history data. */
  public static final HOp hisRead = new HisReadOp();

  /** Write time series history data. */
  public static final HOp hisWrite = new HisWriteOp();
}

//////////////////////////////////////////////////////////////////////////
// AboutOp
//////////////////////////////////////////////////////////////////////////

class AboutOp extends HOp
{
  public String name() { return "about"; }
  public String summary() { return "Summary information for server"; }
  public HGrid onService(HServer db, HGrid req)
  {
    return HGridBuilder.dictToGrid(db.about());
  }
}

//////////////////////////////////////////////////////////////////////////
// OpsOp
//////////////////////////////////////////////////////////////////////////

class OpsOp extends HOp
{
  public String name() { return "ops"; }
  public String summary() { return "Operations supported by this server"; }
  public HGrid onService(HServer db, HGrid req)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("name");
    b.addCol("summary");
    HOp[] ops = db.ops();
    for (int i=0; i<ops.length; ++i)
    {
      HOp op = ops[i];
      b.addRow(new HVal[] {
        HStr.make(op.name()),
        HStr.make(op.summary()),
      });
    }
    return b.toGrid();
  }
}

//////////////////////////////////////////////////////////////////////////
// FormatsOp
//////////////////////////////////////////////////////////////////////////

class FormatsOp extends HOp
{
  public String name() { return "formats"; }
  public String summary() { return "Grid data formats supported by this server"; }
  public HGrid onService(HServer db, HGrid req)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("mime");
    b.addCol("read");
    b.addCol("write");
    HGridFormat[] formats = HGridFormat.list();
    for (int i=0; i<formats.length; ++i)
    {
      HGridFormat format = formats[i];
      b.addRow(new HVal[] {
        HStr.make(format.mime),
        format.reader != null ? HMarker.VAL : null,
        format.writer != null ? HMarker.VAL : null,
      });
    }
    return b.toGrid();
  }
}

//////////////////////////////////////////////////////////////////////////
// ReadOp
//////////////////////////////////////////////////////////////////////////

class ReadOp extends HOp
{
  public String name() { return "read"; }
  public String summary() { return "Read entity records in database"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    // ensure we have one row
    if (req.isEmpty()) throw new Exception("Request has no rows");

    // perform filter or id read
    HRow row = req.row(0);
    HDict[] recs;
    if (row.has("filter"))
    {
      // filter read
      String filter = row.getStr("filter");
      int limit = row.has("limit") ? row.getInt("limit") : Integer.MAX_VALUE;
      return db.readAll(filter, limit);
    }
    else if (row.has("id"))
    {
      // read by ids
      HRef[] ids = gridToIds(req);
      return db.readByIds(ids, false);
    }
    else
    {
      throw new Exception("Missing filter or id columns");
    }
  }
}

//////////////////////////////////////////////////////////////////////////
// WatchSubOp
//////////////////////////////////////////////////////////////////////////

class WatchSubOp extends HOp
{
  public String name() { return "watchSub"; }
  public String summary() { return "Watch subscription"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    // check for watchId or watchId
    String watchId = null;
    String watchDis = null;
    if (req.meta().has("watchId"))
      watchId = req.meta().getStr("watchId");
    else
      watchDis = req.meta().getStr("watchDis");

    // open or lookup watch
    HWatch watch = watchId == null ?
                   db.watchOpen(watchDis) :
                   db.watch(watchId);

    // map grid to ids
    HRef[] ids = gridToIds(req);

    // subscribe and return resulting grid
    return watch.sub(ids);
  }
}

//////////////////////////////////////////////////////////////////////////
// WatchUnsubOp
//////////////////////////////////////////////////////////////////////////

class WatchUnsubOp extends HOp
{
  public String name() { return "watchUnsub"; }
  public String summary() { return "Watch unsubscription"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    // lookup watch, silently ignore failure
    String watchId = req.meta().getStr("watchId");
    HWatch watch = db.watch(watchId, false);

    // check for close or unsub
    if (watch != null)
    {
      if (req.meta().has("close"))
        watch.close();
      else
        watch.unsub(gridToIds(req));
    }

    // nothing to return
    return HGrid.EMPTY;
  }
}

//////////////////////////////////////////////////////////////////////////
// WatchPollOp
//////////////////////////////////////////////////////////////////////////

class WatchPollOp extends HOp
{
  public String name() { return "watchPoll"; }
  public String summary() { return "Watch poll cov or refresh"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    // lookup watch
    String watchId = req.meta().getStr("watchId");
    HWatch watch = db.watch(watchId);

    // poll cov or refresh
    if (req.meta().has("refresh"))
      return watch.pollRefresh();
    else
      return watch.pollChanges();
  }
}

//////////////////////////////////////////////////////////////////////////
// HisReadOp
//////////////////////////////////////////////////////////////////////////

class HisReadOp extends HOp
{
  public String name() { return "hisRead"; }
  public String summary() { return "Read time series from historian"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    if (req.isEmpty()) throw new Exception("Request has no rows");
    HRow row = req.row(0);
    HRef id = row.id();
    String range = row.getStr("range");
    HHisItem[] items = db.hisRead(id, range);
    return HGridBuilder.hisItemsToGrid(id, items);
  }
}

//////////////////////////////////////////////////////////////////////////
// HisWriteOp
//////////////////////////////////////////////////////////////////////////

class HisWriteOp extends HOp
{
  public String name() { return "hisWrite"; }
  public String summary() { return "Write time series data to historian"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    if (req.isEmpty()) throw new Exception("Request has no rows");
    HRef id = req.meta().id();
    HHisItem[] items = HHisItem.gridToItems(req);
    db.hisWrite(id, items);
    return HGrid.EMPTY;
  }
}