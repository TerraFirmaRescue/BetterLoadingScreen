package alexiil.mods.load;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import cpw.mods.fml.client.FMLFileResourcePack;
import cpw.mods.fml.client.SplashProgress;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;


public class ProgressDisplayer {
	private static Logger log;
	private static boolean hasTurnedSplashOff = false;
	private static boolean forgeSplashWasTrue = false;
    public interface IDisplayer {
        void open(Configuration cfg);
        void displayProgress(String text, float percent);
        void close();
    }

    public static class FrameDisplayer implements IDisplayer {
        private LoadingFrame frame = null;

        @Override
        public void open(Configuration cfg) {
            frame = LoadingFrame.openWindow();
            if (frame != null) {
                frame.setMessage("Minecraft Forge Starting");
                frame.setProgress(0);
            }
        }

        @Override
        public void displayProgress(String text, float percent) {
            if (frame == null)
                return;
            frame.setMessage(text);
            frame.setProgress(percent * 100F);
            frame.repaint();
        }

        @Override
        public void close() {
            if (frame != null)
                frame.dispose();
        }
    }

    public static class LoggingDisplayer implements IDisplayer {
        private Logger log;

        @Override
        public void open(Configuration cfg) {
            log = LogManager.getLogger("betterloadingscreen");
        }

        @Override
        public void displayProgress(String text, float percent) {
            log.info(text + " (" + (int) (percent * 100) + "%)");
        }

        @Override
        public void close() {}
    }

    private static IDisplayer displayer;
    private static int clientState = -1;
    public static Configuration cfg;
    public static boolean connectExternally, playSound;
    public static File coreModLocation;
    public static ModContainer modContainer;

    private static boolean hasInitRL = false;

