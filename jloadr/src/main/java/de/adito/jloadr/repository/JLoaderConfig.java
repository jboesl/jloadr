package de.adito.jloadr.repository;

import de.adito.jloadr.api.IResourceId;
import de.adito.jloadr.common.*;
import org.w3c.dom.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.*;

/**
 * @author j.boesl, 22.12.16
 */
public class JLoaderConfig
{
  public static final IResourceId CONFIG_ID = new ResourceId("jloadrConfig.xml");

  public static final String TAG_JAVA = "javaHome";
  public static final String TAG_VM_PARAMETER = "vmParameter";
  public static final String TAG_CLASSPATH = "classpath";
  public static final String TAG_CLASSPATH_DIRECTORY = "classpathDirectory";
  public static final String TAG_MAIN = "main";
  public static final String TAG_ARGUMENT = "argument";

  private String javaHome;
  private List<String> vmParameters;
  private List<String> classpath;
  private List<String> classpathDirectories;
  private String mainCls;
  private List<String> arguments;


  public void load(InputStream pInputStream)
  {
    Document document = XMLUtil.loadDocument(pInputStream);
    Element root = document.getDocumentElement();

    javaHome = XMLUtil.getChildText(root, TAG_JAVA);

    vmParameters = XMLUtil.findChildElements(root, TAG_VM_PARAMETER).stream()
        .map(element -> element.getTextContent().trim())
        .collect(Collectors.toList());

    classpath = XMLUtil.findChildElements(root, TAG_CLASSPATH).stream()
        .map(element -> element.getTextContent().trim())
        .collect(Collectors.toList());

    classpathDirectories = XMLUtil.findChildElements(root, TAG_CLASSPATH_DIRECTORY).stream()
        .map(element -> element.getTextContent().trim())
        .collect(Collectors.toList());

    mainCls = XMLUtil.getChildText(root, TAG_MAIN);
    assert mainCls != null;

    arguments = XMLUtil.findChildElements(root, TAG_ARGUMENT).stream()
        .map(element -> element.getTextContent().trim())
        .collect(Collectors.toList());
  }

  public void save(OutputStream pOutputStream)
  {
    XMLUtil.saveDocument(pOutputStream, pDocument -> {
      Element root = pDocument.createElement("jloadr");
      pDocument.appendChild(root);
      _append(pDocument, root, TAG_JAVA, javaHome);
      _append(pDocument, root, TAG_VM_PARAMETER, vmParameters);
      _append(pDocument, root, TAG_CLASSPATH, classpath);
      _append(pDocument, root, TAG_CLASSPATH_DIRECTORY, classpathDirectories);
      _append(pDocument, root, TAG_MAIN, mainCls);
      _append(pDocument, root, TAG_ARGUMENT, arguments);
    });
  }

  public String[] getStartCommands(Path pWorkingDirectory, List<String> pAdditionalVmParameters)
  {
    String mainCls = getMainCls();
    if (mainCls == null || mainCls.isEmpty())
      throw new RuntimeException("Application can't be started. No main class provided.");

    List<String> parameters = new ArrayList<>();
    parameters.add(_getStartJavaCommand(pWorkingDirectory));
    Stream.concat(getVmParameters().stream(), pAdditionalVmParameters == null ? Stream.empty() : pAdditionalVmParameters.stream())
        .map(param -> "-D" + param)
        .forEach(parameters::add);

    String cp = Stream.concat(getClasspathDirectories().stream()
                                  .map(str -> str + (str.endsWith("/") ? "*" : "/*")),
                              getClasspath().stream())
        .map(str -> str.replace('/', File.separatorChar))
        .collect(Collectors.joining(File.pathSeparator));
    if (!cp.isEmpty())
    {
      parameters.add("-cp");
      parameters.add(cp);
    }
    parameters.add(mainCls);
    parameters.addAll(getArguments());

    return parameters.toArray(new String[parameters.size()]);
  }

  private String _getStartJavaCommand(Path pWorkingDirectory)
  {
    String javaHome = getJavaHome();
    return ProcessUtil.findJavaCmd(pWorkingDirectory, javaHome);
  }

  public String getJavaHome()
  {
    return javaHome;
  }

  public void setJavaHome(String pJavaHome)
  {
    javaHome = pJavaHome;
  }

  public List<String> getVmParameters()
  {
    return vmParameters;
  }

  public void setVmParameters(List<String> pVmParameters)
  {
    vmParameters = pVmParameters;
  }

  public List<String> getClasspath()
  {
    return classpath;
  }

  public void setClasspath(List<String> pClasspath)
  {
    classpath = pClasspath;
  }

  public List<String> getClasspathDirectories()
  {
    return classpathDirectories;
  }

  public void setClasspathDirectories(List<String> pClasspathDirectories)
  {
    classpathDirectories = pClasspathDirectories;
  }

  public String getMainCls()
  {
    return mainCls;
  }

  public void setMainCls(String pMainCls)
  {
    mainCls = pMainCls;
  }

  public List<String> getArguments()
  {
    return arguments;
  }

  public void setArguments(List<String> pArguments)
  {
    arguments = pArguments;
  }

  private void _append(Document pDocument, Element pAppendTo, String pTag, String pValue)
  {
    if (pValue != null)
    {
      Element element = pDocument.createElement(pTag);
      element.setTextContent(pValue);
      pAppendTo.appendChild(element);
    }
  }

  private void _append(Document pDocument, Element pAppendTo, String pTag, List<String> pValues)
  {
    if (pValues != null)
    {
      for (String value : pValues)
      {
        Element element = pDocument.createElement(pTag);
        element.setTextContent(value);
        pAppendTo.appendChild(element);
      }
    }
  }

}
