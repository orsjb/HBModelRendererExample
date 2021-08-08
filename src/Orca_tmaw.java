package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.HBCommand;
import net.happybrackets.rendererengine.HBParam;
import net.happybrackets.rendererengine.Renderer;
import net.happybrackets.rendererengine.RendererController;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
//An empty template with the utility stuff from the other examples

public class Orca_tmaw extends Renderer {

    public static String installationConfig = "config/hardware_setup_casula_km_test.csv"; //this has 6 speakers pos itioned across the Jams model
    public static int oscPort = 5555;
    public static RendererController.RenderMode renderMode = RendererController.RenderMode.UNITY;
    public static int clockInterval = 20;

    float TWO_PI = (float) (Math.PI * 2);

    //movemment selector
    int m = 0;

    //sqk globals
    float boundX;
    float boundY;
    float boundRadius;
    float myHue;
    float thisGlows;

    //phase globals
    float theSlant;
    float thisDelay = 0;
    float startTime = 0;

    //boids globals
    float b1x;
    float b1y;
    float b2x;
    float b2y;

    //hueCycle globals
    float frequency = 0f;
    float intensity = 0f;
    float hueOffset = 0f;

    float hue;
    float brightness = 0f;
    float saturation = 1f;

    @HBCommand
    public void m(OSCMessage oscMessage){ //movement selector
        m = (int) Math.ceil((int)oscMessage.getArg(0) * 0.25f); // counts 1-16 in orcs bc I can't work out a better way.
    }

    @HBCommand
    public void q(OSCMessage oscMessage){ //receive sqk data
        int arg = 0;
        boundX = TWO_PI * (int)oscMessage.getArg(arg++) / 35f; //rng is scaled 0-35
        boundY = (int)oscMessage.getArg(arg++) / 35f;
        boundRadius = TWO_PI * (int)oscMessage.getArg(arg++) / 35f;
        myHue = (int)oscMessage.getArg(arg++) / 35f;
        thisGlows = 1f;
    }

    @HBCommand
    public void b(OSCMessage oscMessage){ //receive boids data
        int arg = 0;
        b1x = (int)oscMessage.getArg(arg++) / 5f;
        b1y = (int)oscMessage.getArg(arg++) / 10f;
        b2x = (int)oscMessage.getArg(arg++) / 5f;
        b2y = (int)oscMessage.getArg(arg++) / 10f;
    }

    @HBCommand
    public void s(OSCMessage oscMessage){ //recieve slant time
        theSlant = (int) oscMessage.getArg(0) / 35f;
        startTime = rc.getInternalClock().getNumberTicks();
    }

    @HBCommand
    public void l(OSCMessage oscMessage){ //receive hueCycle data
        int theFreq = (int) oscMessage.getArg(0);
        int theIntens = (int) oscMessage.getArg(1);
        int theOffset = (int) oscMessage.getArg(2);

        frequency = theFreq / 35f;
        intensity = theIntens / 35f;
        hueOffset = theOffset / 35f;
    }

    @Override
    public void tick(Clock clock) { //do stuff
        if (m == 1)sqk();
        if (m == 2)phase();
        if (m == 3) {
            saturation = 0f;
            boids();
        }
        if (m == 4) {
            saturation = 1f;
            hueCycle();
        }

        displayColor(RGBFromHue(hue, saturation, brightness));
        decay();
    }

    private void decay() {
        brightness *= 0.97f;
    } //lights fade out each tick

    private void sqk(){ //choop choop

            float[] angleHeight = mapCartesianToCylinder(this);
            if (boundRadius > 0 && cylinderDistSquare(boundX, boundY, angleHeight[0], angleHeight[1]) < boundRadius) {
                hue = myHue;
                brightness = 1f; //flash if in blob

        }
    }

    private void phase(){ //steve in space
        float[] angleHeight = mapCartesianToCylinder(this);
        float halfangle = (angleHeight[0] / 2);         //0-pi
        thisDelay =  (float)Math.abs(Math.sin(halfangle * 2)) * theSlant * 70;
        float currentTime = rc.getInternalClock().getNumberTicks() - startTime;
        if(currentTime > thisDelay && currentTime < thisDelay + 1) {
            hue = 0.420f + (angleHeight[1] * 0.2f);
            brightness = 1f;
        }
    }

    private void boids(){ //birdies
        float angleHeight[] = mapCartesianToCylinder(this);
        float myDistanceFromB1 = distance(angleHeight[0], angleHeight[1], b1x, b1y);
        float myDistanceFromB2 = distance(angleHeight[0], angleHeight[1], b2x, b2y);
        if(myDistanceFromB1 < 0.33f || myDistanceFromB2 < 0.33f){
            float closerTo = Math.min(myDistanceFromB1,myDistanceFromB2);
            brightness = 1 - closerTo * 3;
        }
    }

    private void hueCycle(){ //colours
        float angleHeight[] = mapCartesianToCylinder(this);
        hue = (intensity * (0.5f + 0.5f * (float)Math.sin(angleHeight[0] * frequency)) + hueOffset) % 1;
        brightness = 1f;
    }

    int[] RGBFromHue(float hue,float saturation, float brightness) { //give this function a hue to get an array representing RGB values at max saturation and brightness
        int[] RGB = new int[3];
        int c = Color.HSBtoRGB(hue, saturation, brightness);
        RGB[0] = (c >> 16) & 0xFF;
        RGB[1] = (c >> 8) & 0xFF;
        RGB[2] = c & 0xFF;
        return RGB;
    }

    float meanSquare(float x1, float y1, float x2, float y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    float distance(float x1, float y1, float x2, float y2) {
        return (float)Math.sqrt(meanSquare(x1,y1,x2,y2));
    }

    float cylinderDistSquare(float x1, float y1, float x2, float y2) {     //wraps around the x
        float x_gap = Math.abs(x1 - x2);
        if(x_gap > Math.PI) {
            x_gap = TWO_PI - x_gap;
        }
        return x_gap * x_gap + (y1 - y2) * (y1 - y2);
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