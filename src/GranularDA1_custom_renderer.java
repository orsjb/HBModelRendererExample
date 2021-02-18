import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.*;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.core.OSCUDPSender;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



//"Cape Cod, November" By Devin Arne
// Sound Installation with interactive gesture controlled input
// Instructions: Load Sample Folder onto as many PIs as possible
// Select one Pi to be 'controller', input its index into the accelerometer section of the code
// Enter the IP addresses of all other Pies in the bottom of the accelerometer section
// Run Patch
// The sounds change based on a code score (lines 438-750 of the code)
// Moving the controller Pi will control playback features including: delay, reverb, delay jitter...
// ...playback speed, and pitch envelope. (see lines 781-897 for details on the 6 main gestures)
// All sounds were recorded in Harwich Port, MA by Devin Arne and are available to used/edited/remixed
//...under Creative Commons licence

public class GranularDA1_custom_renderer implements HBAction {


    class DevinRenderer extends Renderer {

        int index;
        int interval = 20;

        String groupName;
        float pitchInit = 1;
        float pitchFinal = 1;
        float pbSpeed = 1;
        float grainSize = 60;
        float grainInterval = 80;

        GranularSamplePlayer gsp;
        Envelope gainEnv = new Envelope(0);
        Envelope pitchEnv = new Envelope(1);
        Envelope loopStartEnv = new Envelope(0);
        Envelope loopEndEnv = new Envelope(1000);
        Envelope grainSizeEnv = new Envelope(80);
        Envelope grainIntervalEnv = new Envelope(70);

        Map<Integer, String> groupEvents = new HashMap<>();

        @Override
        public void setupAudio() {

            gsp = new GranularSamplePlayer(null);
            gsp.setPitch(pitchEnv);
            gsp.setLoopStart(loopStartEnv);
            gsp.setLoopEnd(loopEndEnv);
            gsp.setGrainSize(grainSizeEnv);
            gsp.setGrainInterval(grainIntervalEnv);
            //attach all the envelopes you created above to the gsp
            Gain g = new Gain(1, gainEnv);
            g.addInput(gsp);
            out.addInput(g);


        }

        public void setRandomIndex() {
            index = hb.rng.nextInt(4);
            switch (index) {
                case 0:
                    index = 20;
                    //TODO populate group events
                    break;
                case 1:
                    index = 40;
                    //TODO populate group events
                    break;
                case 2:
                    index = 30;
                    //TODO populate group events
                    break;
                case 3:
                    index = 50;
                    //TODO populate group events
                    break;
                default:
                    index = 20;
                    //TODO populate group events
                    break;
            }
        }
    }

    float clockinterval = 1000;

    HB hb;
    RendererController rc = RendererController.getInstance();
    List<DevinRenderer> renderers = new ArrayList<>();
    Map<Renderer, Integer> rendererIndices = new HashMap<>();

    //playback & granulation parameters
    int loopfactor = 1;

    //fx parameters
    float fbgain = .5f;
    float fxgain = .7f;
    float magnitude = .9f;
    float delayjitter = 2f;

    //OSC/Accel. params
    float x = 1f;
    float y = 1f;
    float z = 1f;

    OSCUDPSender oscSend = new OSCUDPSender();

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(20); //ms time of each tick
        rc.getInternalClock().start();
        int myIndex = Math.abs(hb.myIndex());
        /** instead of using myIndex, use 'blobs' as positions of renderers.
         * below is renderer stuff for unity, so this works with 200 light renderers
         */
        //set up the RC for unity sim with GenericSampleAndClock
        rc.setRendererClass(DevinRenderer.class);
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv"); //204 light renderers
//        rc.loadHardwareConfiguration("config/hardware_setup_casula.csv"); // for at Casula
        rc.renderers.forEach(renderer -> {
            DevinRenderer dr = (DevinRenderer) renderer;
            renderers.add(dr);
            dr.setRandomIndex();
        });
        //Sample Banks & Clock Timer-------------------------------------------------------------->
        SampleManager.group("pineCone", "data/audio/CapeCodNovemberV2/Pine Cone");
        SampleManager.group("Lichen", "data/audio/CapeCodNovemberV2/Lichen");
        SampleManager.group("bowedSage", "data/audio/CapeCodNovemberV2/Bowed Sage");
        SampleManager.group("resoLichen", "data/audio/CapeCodNovemberV2/Resonator Lichen");
        SampleManager.group("Twigs", "data/audio/CapeCodNovemberV2/Twigs");
        rc.addClockTickListener((offset, this_clock) -> {// Write your code below this line
            int counter = (int) this_clock.getNumberTicks();
            renderers.forEach(renderer -> {
                //By this line, we are in a clock loop, for each renderer
                //blob pattern1
                //------------------------------------------------------------------------------------>
                if (counter % renderer.interval == 0) {
                    Sample pcsample = SampleManager.randomFromGroup(renderer.groupName);
                    float SampleLegnth = (float) pcsample.getLength();
                    renderer.gsp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
                    //Pitch Envelope---------------------------------------
                    renderer.pitchEnv.clear();
                    renderer.pitchEnv.setValue(renderer.pitchInit);
                    renderer.pitchEnv.addSegment(renderer.pitchFinal, SampleLegnth * 2);
                    //TODO use this pattern to control the other envelopes
                    //Granular Parameters---------------------------------------
                    renderer.loopStartEnv.clear();
                    renderer.loopStartEnv.setValue(0);
                    renderer.loopStartEnv.addSegment(SampleLegnth * 0.5f, SampleLegnth * loopfactor);
                    renderer.loopEndEnv.clear();
                    renderer.loopEndEnv.setValue(0);
                    renderer.loopEndEnv.addSegment(SampleLegnth * 0.75f, SampleLegnth * loopfactor);
                    renderer.grainSizeEnv.clear();
                    renderer.grainSizeEnv.setValue(120);
                    renderer.grainSizeEnv.addSegment(renderer.grainSize, SampleLegnth * loopfactor);
                    renderer.grainIntervalEnv.clear();
                    renderer.grainIntervalEnv.setValue(100);
                    renderer.grainIntervalEnv.addSegment(renderer.grainInterval, SampleLegnth * loopfactor);
                    renderer.gsp.getRateUGen().setValue(5f);
                    //Gain and Gain Envelope-------------------------------------
                    renderer.gainEnv.addSegment(0f, 10);
                    renderer.gainEnv.addSegment(0.9f, ((SampleLegnth * loopfactor / 2f)));
                }
                //Variables at Clock Rate ---------------------------------
                //setting the sample group at certain times
                //"Score" ------Continuity--->Discontinuity----->Continuity
                //Buffer Select
                String groupNameTemp = renderer.groupEvents.get(counter);
                if (groupNameTemp != null) renderer.groupName = groupNameTemp;
            });
            //this is not within the renderer loop
            //Set Clock Interval (1s to 1.2s)
            clockinterval = ((hb.rng.nextFloat() * 200) + 1000);      //TODO: check this works
            this_clock.setInterval(clockinterval);
        });


