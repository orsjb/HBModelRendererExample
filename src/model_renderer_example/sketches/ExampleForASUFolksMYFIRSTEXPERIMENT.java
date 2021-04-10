package model_renderer_example.sketches;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.SampleManager;
import net.happybrackets.core.Device;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.Renderer;
import net.happybrackets.rendererengine.RendererController;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ExampleForASUFolksMYFIRSTEXPERIMENT implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GenericSampleAndClockRenderer> renderers = new ArrayList();

    int count = 0;

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset();
        rc.reset();
        rc.getInternalClock().setInterval(20);
        rc.getInternalClock().stop();
        rc.getInternalClock().reset();
        rc.getInternalClock().start();
        //adding some samples
        //set up the RC
        rc.setRendererClass(GenericSampleAndClockRenderer.class);
        //set up the configuration of the system
        //rc.loadHardwareConfiguration("config/hardware_setup_casula_iml_test.csv");
        String hostname = Device.getDeviceName();
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   450,  200f, 450, "LED-W", 0, 16);
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   0, 100f, 0,  "LED-N", 1, 16);
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   0, 100f, 0,  "LED-S", 2, 16);
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   0, 100f, 0,  "LED-E", 3, 16);
        rc.addRenderer(Renderer.Type.SPEAKER, hostname, 0,  100f, 0, "SPEAKER-W", 0);
        rc.addRenderer(Renderer.Type.SPEAKER, hostname, 0, 100f, 0, "SPEAKER-E", 1);
        hb.setStatus(hostname + ", I Have " + rc.renderers.size() + " objects");

        //for convenience, grab the list of renderers cast to the class we are using
        rc.renderers.forEach(renderer -> {renderers.add((GenericSampleAndClockRenderer) renderer);});
        //some basic configuration
        renderers.forEach(r -> {
            //DO SET UP HERE
            if(r.x < 10 && r.y > 50) {
                r.useGranularSamplePlayer();
                r.clockInterval(20);        //this is measured in ticks
                r.setSample(SampleManager.sample("data/audio/Mattel_Drum_Machine/MatBd.wav"));
                r.clockDelay((int)r.x / 10 );    //this is measured in ticks
                r.gain(1f - r.x / 450);
            } else {

                r.useGranularSamplePlayer();
                r.clockInterval(20);
                r.setSample(SampleManager.sample("data/audio/Mattel_Drum_Machine/MatBd.wav"));
                r.gain(1);
            }
        });

        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r -> {
                //DO CLOCK STUFF HERE
            });
            rc.sendSerialcommand();
        });

        hb.addControllerListener(new OSCListener() {
            @Override
            public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                if(oscMessage.getName().equals("/reset_clock")) {
                    rc.getInternalClock().stop();
                    rc.getInternalClock().reset();
                    rc.getInternalClock().start();
                }
            }
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
    public void doReset() {
        rc.reset();
    }
    //</editor-fold>
}
