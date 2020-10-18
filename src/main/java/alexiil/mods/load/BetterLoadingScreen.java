package alexiil.mods.load;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import alexiil.mods.load.sound.YMusicTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import alexiil.mods.load.ModLoadingListener.State;
import alexiil.mods.load.git.Commit;
import alexiil.mods.load.git.GitHubUser;
import alexiil.mods.load.git.Release;
import alexiil.mods.load.git.SiteRequester;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.FMLModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLLoadEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLStateEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Lib.Mod.ID, guiFactory = "alexiil.mods.load.gui.ConfigGuiFactory", acceptableRemoteVersions = "*")
public class BetterLoadingScreen {
    @Instance(Lib.Mod.ID)
    public static BetterLoadingScreen instance;

    private static List<GitHubUser> contributors = null;
    private static List<Commit> commits = null;
    private static List<Release> releases = null;
    private static Commit thisCommit = null;
    public static ModMetadata meta;

    private static Logger log = LogManager.getLogger("betterloadingscreen");
    private static String defaultSound = "betterloadingscreen:rhapsodia_orb";
//    private YMusicTicker yMusicTicker;

    @EventHandler
    public void construct(FMLConstructionEvent event) throws IOException {
        ModLoadingListener thisListener = null;
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if (mod instanceof FMLModContainer) {
                EventBus bus = null;
                try {
                    // Its a bit questionable to be changing FML itself, but reflection is better than ASM transforming
                    // forge
                    Field f = FMLModContainer.class.getDeclaredField("eventBus");
                    f.setAccessible(true);
                    bus = (EventBus) f.get(mod);
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
                if (bus != null) {
                    if (mod.getModId().equals(Lib.Mod.ID)) {
                        thisListener = new ModLoadingListener(mod);
                        bus.register(thisListener);
                    }
                    else
                        bus.register(new ModLoadingListener(mod));
                }
            }
        }
        if (thisListener != null) {
            ModLoadingListener.doProgress(State.CONSTRUCT, thisListener);
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(instance);
        FMLCommonHandler.instance().bus().register(instance);
        meta = event.getModMetadata();
//
//        this.yMusicTicker = new YMusicTicker(Minecraft.getMinecraft(), this);
//
//        MinecraftForge.EVENT_BUS.register(new YSoundHandler());

        /*alexiil.mods.load.MinecraftDisplayer.blending = true;
    	alexiil.mods.load.MinecraftDisplayer.blendingJustSet = true;*/
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event) {
    	System.out.println("ricardo milos");
    	//alexiil.mods.load.MinecraftDisplayer.blending = true;
    	//alexiil.mods.load.MinecraftDisplayer.blendingJustSet = true;
    }
    
    @EventHandler
    public void inithmm(FMLStateEvent event) {
    	/*if (!alexiil.mods.load.MinecraftDisplayer.blending) {
	    	System.out.println("shrek");
	    	alexiil.mods.load.MinecraftDisplayer.blending = true;
	    	alexiil.mods.load.MinecraftDisplayer.blendingJustSet = true;
    	}*/
    	System.out.println("bruh: "+event.toString());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void guiOpen(GuiOpenEvent event) throws IOException {
        if (event.gui != null && event.gui instanceof GuiMainMenu)
            ProgressDisplayer.close();
//        if (event.gui != null && event.gui instanceof GuiCreateWorld)
//            new Thread() {
//                @Override
//                public void run() {
//                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
//                    playSound(defaultSound);
//                }
//        }.start();
    }

    @SubscribeEvent
    public void configChanged(OnConfigChangedEvent event) {
        if (event.modID == Lib.Mod.ID)
            ProgressDisplayer.cfg.save();
    }

    @EventHandler
    @SideOnly(Side.SERVER)
    public void serverAboutToStart(FMLServerAboutToStartEvent event) throws IOException {
        ProgressDisplayer.close();
    }

    public static String getCommitHash() {
        return Lib.Mod.COMMIT_HASH;
    }

    public static int getBuildType() {
        return Lib.Mod.buildType();
    }

    public static void initSiteVersioning() {
        if (ProgressDisplayer.connectExternally) {
            String droneSite = "https://drone.io/github.com/AlexIIL/BetterLoadingScreen_1.7/files/VersionInfo/build/libs/version/";
            contributors = Collections.unmodifiableList(SiteRequester.getContributors(droneSite + "contributors.json"));
            if (contributors.size() == 0)
                meta.authorList.add("Could not connect to GitHub to fetch the rest...");
            for (GitHubUser c : contributors) {
                if ("AlexIIL".equals(c.login))
                    continue;
                meta.authorList.add(c.login);
            }

            commits = SiteRequester.getCommits(droneSite + "commits.json");
            Collections.sort(commits, new Comparator<Commit>() {
                @Override
                public int compare(Commit c0, Commit c1) {
                    return c1.commit.committer.date.compareTo(c0.commit.committer.date);
                }
            });
            commits = Collections.unmodifiableList(commits);

            for (Commit c : commits)
                if (getCommitHash().equals(c.sha))
                    thisCommit = c;
            if (thisCommit == null && commits.size() > 0 && getBuildType() == 2) {
                System.out.println("Didn't find my commit! This is unexpected, consider this a bug!");
                System.out.println("Commit Hash : \"" + getCommitHash() + "\"");
            }

            releases = Collections.unmodifiableList(SiteRequester.getReleases(droneSite + "releases.json"));
        }
    }

    public static List<GitHubUser> getContributors() {
        if (contributors == null)
            initSiteVersioning();
        return contributors;
    }

    public static List<Commit> getCommits() {
        if (contributors == null)
            initSiteVersioning();
        return commits;
    }

    public static Commit getCurrentCommit() {
        if (contributors == null)
            initSiteVersioning();
        return thisCommit;
    }

    public static List<Release> getReleases() {
        if (contributors == null)
            initSiteVersioning();
        return releases;
    }

    public static void playSound(String soundString) {
        SoundHandler soundHandler = Minecraft.getMinecraft().getSoundHandler();
        ResourceLocation location = new ResourceLocation(soundString);
        SoundEventAccessorComposite snd = soundHandler.getSound(location);
        if (snd == null) {
            log.warn("The sound given (" + soundString + ") did not give a valid sound!");
            location = new ResourceLocation(defaultSound);
            snd = soundHandler.getSound(location);
        }
        if (snd == null) {
            log.warn("Default sound did not give a valid sound!");
            return;
        }
        ISound sound = PositionedSoundRecord.func_147673_a(location);
        soundHandler.playSound(sound);
    }

    public YMusicTicker.MusicType func_147109_W()
    {
        return (Minecraft.getMinecraft().currentScreen instanceof GuiWinGame && Minecraft.getMinecraft().thePlayer != null) ?  YMusicTicker.MusicType.GAME : YMusicTicker.MusicType.MENU;
    }
}
