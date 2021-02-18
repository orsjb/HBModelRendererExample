package new_outline;

import net.happybrackets.core.HBAction;
import net.happybrackets.device.HB;

public abstract class HBBehaviour extends OutputRenderer implements HBAction {

    public static enum RenderMode {
        UNITY, REAL
    }

    public static String installationConfig = "installationConfig.txt";
    public static int oscPort = 5555;
    public static RenderMode renderMode = RenderMode.UNITY;

    @Override
    public void action(HB hb) {
        //notify RendererManager to set me up
        RendererManager.getInstance().setup(this, hb);
    }

    public abstract void setupSound();

    public abstract void setupLight();

    public abstract void tick();

}
