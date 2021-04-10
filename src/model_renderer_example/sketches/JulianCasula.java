package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;

import net.beadsproject.beads.data.SampleManager;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.GranularRenderer;
import net.happybrackets.rendererengine.Renderer;
import net.happybrackets.rendererengine.RendererController;


import javax.sound.sampled.BooleanControl;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JulianCasula implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GranularRenderer> renderers = new ArrayList<>();

    float TWO_PI = (float) (Math.PI * 2);
    float slices = 17; // variable amount of slices
    float kickCounter = 0;

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(20); //ms time of each tick
        rc.getInternalClock().start();

        //set up the RC
        rc.setRendererClass(GranularRenderer.class);

        String computerName = null;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula_km.csv");
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,0,200,0,"treble sounds",0);
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,0,0,0,"bass sounds",1);

        SampleManager.group("drums","data/audio/Mattel_Drum_Machine");

        //Assign each renderer a GenericSampleAndClockRenderer
        rc.renderers.forEach(r -> {
            renderers.add((GranularRenderer) r);
            ((GranularRenderer) r).useRegularSamplePlayer();
            ((GranularRenderer) r).gain(1f);
        });

        rc.addClockTickListener((v, clock) -> {
            rc.sendSerialcommand();
        });

        //this calls any function from an OSC name
        new OSCUDPListener(5000) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                try {
                    //System.out.println(oscMessage.getName());
                    String methodName = oscMessage.getName().substring(1);
                    Method m = JulianCasula.class.getMethod(methodName, OSCMessage.class);
                    m.invoke(JulianCasula.this, oscMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void kickSound (OSCMessage oscMessage){
        renderers.forEach(r -> {
            if (r.y < 100) { // pass to bass speakers
                r.setSample(SampleManager.fromGroup("drums", 0));
                r.position(0);
            }

            //Each angleHeight[0] is the renderers angle between 0-TWO_PI or 0-360 degrees
            float[] angleHeight = mapCartesianToCylinder(r);
            if ((angleHeight[0] / TWO_PI) > kickCounter / slices && (angleHeight[0] / TWO_PI) < (kickCounter + 1) / slices) {
                r.brightness(1f);
                r.setRGB(255, 255, 0);
                r.decay(0.9f);
            }
        });

        kickCounter++;
        if (kickCounter >= slices) {
            kickCounter = 0;
        }
    }

    public void snareSound (OSCMessage oscMessage) {
        renderers.forEach(r -> {
            if (r.y > 100) { // pass to treble speakers
                r.setSample(SampleManager.fromGroup("drums", 6));
                r.position(0);
            }
        });
    }

    public void snareLight (OSCMessage oscMessage) {
        renderers.forEach(r -> {
            if (r.csvData.containsKey("lightRing")) {
                if ((Integer.parseInt(r.csvData.get("lightRing"))) == (int) oscMessage.getArg(0)) {

                    r.brightness(1f);
                    r.setRGB(255, 0, 255);
                    r.decay(0.9f);
                }
            }
        });
    }

    public void synthNote (OSCMessage oscMessage){

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

    //utility function for getting angles
    Map<Renderer, float[]> cachedAngles = new HashMap<>();
    float[] mapCartesianToCylinder(Renderer r) {
        if(!cachedAngles.containsKey(r)) {
            float[] angleHeight = new float[]{0, 0};    //range is [0,2PI] for angle (clockwise when looking above) and [0,1] for height (going up)
            float xnorm = r.x - 214f;
            float ynorm = r.z - 214f;
            angleHeight[0] = (float) Math.atan(xnorm / ynorm);
            if(ynorm < 0) {
                angleHeight[0] += (float)Math.PI;
            } else if(xnorm < 0) {
                angleHeight[0] += TWO_PI;
            }
            angleHeight[1] = r.y / 193f;
            cachedAngles.put(r, angleHeight);
        }
        return cachedAngles.get(r);
    }
}
