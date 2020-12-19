package model_renderer_example.sketches;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.happybrackets.core.Device;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExampleForASUFolksMYSECONDEXPERIMENT implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GenericSampleAndClockRenderer> renderers = new ArrayList();
    Map<Sample, int[]> sampleColourMappings = new HashMap<>();

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
        Sample bd = SampleManager.sample("data/audio/Mattel_Drum_Machine/MatBd.wav");
        Sample cym = SampleManager.sample("data/audio/Mattel_Drum_Machine/MatCym.wav");

        sampleColourMappings.put(bd, new int[]{255, 0, 0});
        sampleColourMappings.put(cym, new int[]{0, 255, 0});

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
            //sound stuff
            r.useGranularSamplePlayer();
            r.clockInterval(0);        //this is measured in ticks
            r.setSample(bd);
            r.gain(1f);
            //r.brightness(1);        //pulse brightness overrides brightness
            r.decay(1f);          //if decay = 1, then no pulsing
            //r.pulseBrightness(1);   //pulse and decay are connected, light pulse triggered along with sample reset (clockInterval and clockDecay)
            //light stuff
            int[] colour = sampleColourMappings.get(bd);
            r.setRGB(colour[0], colour[1], colour[2]);
        });

        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r -> {
                if(clock.getNumberTicks() % 20 == 0) {
                    //a musical event has happened
                    r.masterBrightness = 1;
                    r.gain.setValue(1);
                    r.setSample(SampleManager.sample("data/audio/Mattel_Drum_Machine/MatCym.wav"));
                    r.position(0);  //retriggering the sample
                    r.rate(2);
                } else {
                    //all the rest of the time
                    r.masterBrightness *= 0.9f;
                    r.gain.setValue(r.gain.getValue() * 0.9f);
                }
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

        // type osclistener to create this code
        OSCUDPListener oscudpListener = new OSCUDPListener(4000) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long time) {
                renderers.forEach(r->{
                    if(oscMessage.getName().equals("/volume")) {
                        r.gain(hb.getFloatArg(oscMessage, 0));
                    }
                });
            }
        };

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
