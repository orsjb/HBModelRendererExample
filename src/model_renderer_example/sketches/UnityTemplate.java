package model_renderer_example.sketches;

import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class UnityTemplate implements HBAction, HBReset {

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

        //For unity, send this code to HB simulator
        String computerName = null;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //play stuff on your speakers at home
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,0,0,0,"speaker",0);
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,100,0,0,"speaker",0);

        //properties of the renderers
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv");

        //Assign each renderer a GenericSampleAndClockRenderer
        rc.renderers.forEach(renderer -> {
            renderers.add((GenericSampleAndClockRenderer) renderer);
        });

        //setup
        renderers.forEach(r->{


        });

        //do stuff
        rc.addClockTickListener((v, clock) -> {
                renderers.forEach(r -> {

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
