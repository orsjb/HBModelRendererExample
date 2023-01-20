package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.HBCommand;
import net.happybrackets.rendererengine.Renderer;
import net.happybrackets.rendererengine.RendererController;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class processing_text extends Renderer {

    public static String installationConfig = "config/hardware_setup_casula_km_test.csv";
    public static int oscPort = 4444;
    public static RendererController.RenderMode renderMode = RendererController.RenderMode.UNITY;
    public static int clockInterval = 20;

    float TWO_PI = (float) (Math.PI * 2);

    int[] myColor = new int[]{0,0,0};

    @HBCommand
    public void position(OSCMessage oscMessage){ //sinefield
        int i = (int) oscMessage.getArg(0);
        int j = (int) oscMessage.getArg(1);
//        System.out.println(i + " " +  j);
        System.out.println(csvData.get("lightRingID"));

        if((i == Integer.parseInt(csvData.get("lightRingID")) && j == Integer.parseInt(csvData.get("lightRing")))){
            myColor[0] = 255;
            myColor[1] = 255;
            myColor[2] = 255;
        }
    }

    @Override
    public void setupAudio() {
    }


    @Override
    public void setupLight() {
        colorMode(ColorMode.RGB, 255);
        rc.displayColor(this, 0, 0, 0);
        rc.pushLightColor(this);
    }

    public void tick(Clock clock) {
        displayColor(myColor);

    }

    int[] RGBFromHue(float hue, float saturation, float brightness) {
        int[] RGB = new int[3];
        int c = Color.HSBtoRGB(hue, saturation, brightness);
        RGB[0] = (c >> 16) & 0xFF;
        RGB[1] = (c >> 8) & 0xFF;
        RGB[2] = c & 0xFF;
        return RGB;
    }


    Map<Renderer, float[]> cachedAngles = new HashMap<>();
    float[] mapCartesianToCylinder(Renderer r) {
        if(!cachedAngles.containsKey(r)) {
            float[] angleHeight = new float[]{0, 0};
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
