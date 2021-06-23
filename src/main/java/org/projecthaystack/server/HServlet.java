//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package org.projecthaystack.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 * HServlet implements the haystack HTTP REST API for
 * querying entities and history data.
 *
 * @see <a href='http://project-haystack.org/doc/Rest'>Project Haystack</a>
 */
@WebServlet(name = "Haystack", urlPatterns = "/*")
public class HServlet extends HttpServlet
{

//////////////////////////////////////////////////////////////////////////
// Initialization
//////////////////////////////////////////////////////////////////////////

  @Override
  public void init() throws ServletException
  {
    logger = Logger.getLogger(this.getClass().getName());
    logger.setLevel(Level.FINE);
    final int MEGABYTE_AS_BYTES = 1024 * 1024;
    try {
      logger.addHandler(new FileHandler(this.getClass().getSimpleName() + ".log", MEGABYTE_AS_BYTES, 1));
    }
    catch (IOException e) {
      logger.log(Level.WARNING, "Failed to create log file:", e);
    }

    db = new TestDatabase();
  }

//////////////////////////////////////////////////////////////////////////
// HttpServlet Hooks
//////////////////////////////////////////////////////////////////////////

  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    onService("GET", req, res);
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    onService("POST", new CachedBodyHttpServletRequest(req), res);
  }

//////////////////////////////////////////////////////////////////////////
// Service
//////////////////////////////////////////////////////////////////////////

  private void onService(String method, HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    dumpReq(req);
    // if root, then redirect to {haystack}/about
    String path = req.getPathInfo();
    if (path == null || path.length() == 0 || path.equals("/"))
    {
      logger.info("Received request for root path, redirecting to '/about'.");
      res.sendRedirect(req.getContextPath() + "/about");
      return;
    }

    // parse URI path into "/{opName}/...."
    int slash = path.indexOf('/', 1);
    if (slash < 0) slash = path.length();
    String opName = path.substring(1, slash);

    // resolve the op
    HOp op = db.op(opName, false);
    if (op == null)
    {
      logger.warning("Invalid op received: " + opName);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // route to the op
    try
    {
      op.onService(db, req, res);
    }
    catch (Exception e)
    {
      logger.log(Level.WARNING, "Exception while processing op:", e);
      throw new ServletException(e);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  void dumpReq(HttpServletRequest req)
  {
    logger.fine("==========================================");
    logger.fine("method      = " + req.getMethod());
    logger.fine("pathInfo    = " + req.getPathInfo());
    logger.fine("contextPath = " + req.getContextPath());
    logger.fine("servletPath = " + req.getServletPath());
    try {
      logger.fine("query       = " + (req.getQueryString() == null ? "null" : URLDecoder.decode(req.getQueryString(), "UTF-8")));
    }
    catch (UnsupportedEncodingException e) {
      logger.log(Level.WARNING, "Failed to decode query string:", e);
    }

    logger.fine("headers:");
    Enumeration<String> e = req.getHeaderNames();
    while (e.hasMoreElements())
    {
      String key = e.nextElement();
      String val = req.getHeader(key);
      logger.fine("  " + key + " = " + val);
    }

    if (req.getMethod().equals("POST")) {
      logger.fine("body:");
      try {
        BufferedReader reader = req.getReader();
        String line = null;
        while ((line = reader.readLine()) != null) {
          logger.fine("  " + line);
        }
      }
      catch (Exception ex) {
        logger.log(Level.WARNING, "Exception while trying to log POST body:", ex);
      }
    }
  }

  private HServer db;
  private Logger logger;
}
