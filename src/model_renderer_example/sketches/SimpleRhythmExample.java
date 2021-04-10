package model_renderer_example.sketches;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.SampleManager;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.control.ControlScope;
import net.happybrackets.core.control.DynamicControl;
import net.happybrackets.core.control.FloatControl;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.*;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class SimpleRhythmExample implements HBAction {

    RendererController rc = RendererController.getInstance();
    HB hb;

    void setupDeviceLocations() {
        //TODO you should add new lines for each device and give the device a position
        //rc.addRenderer(OutputRenderer.Type.SPEAKER, "hb-b827eb999a03",0,0, 0,"hb-b827eb999a03", 0);


        //everything below is for the IML setup
        //we populate a data structure with all of the device positions
        //then below that we grab the positions corresponding to our own hostname
        //and set up the OutputRenderer with this device name and position.
        //you probably want to do something similar. Just edit the lines below with the device names
        //and positions of your devices (assumes fixed positions!).

        Map<String, float[]> positions = new HashMap<>();
        positions.put("hb-b827ebe529a6", new float[] {9,10, 0});
        positions.put("hb-b827ebe6f198", new float[] {11,4.5f, 0});
        positions.put("hb-b827ebc11b4a", new float[] {7.5f,5, 0});
        positions.put("hb-b827ebc165c5", new float[] {4,5.5f, 0});
        positions.put("hb-b827eb15dc82", new float[] {7,3, 0});
        positions.put("hb-b827eb1a0c8d", new float[] {11,6.5f, 0});
        positions.put("hb-b827eb01d68a", new float[] {1,2, 0});
        positions.put("hb-b827eb24587c", new float[] {2.5f,4, 0});
        positions.put("hb-b827ebc7d478", new float[] {10.5f,3, 0});
        positions.put("hb-b827eb118819", new float[] {10.5f,3, 0});
        positions.put("hb-b827eb24fc91", new float[] {2,6, 0});
        positions.put("hb-b827ebb507fd", new float[] {1,4, 0});
        positions.put("hb-b827eb824c81", new float[] {11,11, 0});
        positions.put("hb-b827eb4635ff", new float[] {8.5f,3, 0});
        positions.put("hb-b827eb6cb1f8", new float[] {10,5, 0});
        positions.put("hb-b827eb9089ee", new float[] {8,8, 0});
        positions.put("hb-b827ebf55d4d", new float[] {8,10, 0});
        positions.put("hb-b827eb3d046a", new float[] {9,4, 0});
        positions.put("hb-b827eb945f3f", new float[] {9,9.5f, 0});
        positions.put("hb-b827ebbf17a8", new float[] {11,4.5f, 0});
        positions.put("hb-b827ebeccfab", new float[] {8.5f,6, 0});
        positions.put("hb-b827eb7561a0", new float[] {9,6, 0});
        positions.put("hb-b827eb8221a3", new float[] {10.5f,8, 0});

        String myHostname = "";
        try {
            myHostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        float[] position = positions.get(myHostname);
        rc.addRenderer(Renderer.Type.SPEAKER, myHostname, position[0], position[1], position[2], "SPEAKER", 0);
        positions.clear();

    }

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().setInterval(50);
        //adding some samples
        //set up the RC
        rc.setRendererClass(GenericSampleAndClockRenderer.class);
        //set up the configuration of the system
        setupDeviceLocations();
        //some basic configuration
        rc.renderers.forEach(renderer -> {
            GenericSampleAndClockRenderer r = (GenericSampleAndClockRenderer)renderer;
            r.gain(1);
            r.setSample(SampleManager.sample("data/audio/Nylon_Guitar/Clean_G_harm.wav"));
            r.clockInterval(20);
            r.clockDelay(0);
            r.useGranularSamplePlayer();
        });
        //control to change the phase
        FloatControl floatControl = new FloatControl(this, "phase difference", 0) {
            @Override
            public void valueChanged(double control_val) {// Write your DynamicControl code below this line
                rc.renderers.forEach(renderer -> {
                    //if control_val is zero, all are in phase
                    //the delay is determined by the control_val times the x pos
                    GenericSampleAndClockRenderer r = (GenericSampleAndClockRenderer)renderer;
                    r.clockDelay((int)(r.x * control_val));
                });
            }
        }.setDisplayRange(0, 1, DynamicControl.DISPLAY_TYPE.DISPLAY_DEFAULT);// End DynamicControl floatControl code
        floatControl.setControlScope(ControlScope.GLOBAL);

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
