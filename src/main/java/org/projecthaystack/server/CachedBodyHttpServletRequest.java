package org.projecthaystack.server;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.io.*;

/**
 * Allows the body of a request to be read multiple times.
 * 
 * @see <a href="https://www.baeldung.com/spring-reading-httpservletrequest-multiple-times">Source</a>
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper
{
  public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException
  {
    super(request);
    mCachedBody = IOUtils.toByteArray(request.getInputStream());
  }

  @Override
  public ServletInputStream getInputStream() throws IOException
  {
    return new CachedBodyServletInputStream(mCachedBody);
  }

  @Override
  public BufferedReader getReader() throws IOException
  {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(mCachedBody);
    return new BufferedReader(new InputStreamReader(byteArrayInputStream));
  }

  private byte[] mCachedBody;
}
