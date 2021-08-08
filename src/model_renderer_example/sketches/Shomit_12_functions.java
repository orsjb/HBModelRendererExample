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

public class Shomit_12_functions extends Renderer {

    public static String installationConfig = "config/hardware_setup_casula_km_test.csv";
    public static int oscPort = 5555;
    public static RendererController.RenderMode renderMode = RendererController.RenderMode.UNITY;
    public static int clockInterval = 40;
    RendererController rc = RendererController.getInstance();

    final float TWO_PI = (float) (Math.PI * 2); //use this to divide everything up
    int slices = 7;
    int rows  = 3;

    float[] sliceCentre = new float[slices];
    float[] rowCentre = new float[rows];
    float sliceRange = TWO_PI / slices;
    float rowRange = 1f/rows;

    boolean finishedSetup = false;

    float currentTime = 0f;

    SamplePlayer sp;
    Gain g;
    Sample s = SampleManager.sample("data/audio/long/1979.wav");


    @Override
    public void setupLight() {
        colorMode(ColorMode.RGB, 255);
        rc.displayColor(this, 0, 0, 0);
        rc.pushLightColor(this);
        regionSetup();
        finishedSetup = true;
    }

    public void tick(Clock clock) {
        float time = clock.getNumberTicks() * 0.001f % TWO_PI;
        rotate(time);
//        regionSetup();

        int[] sliceRow = mapSliceRow(this);
        float hue = ((float)sliceRow[0]/slices);
        displayColor(RGBFromHue(hue,0.95f,0.95f));
//        displayColor(255 / slices * sliceRow[0],0,0);
//        hb.setStatus(" " + (float)(sliceRow[0] / slices) * 0.5f);


//        if(finishedSetup) {
//            if (rc.renderers.get(146) == this)
//            {
//                int[] sliceRow = mapSliceRow(this);
//                hb.setStatus(" my slice = " + sliceRow[0] + " " + sliceRow[1]);
////            hb.setStatus(" " + sliceCentre[2] + " " + rowCentre.length);
//            }
//        }
    }

    private void regionSetup(){
        for (int i = 0; i < slices; i++) {
            sliceCentre[i] = TWO_PI * ((float)i/slices);
        }

        for (int i = 0; i < rows; i++) {
            rowCentre[i] = (float)i/rows;
        }
    }

    private void rotate(float increment){
        for (int i = 0; i < sliceCentre.length; i++) {
            sliceCentre[i] += increment % TWO_PI;
//            hb.setStatus(sliceCentre[1] + "");
        }
    }


    Map<Renderer, int[]> cachedRegion = new HashMap<>();
    int[] mapSliceRow(Renderer r) {
        float thisTime = rc.getInternalClock().getNumberTicks()* 0.001f % TWO_PI;
        float thisAngleHeight[] = mapCartesianToCylinder(this);
        if(thisTime != currentTime) {
            int[] sliceRow = new int[]{0, 0};    //range is [0,2PI] for angle (clockwise when looking above) and [0,1] for height (going up)
            for (int i = 0; i < sliceCentre.length; i++) {
                float distanceToSliceCentre = Math.abs(sliceCentre[i] - thisAngleHeight[0]);
                if(distanceToSliceCentre < sliceRange * 0.5f){
                    sliceRow[0] = i;
                }
            }
            for (int i = 0; i < rowCentre.length; i++) {
                float distanceToRowCenter = Math.abs(rowCentre[i] - thisAngleHeight[1]);
                if(distanceToRowCenter < rowRange){
                    sliceRow[1] = i;
                }
            }
            if(!cachedRegion.containsKey(r)) {
                cachedRegion.put(r, sliceRow);
            } else {
                cachedRegion.replace(r, sliceRow);
            }
            currentTime = thisTime;

        }
        return cachedRegion.get(r);
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
