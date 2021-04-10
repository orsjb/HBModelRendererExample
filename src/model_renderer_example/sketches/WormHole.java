package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.SampleManager;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.GranularRenderer;
import net.happybrackets.rendererengine.Renderer;
import net.happybrackets.rendererengine.RendererController;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WormHole implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GranularRenderer> renderers = new ArrayList<>();
    Map<Renderer, float[]> cachedAngles = new HashMap<>();

    float piT = 0;
    float normalT = -1;
    float brightnessDial = 0;
    float decayDial = 1;

    float normalOffset0 = -1;
    float normalOffset1 = -1;
    float normalOffset2 = -1;
    float normalOffset3 = -1;


    float TWO_PI = (float) (Math.PI * 2);

    float slices = 5;
    float sliceSize = TWO_PI / slices;

    float initialSlice = -1;

    float initialSlice0 = -1;
    float initialSlice1 = -1;
    float initialSlice2 = -1;
    float initialSlice3 = -1;




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

        //For unity, use the HB simulator, send this code to the HB simulator
        String computerName = null;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        rc.loadHardwareConfiguration("config/hardware_setup_casula.csv");

        //Assign each renderer a GenericSampleAndClockRenderer
        rc.renderers.forEach(renderer -> {
            renderers.add((GranularRenderer) renderer);
        });


        rc.addClockTickListener((v, clock) -> {

            float normalT0 = normalT + normalOffset0;
            float normalT1 = normalT + normalOffset1;
            float normalT2 = normalT + normalOffset2;
            float normalT3 = normalT + normalOffset3;

            renderers.forEach(r->{

                float[] angleHeight = mapCartesianToCylinder(r);
                if(angleHeight[1] > normalT - 0.1 && angleHeight[1] < normalT + 0.1 * 2) {
                    if (angleHeight[0] > initialSlice && angleHeight[0] < initialSlice + sliceSize){
                    r.brightness(0.8f);
                    }
                }

                if(angleHeight[1] > normalT0 - 0.1 && angleHeight[1] < normalT0 + 0.1 * 2) {
                    if (angleHeight[0] > initialSlice0 && angleHeight[0] < initialSlice0 + sliceSize){
                        r.brightness(0.8f);
                    }
                }

                if(angleHeight[1] > normalT1 - 0.1 && angleHeight[1] < normalT1 + 0.1 * 2) {
                    if (angleHeight[0] > initialSlice1 && angleHeight[0] < initialSlice1 + sliceSize){
                        r.brightness(0.8f);
                    }
                }

                if(angleHeight[1] > normalT2 - 0.1 && angleHeight[1] < normalT2 + 0.1 * 2) {
                    if (angleHeight[0] > initialSlice2 && angleHeight[0] < initialSlice2 + sliceSize){
                        r.brightness(0.8f);
                    }
                }

                if(angleHeight[1] > normalT3 - 0.1 && angleHeight[1] < normalT3 + 0.1 * 2) {
                    if (angleHeight[0] > initialSlice3 && angleHeight[0] < initialSlice3 + sliceSize){
                        r.brightness(0.8f);
                    }
                }
            });

            rc.sendSerialcommand();
        });

        new OSCUDPListener(1234) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                try {
                    //System.out.println(oscMessage.getName());
                    String methodName = oscMessage.getName().substring(1);
                    Method m = WormHole.class.getMethod(methodName, OSCMessage.class);
                    m.invoke(WormHole.this, oscMessage);
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





    public void steam (OSCMessage oscMessage){
        hb.setStatus(oscMessage.getName() + " received");
        renderers.forEach(r->{
//            r.brightness(1f);
            r.setRGB(255,255,255);
            r.decay(0.99f);
        });
        rc.sendSerialcommand();
    }

    public void steamAnimation (OSCMessage oscMessage){
        hb.setStatus(oscMessage.getName() + " received");

        piT = (float) oscMessage.getArg(0) ;
        normalT = ((float) oscMessage.getArg(1) * 4) - 3;

    }






    public void steamJitter (OSCMessage oscMessage){
        initialSlice = hb.rng.nextFloat() * 6;
        initialSlice0 = hb.rng.nextFloat() * 6;
        initialSlice1 = hb.rng.nextFloat() * 6;
        initialSlice2 = hb.rng.nextFloat() * 6;
        initialSlice3 = hb.rng.nextFloat() * 6;

        normalOffset0 = hb.rng.nextFloat() * 2;
        normalOffset1 = hb.rng.nextFloat() * 2;
        normalOffset2 = hb.rng.nextFloat() * 2;
        normalOffset3 = hb.rng.nextFloat() * 2;

        renderers.forEach(r->{
            float gasJitter = (float) (hb.rng.nextInt(15) * 0.01);
            r.decay(0.85f + gasJitter);
        });
    }

    public void darkness (OSCMessage oscMessage) { //everything black
        hb.setStatus(oscMessage.getName() + " received");

        renderers.forEach(r->{
//            r.brightness(0);
//            r.setRGB(0,0,0);
            r.decay(0.97f);
        });
    }

    public void brightnessDial (OSCMessage oscMessage){
        brightnessDial = (float) oscMessage.getArg(0);
        renderers.forEach(r->{
            r.brightness(brightnessDial);
        });
    }

    public void decayDial (OSCMessage oscMessage){
        decayDial = (float) oscMessage.getArg(0);
    }

    public void sweep_wash (OSCMessage oscMessage) { //step through rings
        hb.setStatus(oscMessage.getName() + " received " + oscMessage.getArg(0));
        int red = (int) oscMessage.getArg(1);
        int green = (int) oscMessage.getArg(2);
        int blue = (int) oscMessage.getArg(3);

        renderers.forEach(r -> {
            if (r.csvData.containsKey("lightRing")) {
                if ((Integer.parseInt(r.csvData.get("lightRing"))) == (int) oscMessage.getArg(0)) {
                    r.brightness(1f);
                    r.setRGB(red, green, blue);
                    r.decay(0.97f);
                }
            }
        });
    }

    public void glow_wash (OSCMessage oscMessage) { //step through rings
        hb.setStatus(oscMessage.getName() + " received " + oscMessage.getArg(0));
        int red = (int) oscMessage.getArg(1);
        int green = (int) oscMessage.getArg(2);
        int blue = (int) oscMessage.getArg(3);

        renderers.forEach(r -> {
            if (r.csvData.containsKey("lightRing")) {
                if ((Integer.parseInt(r.csvData.get("lightRing"))) == (int) oscMessage.getArg(0)) {
                    r.setRGB(red, green, blue);
                    r.decay(0.99f);
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
    float[] mapCartesianToCylinder(Renderer r) {
        if(!cachedAngles.containsKey(r)) {
            float[] angleHeight = new float[]{0, 0};    //range is [0,2PI] for angle (clockwise when looking above) and [0,1] for height (going up)
            float xnorm = r.x - 214f;
            float ynorm = r.z - 214f;
            angleHeight[0] = (float) Math.atan(xnorm / ynorm);
            if(ynorm < 0) {
                angleHeight[0] += (float)Math.PI;
            } else if(xnorm < 0) {
                angleHeight[0] += TWO_PI; // * two Pi.
            }
            angleHeight[1] = r.y / 193f;
            cachedAngles.put(r, angleHeight);
        }
        return cachedAngles.get(r);
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
