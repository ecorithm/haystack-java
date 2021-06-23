package org.projecthaystack.server;

import java.io.*;
import javax.servlet.*;

public class CachedBodyServletInputStream extends ServletInputStream
{
  public CachedBodyServletInputStream(byte[] cachedBody)
  {
    mCachedBodyInputStream = new ByteArrayInputStream(cachedBody);
  }

  @Override
  public boolean isFinished()
  {
    try {
      return (mCachedBodyInputStream.available() == 0);
    }
    catch (IOException e) {
      return true;
    }
  }

  @Override
  public boolean isReady()
  {
    return true;
  }

  @Override
  public int read() throws IOException
  {
    return mCachedBodyInputStream.read();
  }

  @Override
  public void setReadListener(ReadListener readListener)
  {
    readListener.notify();
  }

  private InputStream mCachedBodyInputStream;
}
