package de.adito.jloadr;

import de.adito.jloadr.api.*;
import de.adito.jloadr.repository.*;
import de.adito.jloadr.repository.local.LocalStore;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * @author j.boesl, 05.09.16
 */
public class Main
{

  public static void main(String[] args) throws IOException, InterruptedException
  {

    URL url = new URL(args[0]);
    IResourcePack remoteResourcePack = ResourcePackFactory.get(url);

    Splash splash = GraphicsEnvironment.isHeadless() ? null : new Splash();

    try {
      LocalStore localStore = new LocalStore(Paths.get("jloadr"));

      IStoreResourcePack localResourcePack = new Loader().load(localStore, remoteResourcePack, splash);

      IStoreResource configResource = localResourcePack.getResource(JLoaderConfig.CONFIG_ID);
      if (configResource != null) {
        JLoaderConfig loaderConfig = new JLoaderConfig();
        try (InputStream inputStream = configResource.getInputStream()) {
          loaderConfig.load(inputStream);
        }

        Process process = new ProcessBuilder(loaderConfig.getStartCommands())
            .directory(new File("jloadr", localResourcePack.getId()))
            .inheritIO()
            .start();

        process.waitFor(4, TimeUnit.SECONDS);
      }
    }
    finally {
      if (splash != null)
        SwingUtilities.invokeLater(splash::dispose);
    }
  }


  private static class Splash extends JFrame implements ILoader.IStateCallback
  {
    private int elementCount;
    private double loaded;


    @Override
    public void inited(IResource pSplashResource, int pElementCount)
    {
      elementCount = pElementCount;

      JLabel label = null;
      if (pSplashResource != null) {
        try {
          label = new JLabel("", new ImageIcon(ImageIO.read(pSplashResource.getInputStream())), SwingConstants.CENTER);
        }
        catch (IOException pE) {
          // no image
        }
      }
      if (label == null)
        label = new JLabel("loading ...", SwingConstants.CENTER);

      getContentPane().add(label);

      setUndecorated(true);
      pack();
      setLocationRelativeTo(null);
      setVisible(true);
    }

    @Override
    public void loaded(int pElementNumber)
    {
      setPercentage((double) pElementNumber / (double) elementCount);
    }

    @Override
    public void finished()
    {
      setPercentage(1);
    }

    private void setPercentage(double pLoaded)
    {
      SwingUtilities.invokeLater(() -> {
        loaded = pLoaded;
        revalidate();
        repaint();
      });
    }

    @Override
    public void paint(Graphics g)
    {
      super.paint(g);
      int w = getContentPane().getWidth();
      int h = getContentPane().getHeight();
      g.setColor(SystemColor.GRAY);
      g.fillRect(4, h - 8, (int) Math.round((w - 8) * loaded), 4);
    }
  }

}