        //OSC Listener-------------------------------------------------------------

        OSCUDPListener oscudpListener = new OSCUDPListener(9001) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long time) {
                // type your code below this line

                //hb.setStatus("message"+ oscMessage.getArg(0)+ oscMessage.getArg(1) + oscMessage.getArg(2));

                x = (float) oscMessage.getArg(0);
                y = (float) oscMessage.getArg(1);
                z = (float) oscMessage.getArg(2);

                //Control Delay Feedback Amount through tilting towards Y  axis
                fbgain = ((Math.abs(y) / 2) + .48f);
                //Control Fx Gain Through Tilting towards Y  axis
                fxgain = ((Math.abs(z) / 2) + .48f);
                //Control Delay Jitter Amount through overall amount of movement of controller Pi
                magnitude = (float) Math.sqrt(Math.pow(x, 2) + Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2)));
                Glide magGlide = new Glide(magnitude, 50);
                float movement = magGlide.getCurrentValue();
                delayjitter = clockinterval / (movement * 4);
                //hb.setStatus("movement"+delayjitter);
                //Define Positive and Negative Thresholds to Change Gesture States
                float acc_threshold = 0.9f;
                float acc_negativethresh = -0.9f;
                //hb.setStatus("test" + x + y + z);
                renderers.forEach(renderer -> {
                    ///Gesture Short and Initial Pitch
                    if (x > acc_threshold) {
                        loopfactor = (1 + (hb.rng.nextInt(2)));
                        renderer.pitchInit = 1f;
                        renderer.pitchFinal = 1f;
                        renderer.pbSpeed = 2f;
                        hb.setStatus("X pos: " + x + y + z);
                        ///Gesture Low To High Pitch Slow Playback Speed
                    } else if (y > acc_threshold) {
                        loopfactor = (3 + (hb.rng.nextInt(4)));
                        renderer.pitchInit = .4f;
                        renderer.pitchFinal = 2f;
                        hb.setStatus("Y pos ");
                        //Gesture Hi to Low Pitch Medium Playback Speed
                    } else if (z > acc_threshold) {
                        loopfactor = (3 + (hb.rng.nextInt(3)));
                        renderer.pitchInit = 1.5f;
                        renderer.pitchFinal = .8f;
                        renderer.pbSpeed = 1.5f;
                        hb.setStatus("Z pos ");
                    }
                    //Gesture: Undulating Pitch, Medium Playback Speed
                    if (x < acc_negativethresh) {
                        loopfactor = (2 + (hb.rng.nextInt(3)));
                        renderer.pitchInit = 1.2f;
                        renderer.pitchFinal = .8f;
                        renderer.pbSpeed = 0.5f;
                        hb.setStatus("X neg ");
                        //Gesture: Undulating High Pitch, Slow Playback Speed
                    } else if (y < acc_negativethresh) {
                        loopfactor = (2 + (hb.rng.nextInt(2)));
                        renderer.pitchInit = 2f;
                        renderer.pitchFinal = 1.4f;
                        renderer.pbSpeed = .25f;
                        hb.setStatus("Y neg");

                        //Gesture: Wobbly Low Pitch, Very Slow Playback Speed
                    } else if (z < acc_negativethresh) {
                        loopfactor = (1 + (hb.rng.nextInt(2)));
                        renderer.pitchInit = .5f;
                        renderer.pitchFinal = .3f;
                        renderer.pbSpeed = .2f;
                        hb.setStatus("Z neg ");
                    }
                });
            }
        };

        if (oscudpListener.getPort() < 0) { //port less than zero is an error
            String error_message = oscudpListener.getLastError();
            System.out.println("Error opening port " + 9001 + " " + error_message);
        } // end oscListener code

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
