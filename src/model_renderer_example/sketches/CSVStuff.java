package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.core.control.BooleanControl;
import net.happybrackets.core.control.DynamicControl;
import net.happybrackets.core.control.FloatControl;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.RendererController;
import the_mind_at_work.renderers.GenericSampleAndClockRenderer;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

//This example moves blobs around the Unity simulator in a circular orbit on a 2D plane
//You can use the internal clock, or an OSC listener
//The internal clock has Dynamic Controls

public class CSVStuff implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GenericSampleAndClockRenderer> renderers = new ArrayList<>();

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(20); //ms time of each tick
        rc.getInternalClock().start();

        //set up the RC
        rc.setRendererClass(GenericSampleAndClockRenderer.class);

        //For unity, use the HB simulator, send this code to the HB simulator
        rc.loadHardwareConfigurationforUnity("Device/HappyBrackets/config/hardware_setup_casula_km.csv");

        //Assign each renderer a GenericSampleAndClockRenderer
        rc.renderers.forEach(renderer -> {
            renderers.add((GenericSampleAndClockRenderer) renderer);
        });

        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r -> {
                int lightRing = Integer.parseInt(r.csvData.get("lightRing"));
                if (clock.getNumberTicks() % 30 == lightRing * 5) {
                    r.brightness(1f);
                    r.decay(0.97f);
                    r.setRGB(255, 255 / 6 * lightRing, 255 / 30 * lightRing);
                }
            });
            rc.sendSerialcommand();
        });
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

    @Override
    public void doReset()
    {
        rc.turnOffLEDs();
        rc.reset();
    }
    //</editor-fold>
}