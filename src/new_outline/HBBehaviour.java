package new_outline;

import net.happybrackets.core.HBAction;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;

public class HBBehaviour extends  Renderer implements HBAction {


    //public static String installationConfig = "installationConfig.txt";
    public static int oscPort = 5555;
    public static RendererController.RenderMode renderMode = RendererController.RenderMode.UNITY;
    HB hb;
    RendererController rc = RendererController.getInstance();

    @Override
    public void setupAudio() {
        System.out.println("setupAudio: " + this.name);
    }

    @Override
    public void setupLight() {
        System.out.println("setupLight: " + this.name);
    }

    @Override
    public void tick(Clock clock) {
        System.out.println("tick: " + clock.getNumberTicks() + " device: " + this.name);
    }

    @Override
    public void action(HB hb) {
        this.hb = hb;

        hb.reset();
        hb.setStatus(this.getClass().getSimpleName() + " Loaded");

        rc.getInternalClock().setInterval(1000);
        rc.getInternalClock().start();
    }

    //<editor-fold defaultstate="collapsed" desc="Debug Start">
    /**
     * This function is used when running sketch in IntelliJ IDE for debugging or testing
     *
     * @param args standard args required
     */
    public static void main(String[] args) {

        try {
            HB.runDebug(MethodHandles.lookup().lookupClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //</editor-fold>

}
