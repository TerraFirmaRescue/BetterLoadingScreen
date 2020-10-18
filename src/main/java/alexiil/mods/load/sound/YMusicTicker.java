package alexiil.mods.load.sound;

import alexiil.mods.load.BetterLoadingScreen;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

@Deprecated
public class YMusicTicker implements IUpdatePlayerListBox {

    private final Random random = new Random();
    private final Minecraft minecraft;
    private ISound iSound;
    private int num = 100;
    private static final String __OBFID = "CL_00001138";
    private final BetterLoadingScreen bls;

    public YMusicTicker(Minecraft minecraft, BetterLoadingScreen bls)
    {
        this.minecraft = minecraft;
        this.bls = bls;
    }

    public void update()
    {
        YMusicTicker.MusicType musictype = this.bls.func_147109_W();

        if (this.iSound != null)
        {
            if (!musictype.getMusicTickerLocation().equals(this.iSound.getPositionedSoundLocation()))
            {
                this.minecraft.getSoundHandler().stopSound(this.iSound);
                this.num = MathHelper.getRandomIntegerInRange(this.random, 0, musictype.func_148634_b() / 2);
            }

            if (!this.minecraft.getSoundHandler().isSoundPlaying(this.iSound))
            {
                this.iSound = null;
                this.num = Math.min(MathHelper.getRandomIntegerInRange(this.random, musictype.func_148634_b(), musictype.func_148633_c()), this.num);
            }
        }

        if (this.iSound == null && this.num-- <= 0)
        {
            this.iSound = PositionedSoundRecord.func_147673_a(musictype.getMusicTickerLocation());
            this.minecraft.getSoundHandler().playSound(this.iSound);
            this.num = Integer.MAX_VALUE;
        }
    }

    @SideOnly(Side.CLIENT)
    public static enum MusicType
    {
        MENU(new ResourceLocation("betterloadingscreen:music.menu"), 12000, 24000),
        GAME(new ResourceLocation("betterloadingscreen:music.game"), 12000, 24000),
        WORLD(new ResourceLocation("betterloadingscreen:music.world"), 12000, 24000);

        private final ResourceLocation field_148645_h;
        private final int field_148646_i;
        private final int field_148643_j;

        private static final String __OBFID = "CL_00001139";

        private MusicType(ResourceLocation p_i45111_3_, int p_i45111_4_, int p_i45111_5_)
        {
            this.field_148645_h = p_i45111_3_;
            this.field_148646_i = p_i45111_4_;
            this.field_148643_j = p_i45111_5_;
        }

        public ResourceLocation getMusicTickerLocation()
        {
            return this.field_148645_h;
        }

        public int func_148634_b()
        {
            return this.field_148646_i;
        }

        public int func_148633_c()
        {
            return this.field_148643_j;
        }
    }
}
