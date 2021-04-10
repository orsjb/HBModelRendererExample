import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.*;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.core.OSCUDPSender;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.device.sensors.AccelerometerListener;
import net.happybrackets.rendererengine.*;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GranularDA1__renderer implements HBAction {

    float clockinterval = 1000;

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GranularRenderer> renderers = new ArrayList<>();
    Map<Renderer, Integer> rendererIndices = new HashMap<>();

    //playback & granulation parameters

    int loopfactor = 1;
    String groupName1 = "Lichen";
    String groupName2 = "Lichen";
    String groupName3 = "Lichen";
    String groupName4 = "Lichen";

    float pitch1init = 1f;
    float pitch2init = 1f;
    float pitch3init = 1f;
    float pitch4init = 1f;

    float pitch1final = 1f;
    float pitch2final = 1f;
    float pitch3final = 1f;
    float pitch4final = 1f;

    float pbspeed1 = .25f;
    float pbspeed2 = 1f;
    float pbspeed3 = 1f;
    float pbspeed4 = 1f;

    float grainsize1 = 60f;
    float graininterval1 = 80f;
    float grainsize2 = 60f;
    float graininterval2 = 80f;
    float grainsize3 = 60f;
    float graininterval3 = 80f;

    //fx parameters

    float fbgain = .5f;
    float fxgain = .7f;
    float magnitude = .9f;
    float delayjitter = 2f;

    //OSC/Accel. params
    float x = 1f;
    float y = 1f;
    float z = 1f;

    //OSCUDPSender oscSend = new OSCUDPSender();

    OSCUDPSender oscSend = new OSCUDPSender();

    @Override
    public void action(HB hb) {

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
        rc.setRendererClass(GranularRenderer.class);
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv"); //204 light renderers
//        rc.loadHardwareConfiguration("config/hardware_setup_casula.csv"); // for at Casula

        rc.renderers.forEach(renderer -> {
            renderers.add((GranularRenderer) renderer);
            rendererIndices.put(renderer, hb.rng.nextInt(4));
        });

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

        /**what can remain from this block? */

        //Setup Effects: Reverb and Master FX Gain
        Reverb reverb = new Reverb();
        Gain verbGain = new Gain(2, fxgain);
        verbGain.addInput(reverb);
        reverb.setSize(0.6f);
        reverb.setDamping(.5f);
        HB.getAudioOutput().addInput(verbGain);

        //Delay
        Envelope tapOutEnvelop = new Envelope(delayjitter);

        TapIn ti = new TapIn(600);
        TapOut to = new TapOut(ti, tapOutEnvelop);
        //Feedback Gain
        Gain feedbackGain = new Gain(1, fbgain);
        feedbackGain.addInput(to);
        ti.addInput(feedbackGain);
        //Send Delay To FX Gain Control
        verbGain.addInput(to);


        /** this block can be replaced with an Ableton osc sender broadcasting to all;
         */

        //Accelerometer-----------Enter The Index of the Pi which you wish to use as the controller
       /* if (myIndex == 2) {
            // type accelerometerSensor to create this. Values typically range from -1 to + 1
            new AccelerometerListener(hb) {
                @Override
                public void sensorUpdated(float x_val, float y_val, float z_val) { // Write your code below this line

                    //data smoothing
                    Glide xGlide = new Glide(x_val, 50);
                    float x = xGlide.getCurrentValue();

                    Glide yGlide = new Glide(y_val, 50);
                    float y = yGlide.getCurrentValue();

                    Glide zGlide = new Glide(z_val, 50);
                    float z = zGlide.getCurrentValue();



                    //enter the IP Addresses of all other Pis Here
                    oscSend.send(HB.createOSCMessage("/Side", x, y, z), "192.168.1.101", 9001);
                    oscSend.send(HB.createOSCMessage("/Side", x, y, z), "192.168.1.102", 9001);
                    oscSend.send(HB.createOSCMessage("/Side", x, y, z), "192.168.1.103", 9001);
                    oscSend.send(HB.createOSCMessage("/Side", x, y, z), "192.168.1.104", 9001);
                    oscSend.send(HB.createOSCMessage("/Side", x, y, z), "192.168.1.100", 9001);
                }
           };//  End accelerometerSensor
    }
    */


        //Sample Banks & Clock Timer-------------------------------------------------------------->

        SampleManager.group("pineCone", "data/audio/CapeCodNovemberV2/Pine Cone");
        SampleManager.group("Lichen", "data/audio/CapeCodNovemberV2/Lichen");
        SampleManager.group("bowedSage", "data/audio/CapeCodNovemberV2/Bowed Sage");
        SampleManager.group("resoLichen", "data/audio/CapeCodNovemberV2/Resonator Lichen");
        SampleManager.group("Twigs", "data/audio/CapeCodNovemberV2/Twigs");

        rc.addClockTickListener((offset, this_clock) -> {// Write your code below this line


            int counter = (int) this_clock.getNumberTicks();

            rc.renderers.forEach(renderer -> {

                //By this line, we are in a clock loop, for each renderer

                int rendererIndex = rendererIndices.get(renderer);
                GranularRenderer myRenderer = (GranularRenderer)renderer;

                //blob pattern1

                //------------------------------------------------------------------------------------>

                if (rendererIndex == 0) {

                    if (counter % 8 == 0) {

                        Sample pcsample = SampleManager.randomFromGroup(groupName1);
                        float SampleLegnth = (float) pcsample.getLength();
//                        GranularSamplePlayer gs1 = new GranularSamplePlayer(pcsample);

                        //Pitch Envelope---------------------------------------
//                        Envelope pitchEnv = new Envelope(pitch1init);
//                        pitchEnv.addSegment(pitch1final, SampleLegnth * 2);

                        myRenderer.loopType(SamplePlayer.LoopType.LOOP_FORWARDS);
//                        myRenderer.pitch(pitchEnv);       //TODO fix pitch envelope idea

                        //Granular Parameters---------------------------------------
//                        Envelope loopStart = new Envelope(0f);
//                        loopStart.addSegment(SampleLegnth * 0.5f, SampleLegnth * loopfactor);
//                        myRenderer.loopStart(loopStart); //TODO fix loop start envelope idea

//                        Envelope loopEnd = new Envelope(0f);
//                        loopEnd.addSegment(SampleLegnth * 0.75f, SampleLegnth * loopfactor);
//                        myRenderer.loopEnd(loopEnd); //TODO fix loop end envelope idea

//                        Envelope grainsize = new Envelope(120);
//                        grainsize.addSegment(grainsize1, SampleLegnth * loopfactor);
//                        myRenderer.grainOverlap(grainsize); //TODO fix grain size envelope idea

//                        Envelope grainInterval = new Envelope(100);
//                        grainInterval.addSegment(graininterval1, SampleLegnth * loopfactor);
//                        gs1.setGrainInterval(grainInterval);

                        myRenderer.rate(5f);

                        //Gain and Gain Envelope-------------------------------------

                        Envelope gainEnvelope = new Envelope();
                        Gain gain = new Gain(2, gainEnvelope);

                        gainEnvelope.addSegment(0f, 10);
                        gainEnvelope.addSegment(0.9f, ((SampleLegnth * loopfactor / 2f)));
                        //Main and FX output
                        reverb.addInput(gain);
                        ti.addInput(gain);
                        hb.sound(gain);

                        gainEnvelope.addSegment(0, ((SampleLegnth * loopfactor / 2f)), new KillTrigger(gain));
                    }
                }

                //------------------------------------------------------------------------------------


                if (rendererIndex == 1) {

                    if (counter % 10 == 0) {

                        Sample pcsample2 = SampleManager.randomFromGroup(groupName2);
                        float pcSampleLegnth2 = (float) pcsample2.getLength();

                        GranularSamplePlayer gs2 = new GranularSamplePlayer(pcsample2);

                        Envelope pitchEnv = new Envelope(pitch2init);
                        gs2.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
                        gs2.setPitch(pitchEnv);
                        pitchEnv.addSegment(pitch2final, pcSampleLegnth2);

                        gs2.getRateUGen().setValue(pbspeed2);

                        Envelope loopStart2 = new Envelope(0f);
                        loopStart2.addSegment(pcSampleLegnth2 * 0.5f, pcSampleLegnth2 * loopfactor);
                        gs2.setLoopStart(loopStart2);

                        Envelope loopEnd2 = new Envelope(0f);
                        loopEnd2.addSegment(pcSampleLegnth2 * 0.75f, pcSampleLegnth2 * loopfactor);
                        gs2.setLoopEnd(loopEnd2);

                        Envelope grainsize = new Envelope(100);
                        grainsize.addSegment(grainsize2, pcSampleLegnth2 * loopfactor);
                        //grainsize2.addSegment(100, pcSampleLegnth2);

                        gs2.setGrainSize(grainsize);

                        Envelope grainInterval2 = new Envelope(100);
                        grainInterval2.addSegment(graininterval2, pcSampleLegnth2 * loopfactor);
                        gs2.setGrainInterval(grainInterval2);

                        //Gain and Gain Envelope
                        Envelope gainEnvelope = new Envelope();
                        gainEnvelope.addSegment(0f, 10);
                        gainEnvelope.addSegment(0.9f, (clockinterval * loopfactor / 2f));

                        Gain gain2 = new Gain(2, gainEnvelope);
                        gainEnvelope.addSegment(0, (clockinterval * loopfactor / 2f), new KillTrigger(gain2));

                        //Main and FX output
                        gain2.addInput(gs2);
                        reverb.addInput(gain2);
                        ti.addInput(gain2);
                        hb.sound(gain2);

                    }
                }

                //------------------------------------------------------------------------------------


                if (rendererIndex == 2) {

                    if (counter % 9 == 0) {
                        Sample pcsample3 = SampleManager.randomFromGroup(groupName3);

                        float SampleLegnth = (float) pcsample3.getLength();

                        GranularSamplePlayer gs3 = new GranularSamplePlayer(pcsample3);

                        //Pitch Envelope---------------------------------------
                        Envelope pitchEnv = new Envelope(pitch3init);
                        pitchEnv.addSegment(pitch3final, SampleLegnth * loopfactor);


                        gs3.setLoopType(SamplePlayer.LoopType.LOOP_ALTERNATING);
                        gs3.setPitch(pitchEnv);

                        gs3.getRateUGen().setValue(pbspeed3);

                        //Granular Parameters---------------------------------------
                        Envelope loopStart = new Envelope(0f);
                        loopStart.addSegment(SampleLegnth * 0.5f, SampleLegnth * loopfactor);
                        gs3.setLoopStart(loopStart);

                        Envelope loopEnd = new Envelope(0f);
                        loopEnd.addSegment(SampleLegnth * 0.75f, SampleLegnth * loopfactor);
                        gs3.setLoopEnd(loopEnd);

                        Envelope grainsize = new Envelope(120);
                        grainsize.addSegment(grainsize3, SampleLegnth);
                        gs3.setGrainSize(grainsize);

                        Envelope grainInterval = new Envelope(100);
                        grainInterval.addSegment(graininterval3, SampleLegnth);
                        gs3.setGrainInterval(grainInterval);


                        //Gain and Gain Envelope-------------------------------------
                        Envelope gainEnvelope = new Envelope();
                        gainEnvelope.addSegment(0f, 10);
                        gainEnvelope.addSegment(0.9f, ((SampleLegnth * loopfactor / 2f)));

                        Gain gain3 = new Gain(2, gainEnvelope);
                        gainEnvelope.addSegment(0, ((SampleLegnth * loopfactor / 2f)), new KillTrigger(gain3));

                        //Main and FX output
                        gain3.addInput(gs3);
                        reverb.addInput(gain3);
                        ti.addInput(gain3);
                        hb.sound(gain3);

                    }
                }
                //---------------------------------------------------------------------------


                if (rendererIndex == 3) {

                    if (counter % 12 == 0) {
                        Sample pcsample4 = SampleManager.randomFromGroup(groupName4);
                        float SampleLegnth = (float) pcsample4.getLength();

                        GranularSamplePlayer gs4 = new GranularSamplePlayer(pcsample4);

                        //Pitch Envelope---------------------------------------

                        Envelope pitchEnv = new Envelope(pitch4init);
                        pitchEnv.addSegment(pitch4final, SampleLegnth * loopfactor);

                        gs4.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);
                        gs4.setPitch(pitchEnv);

                        gs4.getRateUGen().setValue(pbspeed4);


                        //Granular Parameters---------------------------------------
                        Envelope loopStart = new Envelope(0f);
                        loopStart.addSegment(SampleLegnth * 0.5f, SampleLegnth * loopfactor);
                        gs4.setLoopStart(loopStart);

                        Envelope loopEnd = new Envelope(0f);
                        loopEnd.addSegment(SampleLegnth * 0.75f, SampleLegnth * loopfactor);
                        gs4.setLoopEnd(loopEnd);

                        Envelope grainsize = new Envelope(120);
                        grainsize.addSegment(grainsize1, SampleLegnth * loopfactor);
                        gs4.setGrainSize(grainsize);

                        Envelope grainInterval = new Envelope(100);
                        grainInterval.addSegment(graininterval1, SampleLegnth * loopfactor);
                        gs4.setGrainInterval(grainInterval);


                        //Gain and Gain Envelope-------------------------------------
                        Envelope gainEnvelope = new Envelope();
                        gainEnvelope.addSegment(0f, 10);
                        gainEnvelope.addSegment(0.9f, ((SampleLegnth * loopfactor / 2f)));

                        Gain gain4 = new Gain(2, gainEnvelope);
                        gainEnvelope.addSegment(0, ((SampleLegnth * loopfactor / 2f)), new KillTrigger(gain4));

                        //Main and FX output
                        gain4.addInput(gs4);
                        reverb.addInput(gain4);
                        ti.addInput(gain4);
                        hb.sound(gain4);

                    }
                }

                //Variables at Clock Rate ---------------------------------


                //setting the sample group at certain times


                //"Score" ------Continuity--->Discontinuity----->Continuity


                //Buffer Select

                //A-----------------------------------------------------------
                if (rendererIndex == 0) {
                    if (counter == 20) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 40) {
                        groupName1 = "resoLichen";
                    }
                    if (counter == 60) {
                        groupName1 = "Twigs";
                    }
                    if (counter == 80) {
                        groupName1 = "pineCone";
                    }
                    if (counter == 100) {
                        groupName1 = "bowedSage";
                    }
                    if (counter == 120) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 140) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 160) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 180) {
                        groupName1 = "bowedSage";
                    }
                    if (counter == 200) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 220) {
                        groupName1 = "resoLichen";
                    }
                    if (counter == 240) {
                        groupName1 = "Twigs";
                    }
                    if (counter == 260) {
                        groupName1 = "pineCone";
                    }
                    if (counter == 280) {
                        groupName1 = "bowedSage";
                    }
                    if (counter == 300) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 320) {
                        groupName1 = "resoLichen";
                    }
                    if (counter == 340) {
                        groupName1 = "Twigs";
                    }
                    if (counter == 360) {
                        groupName1 = "pineCone";
                    }
                    if (counter == 380) {
                        groupName1 = "bowedSage";
                    }
                    if (counter == 400) {
                        groupName1 = "pineCone";
                    }
                    if (counter == 420) {
                        groupName1 = "Twigs";
                    }
                    if (counter == 440) {
                        groupName1 = "resoLichen";
                    }
                    if (counter == 480) {
                        groupName1 = "Lichen";
                    }
                    if (counter == 500) {
                        groupName1 = "null";
                    }

                }
                //B------------------------------------------------------------>
                if (rendererIndex == 1) {
                    if (counter == 20) {
                        groupName2 = "Lichen";
                    }
                    if (counter == 40) {
                        groupName2 = "resoLichen";
                    }
                    if (counter == 60) {
                        groupName2 = "Twigs";
                    }
                    if (counter == 80) {
                        groupName2 = "pineCone";
                    }
                    if (counter == 100) {
                        groupName2 = "bowedSage";
                    }
                    if (counter == 120) {
                        groupName2 = "Lichen";
                    }
                    if (counter == 140) {
                        groupName2 = "Lichen";
                    }
                    if (counter == 160) {
                        groupName2 = "bowedSage";
                    }
                    if (counter == 180) {
                        groupName2 = "Lichen";
                    }
                    if (counter == 200) {
                        groupName2 = "resoLichen";
                    }
                    if (counter == 220) {
                        groupName2 = "Twigs";
                    }
                    if (counter == 240) {
                        groupName2 = "pineCone";
                    }
                    if (counter == 260) {
                        groupName2 = "bowedSage";
                    }
                    if (counter == 280) {
                        groupName2 = "Lichen";
                    }
                    if (counter == 300) {
                        groupName2 = "resoLichen";
                    }
                    if (counter == 320) {
                        groupName2 = "Twigs";
                    }
                    if (counter == 340) {
                        groupName2 = "pineCone";
                    }
                    if (counter == 360) {
                        groupName2 = "bowedSage";
                    }
                    if (counter == 380) {
                        groupName2 = "bowedSage";
                    }
                    if (counter == 400) {
                        groupName2 = "pineCone";
                    }
                    if (counter == 420) {
                        groupName2 = "Twigs";
                    }
                    if (counter == 440) {
                        groupName2 = "resoLichen";
                    }
                    if (counter == 480) {
                        groupName2 = "Lichen";
                    }
                    if (counter == 500) {
                        groupName2 = "null";
                    }
                }

                //C----------------------------------------------------->
                if (rendererIndex == 2) {
                    if (counter == 20) {
                        groupName3 = "Lichen";
                    }
                    if (counter == 40) {
                        groupName3 = "resoLichen";
                    }
                    if (counter == 60) {
                        groupName3 = "Twigs";
                    }
                    if (counter == 80) {
                        groupName3 = "pineCone";
                    }
                    if (counter == 100) {
                        groupName3 = "bowedSage";
                    }
                    if (counter == 120) {
                        groupName3 = "Lichen";
                    }
                    if (counter == 140) {
                        groupName3 = "bowedSage";
                    }
                    if (counter == 160) {
                        groupName3 = "Lichen";
                    }
                    if (counter == 180) {
                        groupName3 = "resoLichen";
                    }
                    if (counter == 200) {
                        groupName3 = "Twigs";
                    }
                    if (counter == 220) {
                        groupName3 = "pineCone";
                    }
                    if (counter == 240) {
                        groupName3 = "bowedSage";
                    }
                    if (counter == 260) {
                        groupName3 = "Lichen";
                    }
                    if (counter == 280) {
                        groupName3 = "resoLichen";
                    }
                    if (counter == 300) {
                        groupName3 = "Twigs";
                    }
                    if (counter == 320) {
                        groupName3 = "pineCone";
                    }
                    if (counter == 340) {
                        groupName3 = "bowedSage";
                    }
                    if (counter == 360) {
                        groupName3 = "bowedSage";
                    }
                    if (counter == 380) {
                        groupName3 = "bowedSage";
                    }
                    if (counter == 400) {
                        groupName3 = "pineCone";

                    }
                    if (counter == 420) {
                        groupName3 = "Twigs";
                    }
                    if (counter == 440) {
                        groupName3 = "resoLichen";
                    }
                    if (counter == 480) {
                        groupName3 = "Lichen";
                    }
                    if (counter == 500) {
                        groupName3 = "null";
                    }
                }

                //D--------------------------------------------------->
                if (rendererIndex == 3) {
                    if (counter == 20) {
                        groupName4 = "Lichen";
                    }
                    if (counter == 40) {
                        groupName4 = "resoLichen";
                    }
                    if (counter == 60) {
                        groupName4 = "Twigs";
                    }
                    if (counter == 80) {
                        groupName4 = "pineCone";
                    }
                    if (counter == 100) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 120) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 140) {
                        groupName4 = "Lichen";
                    }
                    if (counter == 160) {
                        groupName4 = "resoLichen";
                    }
                    if (counter == 180) {
                        groupName4 = "Twigs";
                    }
                    if (counter == 200) {
                        groupName4 = "pineCone";
                    }
                    if (counter == 220) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 240) {
                        groupName4 = "Lichen";
                    }
                    if (counter == 260) {
                        groupName4 = "resoLichen";
                    }
                    if (counter == 280) {
                        groupName4 = "Twigs";
                    }
                    if (counter == 300) {
                        groupName4 = "pineCone";
                    }
                    if (counter == 320) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 340) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 360) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 380) {
                        groupName4 = "bowedSage";
                    }
                    if (counter == 400) {
                        groupName4 = "pineCone";

                    }
                    if (counter == 420) {
                        groupName4 = "Twigs";
                    }
                    if (counter == 440) {
                        groupName4 = "resoLichen";
                    }
                    if (counter == 480) {
                        groupName4 = "Lichen";
                    }
                    if (counter == 500) {
                        groupName4 = "null";
                    }
                }
                

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

                ///Gesture Short and Initial Pitch
                if (x > acc_threshold) {

                    loopfactor = (1 + (hb.rng.nextInt(2)));

                    pitch1init = 1f;
                    pitch2init = 1f;
                    pitch3init = 1f;
                    pitch4init = 1f;
                    pitch1final = 1f;
                    pitch2final = 1f;
                    pitch3final = 1f;
                    pitch4final = 1f;

                    pbspeed1 = 2f;
                    pbspeed2 = 2f;
                    pbspeed3 = 2f;
                    pbspeed4 = 2f;
                    hb.setStatus("X pos: " + x + y + z);

                    ///Gesture Low To High Pitch Slow Playback Speed
                } else if (y > acc_threshold) {
                    loopfactor = (3 + (hb.rng.nextInt(4)));

                    pitch1init = .4f;
                    pitch2init = .5f;
                    pitch3init = .3f;
                    pitch4init = .6f;
                    pitch1final = 2f;
                    pitch2final = 3f;
                    pitch3final = 2f;
                    pitch4final = 2.5f;

                    pbspeed1 = .5f;
                    pbspeed2 = .5f;
                    pbspeed3 = .5f;
                    pbspeed4 = .5f;
                    hb.setStatus("Y pos ");
                    //Gesture Hi to Low Pitch Medium Playback Speed
                } else if (z > acc_threshold) {
                    loopfactor = (3 + (hb.rng.nextInt(3)));

                    pitch1init = 1.5f;
                    pitch2init = 2f;
                    pitch3init = 2.5f;
                    pitch4init = 2f;
                    pitch1final = .8f;
                    pitch2final = .5f;
                    pitch3final = .7f;
                    pitch4final = .75f;

                    pbspeed1 = 1.5f;
                    pbspeed2 = 1.3f;
                    pbspeed3 = 1.4f;
                    pbspeed4 = 1.2f;
                    hb.setStatus("Z pos ");
                }
                //Gesture: Undulating Pitch, Medium Playback Speed
                if (x < acc_negativethresh) {
                    loopfactor = (2 + (hb.rng.nextInt(3)));

                    pitch1init = 1.2f;
                    pitch2init = 1.3f;
                    pitch3init = 1.4f;
                    pitch4init = 1.2f;
                    pitch1final = .8f;
                    pitch2final = .9f;
                    pitch3final = 1.1f;
                    pitch4final = .95f;

                    pbspeed1 = .5f;
                    pbspeed2 = .6f;
                    pbspeed3 = .5f;
                    pbspeed4 = .75f;
                    hb.setStatus("X neg ");

                    //Gesture: Undulating High Pitch, Slow Playback Speed
                } else if (y < acc_negativethresh) {
                    loopfactor = (2 + (hb.rng.nextInt(2)));

                    pitch1init = 2f;
                    pitch2init = 1.5f;
                    pitch3init = 2.2f;
                    pitch4init = 1.8f;
                    pitch1final = 1.4f;
                    pitch2final = 1.7f;
                    pitch3final = 2.3f;
                    pitch4final = 1.5f;

                    pbspeed1 = .25f;
                    pbspeed2 = .25f;
                    pbspeed3 = .25f;
                    pbspeed4 = .25f;
                    hb.setStatus("Y neg");

                    //Gesture: Wobbly Low Pitch, Very Slow Playback Speed
                } else if (z < acc_negativethresh) {
                    loopfactor = (1 + (hb.rng.nextInt(2)));

                    pitch1init = .5f;
                    pitch2init = .2f;
                    pitch3init = .6f;
                    pitch4init = .75f;
                    pitch1final = .3f;
                    pitch2final = .5f;
                    pitch3final = .4f;
                    pitch4final = .2f;

                    pbspeed1 = .2f;
                    pbspeed2 = .18f;
                    pbspeed3 = .25f;
                    pbspeed4 = .15f;

                    hb.setStatus("Z neg ");
                }
                

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