    public static boolean isClient() {
        if (clientState != -1)
            return clientState == 1;
        StackTraceElement[] steArr = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : steArr) {
            if (ste.getClassName().startsWith("cpw.mods.fml.relauncher.ServerLaunchWrapper")) {
                clientState = 0;
                return false;
            }
        }
        clientState = 1;
        return true;
    }

    private static void loadResourceLoader() {
        try {
            Class<?> resLoaderClass = Class.forName("lumien.resourceloader.ResourceLoader");
            Object instance = resLoaderClass.newInstance();
            resLoaderClass.getField("INSTANCE").set(null, instance);
            Method m = resLoaderClass.getMethod("preInit", FMLPreInitializationEvent.class);
            m.invoke(instance, new Object[] { null });
            System.out.println("Resource loader loaded early succssessfully :)");
        }
        catch (ClassNotFoundException ex) {
            System.out.println("Resource loader not loaded, not initialising early");
        }
        catch (Throwable t) {
            System.out.println("Resource Loader Compat FAILED!");
            t.printStackTrace();
        }
    }

    public static void start(File coremodLocation) {
        LoadingFrame.setSystemLAF();
        coreModLocation = coremodLocation;
        if (coreModLocation == null)
            coreModLocation = new File("./../bin/");
        // Assume this is a dev environment, and that the build dir is in bin, and the test dir has the same parent as
        // the bin dir...
        ModMetadata md = new ModMetadata();
        md.name = Lib.Mod.NAME;
        md.modId = Lib.Mod.ID;
        modContainer = new DummyModContainer(md) {
            @Override
            public Class<?> getCustomResourcePackClass() {
                return FMLFileResourcePack.class;
            }

            @Override
            public File getSource() {
                return coreModLocation;
            }

            @Override
            public String getModId() {
                return Lib.Mod.ID;
            }
        };

        //File fileOld = new File("./config/betterloadingscreen.cfg");
        //File fileNew = new File("./config/BetterLoadingScreen/config.cfg");
        File fileOld = new File("./config/Betterloadingscreen/betterloadingscreen.cfg");

        /*if (fileOld.exists())
            cfg = new Configuration(fileOld);
        else
            cfg = new Configuration(fileNew);*/
        String n = System.lineSeparator();
        cfg = new Configuration(fileOld);
        
        boolean useMinecraft = isClient();

        if (useMinecraft) {
            String comment =
                    "Whether or not to use minecraft's display to show the progress. This looks better, but there is a possibilty of not being ";
            comment += "compatible, so if you do have any strange crash reports or compatability issues, try setting this to false" + n +
            		"Note: IIRC, setting this to false makes the screen black";
            useMinecraft = cfg.getBoolean("useMinecraft", "general", true, comment);
        }

        connectExternally = cfg.getBoolean("connectExternally", "general", false, "If this is true, it will conect to drone.io to get a changelog." + n + "Note: this repo is down.");

        playSound = cfg.getBoolean("playSound", "general", true, "Play a sound after minecraft has finished starting up");

        if (useMinecraft)
            displayer = new MinecraftDisplayerWrapper();
        else if (!GraphicsEnvironment.isHeadless())
            displayer = new FrameDisplayer();
        else
            displayer = new LoggingDisplayer();
        displayer.open(cfg);
        cfg.save();
    }
    
    public static boolean turnForgeSplashOff(String file) throws IOException {
    	log = LogManager.getLogger("betterloadingscreen");
    	boolean hasTurnedOff = false;
    	BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuffer inputBuffer = new StringBuffer();
            //System.out.println("got the file");
            String line;
            while ((line = reader.readLine()) != null) {
				//System.out.println(line);
				if (line.equals("enabled=true")) {
					line = "enabled=false";
					hasTurnedOff = true;
				}
				inputBuffer.append(line);
				inputBuffer.append('\n');
            }
            reader.close();
            
            FileOutputStream fileOut = new FileOutputStream(file);
            fileOut.write(inputBuffer.toString().getBytes());
            fileOut.close();
            log.info("Turned Forge splash screen off in splash.properties");
        }
        catch (FileNotFoundException e) {
        	log.warn("Error while opening splash.properties");
        }
        return hasTurnedOff;
    }
    
    public static void turnForgeSplashOn(String file) throws IOException {
    	log = LogManager.getLogger("betterloadingscreen");
    	BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuffer inputBuffer = new StringBuffer();
            //System.out.println("got the file");
            String line;
            while ((line = reader.readLine()) != null) {
				//System.out.println(line);
				if (line.equals("enabled=false")) {
					line = "enabled=true";
				}
				inputBuffer.append(line);
				inputBuffer.append('\n');
            }
            reader.close();
            
            FileOutputStream fileOut = new FileOutputStream(file);
            fileOut.write(inputBuffer.toString().getBytes());
            fileOut.close();
            log.info("Turned Forge splash screen on in splash.properties");
        }
        catch (FileNotFoundException e) {
        	log.warn("Error while opening splash.properties");
        }
    }
    
    public static void displayProgress(String text, float percent) throws IOException {
    	if (!hasTurnedSplashOff) {
    		String file = "./config/splash.properties";
			hasTurnedSplashOff = true;
    		if (turnForgeSplashOff(file)) {
    			forgeSplashWasTrue = true;
    		}
		}
        if (!hasInitRL) {
            loadResourceLoader();
            overrideForgeSplashProgress();
            hasInitRL = true;
        }
        displayer.displayProgress(text, percent);
    }

    public static void close() throws IOException {
        if (displayer == null)
            return;
        displayer.close();
        displayer = null;
        if (isClient() && playSound) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException e) {}
                    MinecraftDisplayerWrapper.playFinishedSound();
                }
            }.start();
        }
        String file = "./config/splash.properties";
        if (forgeSplashWasTrue) {
			turnForgeSplashOn(file);
		}
    }

    private static void overrideForgeSplashProgress() {
        Class<?> cl = null;
        Field fi = null;
        try {
            cl = Class.forName("cpw.mods.fml.client.SplashProgress");
            fi = cl.getDeclaredField("enabled");
            fi.setAccessible(true);
            fi.set(null, false);
            // Set this just to make forge's screen exit ASAP.
            fi = cl.getDeclaredField("done");
            fi.setAccessible(true);
            fi.set(null, true);
        }
        catch (Throwable t) {
            System.out.println("Could not override forge's splash screen for some reason...");
            System.out.println("class = " + cl);
            System.out.println("field = " + fi);
            t.printStackTrace();
        }
    }

    public static void minecraftDisplayFirstProgress() throws IOException {
        displayProgress(Translation.translate("betterloadingscreen.state.minecraft_init", "Minecraft Initializing"), 0F);
    }
}
