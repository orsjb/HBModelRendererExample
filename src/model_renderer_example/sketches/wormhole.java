package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.SampleManager;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class wormhole implements HBAction, HBReset {

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
        String computerName = null;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        rc.addRenderer(Renderer.Type.SPEAKER, computerName, 0, 0, 0, "bass sounds", 1);
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula_km.csv");

        SampleManager.group("drums", "data/audio/Mattel_Drum_Machine");

        //Assign each renderer a GenericSampleAndClockRenderer
        rc.renderers.forEach(renderer -> {
            renderers.add((GenericSampleAndClockRenderer) renderer);
        });


        rc.addClockTickListener((v, clock) -> {
            rc.sendSerialcommand();
        });

        new OSCUDPListener(1234) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                try {
                    //System.out.println(oscMessage.getName());
                    String methodName = oscMessage.getName().substring(1);
                    Method m = wormhole.class.getMethod(methodName, OSCMessage.class);
                    m.invoke(wormhole.this, oscMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void blood_red (OSCMessage oscMessage){ //colour
        hb.setStatus(oscMessage.getName() + " received");

        renderers.forEach(r->{
            r.brightness(1f);
            r.setRGB(255,0,0);
            r.decay(0.99f);
        });
    }

    public void steam (OSCMessage oscMessage){ //flocking
        hb.setStatus(oscMessage.getName() + " received");

            int boids = (oscMessage.getArgCount() - 1) / 3;
            float flockingIntensity = (int)(oscMessage.getArg(oscMessage.getArgCount() - 1)) / 127f;
            renderers.forEach(r -> {
                float flockingIntensityX = flockingIntensity;
                if(flockingIntensityX < 0.25f) {
                    flockingIntensityX = flockingIntensityX / 0.25f;
                    r.decay(flockingIntensityX * 0.3f + 0.7f);
                } else {
                    flockingIntensityX = (flockingIntensityX - 0.25f) / 0.75f;
                    float positionIntensity = 0, velIntensity = 0;
                    for (int i = 0; i < boids; i++) {
                        //position
                        float x = hb.getFloatArg(oscMessage, i * 3 + 0);
                        float y = hb.getFloatArg(oscMessage, i * 3 + 1);
                        float distance = distance(r.x, r.z, x, y) / 608f; //608 is the diagonal of a 430x430 square
                        positionIntensity += Math.max(0.2f - distance, 0) * 5f / boids;
                        float vmag = hb.getFloatArg(oscMessage, i * 3 + 2);
                        velIntensity += vmag * Math.max(0.2f - distance, 0) * 5f / boids;
                    }
                    float overallIntensity = 1 - flockingIntensityX * (1 - positionIntensity);
                    if(overallIntensity > 1) overallIntensity = 1;
                    overallIntensity *= 0.9f;
                    r.gain(overallIntensity);
                    r.brightness(overallIntensity);
//            r.lfoDepth(1 - flockingIntensity * (1 - velIntensity));       //TODO use vel intensity on something
                }


            });
        }

    public void darkness (OSCMessage oscMessage) { //everything black
        hb.setStatus(oscMessage.getName() + " received");

        renderers.forEach(r->{
            r.brightness(0);
            r.setRGB(0,0,0);
            r.decay(0);
        });
    }

    public void sweep_wash (OSCMessage oscMessage) { //step through rings
        hb.setStatus(oscMessage.getName() + "received");

        renderers.forEach(r -> {
            if (r.csvData.containsKey("lightRing")) {
                if ((Integer.parseInt(r.csvData.get("lightRing"))) == (int) oscMessage.getArg(0)) {
                    r.brightness(1f);
                    r.setRGB(255, 255, 255);
                    r.decay(0.9f);
                }
            }
        });
    }
    float meanSquare(float x1, float y1, float x2, float y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }
    float distance(float x1, float y1, float x2, float y2) {
        return (float)Math.sqrt(meanSquare(x1,y1,x2,y2));
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
