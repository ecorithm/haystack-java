//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package org.projecthaystack.server;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import org.projecthaystack.*;
import org.projecthaystack.io.*;

/**
 * TestDatabase provides a simple implementation of
 * HDatabase with some test entities.
 */
public class TestDatabase extends HServer
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public TestDatabase()
  {
    logger = Logger.getLogger(this.getClass().getName());
    logger.setLevel(Level.INFO);
    final int MEGABYTE_AS_BYTES = 1024 * 1024;
    try {
      logger.addHandler(new FileHandler(this.getClass().getSimpleName() + ".log", MEGABYTE_AS_BYTES, 1));
    }
    catch (IOException e) {
      logger.log(Level.WARNING, "Failed to create log file:", e);
    }

    final String zincFile = "/usr/local/tomcat/alpha.zinc";
    try {
      HZincReader gridReader = new HZincReader(new FileInputStream(zincFile));
      logger.info("Grid file '" + zincFile + "' loaded.");
      HGrid grid = gridReader.readGrid();
      logger.info("Grid file '" + zincFile + "' parsed.");
      logger.info("Grid metadata: " + grid.meta().toString());

      for (Iterator it = grid.iterator(); it.hasNext();) {
        HDict row = (HDict)it.next();
        recs.put(row.id().val, row);
      }

      logger.info("Loaded " + String.valueOf(recs.size()) + " rows.");
    }
    catch (FileNotFoundException e) {
      logger.severe("File '" + zincFile + "' not found.");
    }
  }

//////////////////////////////////////////////////////////////////////////
// Ops
//////////////////////////////////////////////////////////////////////////

  public HOp[] ops()
  {
    return new HOp[] {
      HStdOps.about,
      HStdOps.ops,
      HStdOps.formats,
      HStdOps.read,
      HStdOps.nav,
      HStdOps.pointWrite,
      HStdOps.hisRead,
      HStdOps.invokeAction,
    };
  }

  public HDict onAbout() { return about; }
  private final HDict about = new HDictBuilder()
    .add("serverName",  "Pametan Test Haystack Server")
    .add("vendorName", "Haystack Java Toolkit")
    .add("vendorUri", HUri.make("http://project-haystack.org/"))
    .add("productName", "Haystack Java Toolkit")
    .add("productVersion", "2.0.0")
    .add("productUri", HUri.make("http://project-haystack.org/"))
    .toDict();

//////////////////////////////////////////////////////////////////////////
// Reads
//////////////////////////////////////////////////////////////////////////

  protected HDict onReadById(HRef id) { return (HDict)recs.get(id.val); }

  protected Iterator<HDict> iterator() { return recs.values().iterator(); }

//////////////////////////////////////////////////////////////////////////
// Navigation
//////////////////////////////////////////////////////////////////////////

  protected HGrid onNav(String navId)
  {
    // test database navId is record id
    HDict base = null;
    if (navId != null) base = readById(HRef.make(navId));

    // map base record to site, equip, or point
    String filter = "site";
    if (base != null)
    {
      if (base.has("site")) filter = "equip and siteRef==" + base.id().toCode();
      else if (base.has("equip")) filter = "point and equipRef==" + base.id().toCode();
      else filter = "navNoChildren";
    }

    // read children of base record
    HGrid grid = readAll(filter);

    // add navId column to results
    HDict[] rows = new HDict[grid.numRows()];
    Iterator it = grid.iterator();
    for (int i=0; it.hasNext(); ) rows[i++] = (HDict)it.next();
    for (int i=0; i<rows.length; ++i)
      rows[i] = new HDictBuilder().add(rows[i]).add("navId", rows[i].id().val).toDict();
    return HGridBuilder.dictsToGrid(rows);
  }

  protected HDict onNavReadByUri(HUri uri)
  {
    return null;
  }

//////////////////////////////////////////////////////////////////////////
// Watches
//////////////////////////////////////////////////////////////////////////

  protected HWatch onWatchOpen(String dis, HNum lease)
  {
    throw new UnsupportedOperationException();
  }

  protected HWatch[] onWatches()
  {
    throw new UnsupportedOperationException();
  }

  protected HWatch onWatch(String id)
  {
    throw new UnsupportedOperationException();
  }

//////////////////////////////////////////////////////////////////////////
// Point Write
//////////////////////////////////////////////////////////////////////////

  protected HGrid onPointWriteArray(HDict rec)
  {
    WriteArray array = (WriteArray)writeArrays.get(rec.id());
    if (array == null) array = new WriteArray();

    HGridBuilder b = new HGridBuilder();
    b.addCol("level");
    b.addCol("levelDis");
    b.addCol("val");
    b.addCol("who");

    for (int i=0; i<17; ++i)
      b.addRow(new HVal[] {
          HNum.make(i+1),
          HStr.make("" + (i+1)),
          array.val[i],
          HStr.make(array.who[i]),
        });
    return b.toGrid();
  }

  protected void onPointWrite(HDict rec, int level, HVal val, String who, HNum dur, HDict opts)
  {
    logger.info("onPointWrite: " + rec.dis() + "  " + val + " @ " + level + " [" + who + "]");
    WriteArray array = (WriteArray)writeArrays.get(rec.id());
    if (array == null) writeArrays.put(rec.id(), array = new WriteArray());
    array.val[level-1] = val;
    array.who[level-1] = who;
  }

  static class WriteArray
  {
    final HVal[] val = new HVal[17];
    final String[] who = new String[17];
  }

  // hacky, but keep it simple for servlet environment
  static HashMap writeArrays = new HashMap();

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

  public HHisItem[] onHisRead(HDict entity, HDateTimeRange range)
  {
    // generate dummy 15min data
    ArrayList acc = new ArrayList();
    HDateTime ts = range.start;
    boolean isBool = ((HStr)entity.get("kind")).val.equals("Bool");
    while (ts.compareTo(range.end) <= 0)
    {
      HVal val = isBool ?
        (HVal)HBool.make(acc.size() % 2 == 0) :
        (HVal)HNum.make(acc.size());
      HDict item = HHisItem.make(ts, val);
      if (ts != range.start) acc.add(item);
      ts = HDateTime.make(ts.millis() + 15*60*1000);
    }
    return (HHisItem[])acc.toArray(new HHisItem[acc.size()]);
  }

  public void onHisWrite(HDict rec, HHisItem[] items)
  {
    throw new RuntimeException("Unsupported");
  }

//////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////

  public HGrid onInvokeAction(HDict rec, String action, HDict args)
  {
    logger.info("-- invokeAction \"" + rec.dis() + "." + action + "\" " + args);
    return HGrid.EMPTY;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private Logger logger;
  private HashMap<String, HDict> recs = new HashMap<String, HDict>();
}
