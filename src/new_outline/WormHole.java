package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import fi.iki.elonen.NanoHTTPD;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WormHole implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GenericSampleAndClockRenderer> renderers = new ArrayList<>();
    Map<Renderer, float[]> cachedAngles = new HashMap<>();

    float TWO_PI = (float) (Math.PI * 2);
    float slices = 5;

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
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv");

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
                    Method m = WormHole.class.getMethod(methodName, OSCMessage.class);
                    m.invoke(WormHole.this, oscMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void bloodRed (OSCMessage oscMessage){ //colour
        hb.setStatus(oscMessage.getName() + " received");

        renderers.forEach(r->{
            r.brightness(0.9f);
            r.setRGB(255,0,0);
            r.decay(0.99f);
        });
    }

    public void gradientController (OSCMessage oscMessage){ //colour
        hb.setStatus(oscMessage.getName() + " received");
        int RGB1[] = RGBFromHue((Float) oscMessage.getArg(0));
        int RGB2[] = RGBFromHue((Float) oscMessage.getArg(1));
        float heightMod = (float) oscMessage.getArg(2);

        renderers.forEach(r -> {
            float angleHeight[] = mapCartesianToCylinder(r);
            int gradientRGB[] = gradientTwoColours((float) (angleHeight[1] + heightMod),RGB1[0],RGB1[1],RGB1[2],RGB2[0],RGB2[1],RGB2[2]);

            r.setRGB(gradientRGB[0],gradientRGB[1],gradientRGB[2]);
            r.brightness(1f);
            r.decay(1f);
        });
    }

    public void steamOrbit (OSCMessage oscMessage){
        float anglePos = (float) oscMessage.getArg(0);
        float heightPos = (float) oscMessage.getArg(1);
        float staticVolume = (float) oscMessage.getArg(2);
        float bandDepth = (float) oscMessage.getArg(3);

        hb.setStatus(oscMessage.getName() + " received. anglePos = " + anglePos);
        renderers.forEach(r->{
            float[] angleHeight = mapCartesianToCylinder(r);
            if (r.z < anglePos + 0.1 && r.z > anglePos - 30)
                if (angleHeight[1] < heightPos + 0.1 && angleHeight[1] > heightPos - bandDepth)
            r.brightness(staticVolume);
        });
    }

    public void steamAnimation (OSCMessage oscMessage){
        float heightMarker = (float)oscMessage.getArg(0);
        float anglePos = (float)oscMessage.getArg(1);

        hb.setStatus(oscMessage.getName() + " received. anglePos = " + anglePos);

        renderers.forEach(r -> {
            float[] angleHeight = mapCartesianToCylinder(r);
            if(angleHeight[1] < heightMarker && angleHeight[1] > heightMarker - 0.2f)
                if(angleHeight[0] > anglePos - 0.45f && angleHeight[0] < anglePos + 0.45f)
                r.brightness(0.8f);
        });
    }

    public void colourOn (OSCMessage oscMessage) {
            int red = (int) oscMessage.getArg(0);
            int green = (int) oscMessage.getArg(1);
            int blue = (int) oscMessage.getArg(2);
            renderers.forEach(r->{
                r.setRGB(red,green,blue);
                r.brightness(1f);
                r.decay(1f);
                rc.sendSerialcommand();
            });
    }

    public void colourOff (OSCMessage oscMessage) {
        renderers.forEach(r -> {
            r.decay(0.98f);
        });
        rc.sendSerialcommand();
    }

    public void m4lBrightness (OSCMessage oscMessage) {
        renderers.forEach(r -> {
            r.brightness((Float) oscMessage.getArg(0));

        });
        rc.sendSerialcommand();
    }

    public void m4lDecay (OSCMessage oscMessage) {
        renderers.forEach(r -> {
            r.decay((Float) oscMessage.getArg(0));
        });
        rc.sendSerialcommand();
    }

    public void sweepWash (OSCMessage oscMessage){
            hb.setStatus(oscMessage.getName() + " received " + oscMessage.getArg(0));
            int red = (int) oscMessage.getArg(1);
            int green = (int) oscMessage.getArg(2);
            int blue = (int) oscMessage.getArg(3);

            float heightMarker = (float) oscMessage.getArg(0);

        renderers.forEach(r -> {
            float[] angleHeight = mapCartesianToCylinder(r);

            if(angleHeight[1] < heightMarker && angleHeight[1] > heightMarker - 0.1f) {
                r.brightness(0.8f);
                r.setRGB(red, green, blue);
                r.decay(0.97f);
            }
        });
    }

    int[] RGBFromHue(float hue){
        int[] RGB = new int[3];
        int c = Color.HSBtoRGB(hue,1,1);
        RGB[0] = (c >> 16) & 0xFF;
        RGB[1] = (c >> 8) & 0xFF;
        RGB[2] = c & 0xFF;
        return RGB;

    }
    int[] gradientTwoColours(float x, int red1, int blue1, int green1, int red2, int blue2, int green2){
        int[] gradientRGB = new int[]{0,0,0};
        gradientRGB[0] = (int) (red1 * (1 - x) + red2 * x);
        gradientRGB[1] = (int) (green1 * (1 - x) + green2 * x);
        gradientRGB[2] =(int) (blue1 * (1 - x) + blue2 * x);
        return gradientRGB;
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
