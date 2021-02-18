package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.core.control.BooleanControl;
import net.happybrackets.core.control.DynamicControl;
import net.happybrackets.core.control.FloatControl;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.RendererController;


import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static java.lang.StrictMath.abs;

/**
 * This example moves blobs around the Unity simulation of the Casula model in a circular orbit on a 2D plane
 * You can use the internal clock, or an OSC listener
 * The internal clock has Dynamic Controls
 */


public class OrbitExample implements HBAction, HBReset {

    final float TWO_PI = (float)Math.PI * 2;
    final int PORT = 4000; //the OSC listening port

    double orbitTime = 100; //amount of ticks for each revolution
    double blobSize = 150; //the distance threshold ~ each ring is about 20 apart
    double radius = 70; // innermost circle is radius ~70, outermost ~220
    float offset = 214; // Move the origin of the parametric equation below to be at 214,214 instead of 0,0

    int direction = 1;

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

        //For unity, send this sketch to the HB simulator
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv");

        //Assign each renderer a GenericSampleAndClockRenderer
        rc.renderers.forEach(renderer -> {
            renderers.add((GenericSampleAndClockRenderer) renderer);
        });

        // this block of code animates a blob rotating around r.x and r.z
        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r -> {

                r.decay(0.97f);

                //time as a loop going from 0-TWO_PI.
                float t = 0;

                //normalise t from 0-orbitTime to 0-TWO_PI
                t = clock.getNumberTicks() % (float) orbitTime / (float)orbitTime* TWO_PI * direction;

                double xCoord = radius * Math.cos(t) + offset; // x coordinate of a point on a circle
                double yCoord = radius * Math.sin(t) + offset; // y coordinate of a point on a circle

                float dist = (distance(r.x, r.z, (float) xCoord, (float) yCoord)); //use utility functions below to calculate distance
                if  (dist < blobSize) {
                    int kindOfBlue = (int)(r.y * 2f); //vary colour on position

                    r.brightness(1f);//only turn the brightness from the if
                    r.setRGB(0, 200, kindOfBlue); //change to this colour if dist
                }
            });
            rc.sendSerialcommand();
        });

        // parameters for the blob rotating around the sim
        FloatControl orbiteBuddyControl = new FloatControl(this, "Orbit speed", 100) {
            @Override
            public void valueChanged(double control_val) {/* Write your DynamicControl code below this line */
                orbitTime = control_val;         }
        }.setDisplayRange(0, 666, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY);
        FloatControl blobSizeBuddyControl = new FloatControl(this, "Blob size", 150) {
            @Override
            public void valueChanged(double control_val) {/* Write your DynamicControl code below this line */
                blobSize = control_val;         }
        }.setDisplayRange(1, 200, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY);
        FloatControl radiusBuddyControl = new FloatControl(this, "Radius", 72) {
            @Override
            public void valueChanged(double control_val) {/* Write your DynamicControl code below this line */
                radius = control_val;         }
        }.setDisplayRange(50, 250, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY);
        BooleanControl booleanControl = new BooleanControl(this, "Clockwise / anti-clockwise", true) {
            @Override
            public void valueChanged(Boolean control_val) {// Write your DynamicControl code below this line
                if (control_val){
                    direction = 1;
                } else {
                    direction = -1;
                }

            }
        };// End DynamicControl booleanControl code

           //generic OSC listener - for any message, e.g., "/sqk" it tries to call the function, e.g., "sqk".
        new OSCUDPListener(PORT) {
        @Override
        public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
            try {
                //System.out.println(oscMessage.getName());
                String methodName = oscMessage.getName().substring(1);
                Method m = OrbitExample.class.getMethod(methodName, OSCMessage.class);
                m.invoke(OrbitExample.this, oscMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
// You can change the blob with an osc message
    public void orbit(OSCMessage oscMessage) {

        float t = (float) oscMessage.getArg(0); //counting to TWO_PI
        float blob = (float) oscMessage.getArg(1); //if dist is less than this..
        float radius = (float) oscMessage.getArg(2); // innermost circle is radius around 70, outermost 220
        float offset = 214; // Move the origin of the parametric equation below to be at 214,214 instead of 0,0

        double xCoord = radius * Math.cos(t) + offset; // x coordinate of a point on a circle
        double yCoord = radius * Math.sin(t) + offset; // y coordinate of a point on a circle

        renderers.forEach(r -> {
            float dist = (distance(r.x, r.z, (float) xCoord, (float) yCoord));
            if  (dist < blob) {
                r.setRGB(255, 0, 127);
                r.brightness(1f);
                r.decay(0.99f);
            }
         });
    }

    // OB's utility functions from tmaw -- calculate distance of two 2D coordinates
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
