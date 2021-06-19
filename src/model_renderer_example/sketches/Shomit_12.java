package model_renderer_example.sketches;

import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.Renderer;
import net.happybrackets.rendererengine.RendererController;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class Shomit_12 extends Renderer {

    public static String installationConfig = "config/hardware_setup_casula_km_test.csv";
    public static int oscPort = 5555;
    public static RendererController.RenderMode renderMode = RendererController.RenderMode.UNITY;
    public static int clockInterval = 40;
    RendererController rc = RendererController.getInstance();

    final float TWO_PI = (float) (Math.PI * 2); //use this to divide everything up

    SamplePlayer sp;
    Gain g;
    Sample s = SampleManager.sample("data/audio/long/1979.wav");

    @Override
    public void setupAudio() {
        sp = new SamplePlayer(1);
        g = new Gain(1, 0f);
        g.addInput(sp);
        out.addInput(g);
    }

    //NOTE^^^^ you have 100 SamplePlayers in this simulator, I'm only turning the gain up the ones in a particular region.

    @Override
    public void setupLight() {
        colorMode(ColorMode.RGB, 255);
        rc.displayColor(this, 0, 0, 0);
        rc.pushLightColor(this);
    }

    public void tick(Clock clock) {
        float angleHeight[] = mapCartesianToCylinder(this); //angleHeight[0] is the light or speakers angle from 0 -> TWO_PI. angleHeight[1] is the light or speakers y positions from 0-1
        float aBeat = clock.getNumberTicks() % 100;

        if(angleHeight[0] < TWO_PI * 0.25 && angleHeight[0] > 0f) { //everything in this if is quadrant one
            float quadrantHue = 0f;  //every quadrant has the same hue...

            if(angleHeight[1] < 0.25){  //every row a different saturation
                int myRGB[] = RGBFromHue(quadrantHue,1,1 - (aBeat * 0.01f));
                displayColor(myRGB);//every renderer in this region displays the array above.
            }

            if(angleHeight[1] > 0.25 && angleHeight[1] < 0.5){ //everything here in the second lowest ring etc...
                int myRGB[] = RGBFromHue(quadrantHue,0.75f,1 - (aBeat * 0.01f));
                displayColor(myRGB);

                //each if statement is a section inside a quadrant.


            }

            if(angleHeight[1] > 0.5 && angleHeight[1] < 0.75){
                int myRGB[] = RGBFromHue(quadrantHue,0.5f,1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if(angleHeight[1] > 0.75 && angleHeight[1] < 1){
                int myRGB[] = RGBFromHue(quadrantHue,0.25f,1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

        }

        if(angleHeight[0] > TWO_PI * 0.25 && angleHeight[0] < TWO_PI * 0.5) { //this is the second quadrant
            float quadrantHue = 0.25f;

            if(angleHeight[1] < 0.25){
                int myRGB[] = RGBFromHue(quadrantHue,1,1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if(angleHeight[1] > 0.25 && angleHeight[1] < 0.5){
                int myRGB[] = RGBFromHue(quadrantHue,0.75f,1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if(angleHeight[1] > 0.5 && angleHeight[1] < 0.75){
                displayColor(0,255,0); //this particular section make a different colour for this region
            }

            if(angleHeight[1] > 0.75 && angleHeight[1] < 1){
                int myRGB[] = RGBFromHue(quadrantHue,0.25f,1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }
        }

        if(angleHeight[0] > TWO_PI * 0.5 && angleHeight[0] < TWO_PI * 0.75) {
            float quadrantHue = 0.5f;

            if (angleHeight[1] < 0.25) {
                int myRGB[] = RGBFromHue(quadrantHue, 1, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if (angleHeight[1] > 0.25 && angleHeight[1] < 0.5) { //this section
                if(this.type == Type.SPEAKER) {
                    sp.setSample(s);
                    g.setGain(aBeat * 0.005f);
                }
                int myRGB[] = RGBFromHue(quadrantHue, 0.75f, aBeat * 0.005f);
                displayColor(myRGB);
            }

            if (angleHeight[1] > 0.5 && angleHeight[1] < 0.75) {
                int myRGB[] = RGBFromHue(quadrantHue, 0.5f, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if (angleHeight[1] > 0.75 && angleHeight[1] < 1) {
                int myRGB[] = RGBFromHue(quadrantHue, 0.25f, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }
        }

        if(angleHeight[0] > TWO_PI * 0.75 && angleHeight[0] < TWO_PI) {
            float quadrantHue = 0.75f;

            if (angleHeight[1] < 0.25) {
                int myRGB[] = RGBFromHue(quadrantHue, 1, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if (angleHeight[1] > 0.25 && angleHeight[1] < 0.5) {
                int myRGB[] = RGBFromHue(quadrantHue, 0.75f, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if (angleHeight[1] > 0.5 && angleHeight[1] < 0.75) {
                int myRGB[] = RGBFromHue(quadrantHue, 0.5f, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }

            if (angleHeight[1] > 0.75 && angleHeight[1] < 1) {
                int myRGB[] = RGBFromHue(quadrantHue, 0.25f, 1 - (aBeat * 0.01f));
                displayColor(myRGB);
            }
        }

    }

    int[] RGBFromHue(float hue, float saturation, float brightness) { //give this function a hue to get an array representing RGB values at max saturation and brightness
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
