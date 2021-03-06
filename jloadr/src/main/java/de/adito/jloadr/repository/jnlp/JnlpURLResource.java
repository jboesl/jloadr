package de.adito.jloadr.repository.jnlp;

import de.adito.jloadr.api.*;
import de.adito.jloadr.repository.*;
import org.w3c.dom.Element;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * IResource-Impl
 */
class JnlpURLResource implements IResource
{
  private JnlpReference jarJnlpReference;
  private URLResource resource;

  public JnlpURLResource(JnlpReference pJarJnlpReference)
  {
    jarJnlpReference = pJarJnlpReference;
  }

  @Override
  public IResourceId getId()
  {
    IResourceId id = _getResource().getId();
    String idStr = id.toString();
    String codebase = jarJnlpReference.getCodebase().getPath();
    if (idStr.startsWith(codebase))
      return new ResourceId(idStr.substring(codebase.length()));
    if (idStr.startsWith("/"))
      return new ResourceId(idStr.substring(1));
    return id;
  }

  @Override
  public String getHash()
  {
    return _getResource().getHash();
  }

  @Override
  public InputStream getInputStream() throws IOException
  {
    return _getResource().getInputStream();
  }

  @Override
  public long getSize() throws IOException
  {
    return _getResource().getSize();
  }

  @Override
  public long getLastModified() throws IOException
  {
    return _getResource().getLastModified();
  }

  @Override
  public boolean equals(Object pO)
  {
    if (this == pO)
      return true;
    if (pO == null || getClass() != pO.getClass())
      return false;
    JnlpURLResource that = (JnlpURLResource) pO;
    return Objects.equals(jarJnlpReference, that.jarJnlpReference);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(jarJnlpReference);
  }

  private URLResource _getResource()
  {
    if (resource == null)
    {
      Element jarElement = jarJnlpReference.getJarElement();
      String href = jarElement.getAttribute("href");
      String version = jarElement.getAttribute("version");

      List<String> variants = new ArrayList<>();
      if (version != null && !version.isEmpty())
      {
        int index = href.lastIndexOf(".jar");
        if (index != -1)
        {
          String name = href.substring(0, index);
          String versionedJar = name + "__V" + version + ".jar";
          variants.add(versionedJar);
          variants.add(versionedJar + ".pack.gz");
        }
      }
      variants.add(href);
      variants.add(href + ".pack.gz");

      for (String variant : variants)
      {
        try
        {
          URL url = new URL(jarJnlpReference.getCodebase(), variant);
          URLResource urlResource = new URLResource(url);
          try
          {
            urlResource.checkAvailable();
            resource = urlResource;
            return resource;
          }
          catch (IOException pE)
          {
            // ignore
          }
        }
        catch (MalformedURLException pE)
        {
          throw new RuntimeException("Could not resolve URL.", pE);
        }
      }
      throw new RuntimeException("resource could not be found: " + jarJnlpReference.getUrl());
    }
    return resource;
  }
}
