package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.*;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DA_casula implements HBAction  {
    //renderer
    public static class DevinRenderer extends Renderer {

        RendererController rc = RendererController.getInstance();

        public String groupName = "";

        public float pitchInit = 1;
        public float pitchFinal = 1;
        public float grainSize = 60;
        public float grainInterval = 80;


        public Envelope pitchEnv = new Envelope(1);
        public Envelope loopStartEnv = new Envelope(0);
        public Envelope loopEndEnv = new Envelope(1000);
        public Envelope grainSizeEnv = new Envelope(80);
        public Envelope grainIntervalEnv = new Envelope(70);

        public GranularSamplePlayer gsp;
        Gain g;

        public Gain delayGain = new Gain(1, 1f);

        public TapIn tapIn = new TapIn(2000);
        public TapOut tapOut = new TapOut(tapIn,200f);

        //light stuff
        double[] rgbD = new double[]{255, 255, 255};
        double masterBrightness = 0;
        double decay = 0.99;

        //id for tracking event objects
        int timeoutThresh = -1;
        int timeoutCount = 0;

        boolean audioIsSetup = false;

        @Override
        public void setupLight() {
            rc.addClockTickListener((offset, this_clock) -> {       //assumes clock is running at 20ms intervals for now
                triggerTick();
            });
        }

        @Override
        public void setupAudio() {
            audioIsSetup = true;

            gsp = new GranularSamplePlayer(1);
            gsp.setLoopType(SamplePlayer.LoopType.LOOP_FORWARDS);

            gsp.setPitch(pitchEnv);
            gsp.setLoopStart(loopStartEnv);
            gsp.setLoopEnd(loopEndEnv);
            gsp.setGrainSize(grainSizeEnv);
            gsp.setGrainInterval(grainIntervalEnv);

            g = new Gain(1, 0.5f);

            tapIn.addInput(gsp);
            delayGain.addInput(tapOut);

            g.addInput(gsp);
            g.addInput(delayGain);
            out.addInput(g);
        }

        public void triggerTick() {
            lightUpdate();
            timeoutCount++;
        }

        public void lightUpdate() {
            //decay
            masterBrightness *= decay;
            rc.displayColor(this,
                    (int)clip(rgbD[0] * masterBrightness, 0, 255),
                    (int)clip(rgbD[1] * masterBrightness, 0, 255),
                    (int)clip(rgbD[2] * masterBrightness, 0, 255)
            );
        }

        public void setRGB(int r, int g, int b) {
            rgbD[0] = r;
            rgbD[1] = g;
            rgbD[2] = b;
        }

        public void brightness(float brightness) {
            this.masterBrightness = brightness;
        }

        public void decay(float decay) {
            this.decay = decay;
        }

        public void setTimeoutThresh(int thresh) {
            this.timeoutThresh = thresh;
        }

        public double clip(double val, double min, double max) {
            return Math.min(Math.max(val, min), max);
        }
    }

    //code
    HB hb;
    RendererController rc = RendererController.getInstance();
    List<DevinRenderer> renderers = new ArrayList<>();
    Map<Renderer, float[]> cachedAngles = new HashMap<>();

    float TWO_PI = (float) (Math.PI * 2);

    //playback & granulation parameters
    int loopfactor = 1;
    float clockinterval = 1000;

    float sampleLength;

    //OSC/Accel. params
    float x = 1f;
    float y = 1f;
    float z = 1f;

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(20); //ms time of each tick
        rc.getInternalClock().start();
        rc.setRendererClass(DevinRenderer.class);

        String computerName = null;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,100,0,0,"sound from blob 0",0); //computer audio
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,300,0,0,"sound from blob 1",1);

        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv");

        rc.renderers.forEach(renderer -> {
            DevinRenderer dr = (DevinRenderer) renderer;
            renderers.add(dr);
        });

        //Sample Banks & Clock Timer-------------------------------------------------------------->
        SampleManager.group("pineCone", "data/audio/CapeCodNovember_V2/Pine Cone");
        SampleManager.group("Lichen", "data/audio/CapeCodNovember_V2/Lichen");
        SampleManager.group("bowedSage", "data/audio/CapeCodNovember_V2/Bowed Sage");
        SampleManager.group("resoLichen", "data/audio/CapeCodNovember_V2/Resonator Lichen");
        SampleManager.group("Twigs", "data/audio/CapeCodNovember_V2/Twigs");

        rc.addClockTickListener((offset, clock) -> {// Write your code below this line
            rc.renderers.forEach(r -> {
                DevinRenderer dr = (DevinRenderer) r;
                if (clock.getNumberTicks() % 10 == 0) {
                    //By this line, we are in a clock loop, for each renderer
                    //-----------------------------------------------------
                    if (dr.groupName.length() != 0){
                        sampleLength = (float) dr.gsp.getSample().getLength();
                        System.out.println(sampleLength);
                    }
                    //Pitch Envelope---------------------------------------
                    dr.pitchEnv.clear();
                    dr.pitchEnv.setValue(dr.pitchInit);
                    dr.pitchEnv.addSegment(dr.pitchFinal, sampleLength * 2);

                    //Granular Parameters----------------------------------
                    dr.loopStartEnv.clear();
                    dr.loopStartEnv.setValue(0);
                    dr.loopStartEnv.addSegment(sampleLength * 0.5f, sampleLength * loopfactor);

                    dr.loopEndEnv.clear();
                    dr.loopEndEnv.setValue(0);
                    dr.loopEndEnv.addSegment(sampleLength * 0.75f, sampleLength * loopfactor);

                    dr.grainSizeEnv.clear();
                    dr.grainSizeEnv.setValue(120);
                    dr.grainSizeEnv.addSegment(dr.grainSize, sampleLength * loopfactor);

                    dr.grainIntervalEnv.clear();
                    dr.grainIntervalEnv.setValue(100);
                    dr.grainIntervalEnv.addSegment(dr.grainInterval, sampleLength * loopfactor);
                }

                if (clock.getNumberTicks() % 50 == 0){
                    dr.brightness(1f);
                    dr.decay(0.93f);
                }
            });
            rc.sendSerialcommand();
        });

        //OSC Listener-------------------------------------------------------------
        //gyro stuff
        OSCUDPListener oscudpListener = new OSCUDPListener(8000) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long time) {

                hb.setStatus("message"+ oscMessage.getArg(0)+ oscMessage.getArg(1) + oscMessage.getArg(2));

                x = (float) oscMessage.getArg(0);
                y = (float) oscMessage.getArg(1);
                z = (float) oscMessage.getArg(2);

                //Control Delay Jitter Amount through overall amount of movement of controller Pi
                float magnitude = (float) Math.sqrt(Math.pow(x, 2) + Math.sqrt(Math.pow(y, 2) + Math.pow(z, 2)));
                Glide magGlide = new Glide(magnitude, 50);
                float movement = magGlide.getCurrentValue();

                //Control Delay Feedback Amount through tilting towards Y  axis
                float delayAmt = ((Math.abs(y) / 2) + .48f);
                float delayTime = clockinterval / (movement * 4);

                //Define Positive and Negative Thresholds to Change Gesture States
                float acc_threshold = 0.9f;
                float acc_negativethresh = -0.9f;
                hb.setStatus("test" + x + y + z);
                renderers.forEach(r -> {
                    DevinRenderer dr = (DevinRenderer) r;
                    ///Gesture Short and Initial Pitch

                    dr.delayGain.setGain(delayAmt);
                    dr.tapOut.setDelay(delayTime);

                    if (x > acc_threshold) {
                        loopfactor = (1 + (hb.rng.nextInt(2)));
                        dr.pitchInit = 1f;
                        dr.pitchFinal = 1f;
                        hb.setStatus("X pos: " + x + y + z);
                        ///Gesture Low To High Pitch Slow Playback Speed
                    } else if (y > acc_threshold) {
                        loopfactor = (3 + (hb.rng.nextInt(4)));
                        dr.pitchInit = .4f;
                        dr.pitchFinal = 2f;
                        hb.setStatus("Y pos ");
                        //Gesture Hi to Low Pitch Medium Playback Speed
                    } else if (z > acc_threshold) {
                        loopfactor = (3 + (hb.rng.nextInt(3)));
                        dr.pitchInit = 1.5f;
                        dr.pitchFinal = .8f;
                        hb.setStatus("Z pos ");
                    }
                    //Gesture: Undulating Pitch, Medium Playback Speed
                    if (x < acc_negativethresh) {
                        loopfactor = (2 + (hb.rng.nextInt(3)));
                        dr.pitchInit = 1.2f;
                        dr.pitchFinal = .8f;
                        hb.setStatus("X neg ");

                        //Gesture: Undulating High Pitch, Slow Playback Speed
                    } else if (y < acc_negativethresh) {
                        loopfactor = (2 + (hb.rng.nextInt(2)));
                        dr.pitchInit = 2f;
                        dr.pitchFinal = 1.4f;
                        hb.setStatus("Y neg");

                        //Gesture: Wobbly Low Pitch, Very Slow Playback Speed
                    } else if (z < acc_negativethresh) {
                        loopfactor = (1 + (hb.rng.nextInt(2)));
                        dr.pitchInit = .5f;
                        dr.pitchFinal = .3f;
                        hb.setStatus("Z neg ");
                    }
                });
            }
        };

        // method invoke - call functions from ableton on this port
        new OSCUDPListener(1234) {
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                try {
                    //System.out.println(oscMessage.getName());
                    String methodName = oscMessage.getName().substring(1);
                    Method m = DA_casula.class.getMethod(methodName, OSCMessage.class);
                    m.invoke(DA_casula.this, oscMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

        //ableton stuff
        public void blobSample0(OSCMessage oscMessage) {
            renderers.forEach(renderer -> {
                DevinRenderer dr = (DevinRenderer) renderer;
                float angleHeight[] = mapCartesianToCylinder(dr);
                float theta = angleHeight[0];

//                if (dr.x < 100 && dr.x > 0){ //an area of speakers and lights, can animate this area maybe?
                if(theta < TWO_PI * 0.25){
                    dr.setRGB(116, 0, 184);

                }
                if (dr.type == renderer.type.SPEAKER && dr.id == 0){
                    dr.groupName = (String) oscMessage.getArg(0);
                    Sample sample = SampleManager.randomFromGroup(dr.groupName);
                    dr.gsp.setSample(sample);
                }
            });
        }

    public void blobSample1(OSCMessage oscMessage) { //define the space for each blob her
        renderers.forEach(renderer -> {
            DevinRenderer dr = (DevinRenderer) renderer;
            float angleHeight[] = mapCartesianToCylinder(dr);
            float theta = angleHeight[0];

//                if (dr.x < 100 && dr.x > 0){ //an area of speakers and lights, can animate this area maybe?
            if(theta > TWO_PI * 0.25 && theta < TWO_PI * 0.5) {
                dr.setRGB(83, 144, 217);
            }
            if (dr.type == renderer.type.SPEAKER && dr.id == 1){
                dr.groupName = (String) oscMessage.getArg(0);
                Sample sample = SampleManager.randomFromGroup(dr.groupName);
                dr.gsp.setSample(sample);
            }
        });
    }

    public void blobSample2(OSCMessage oscMessage) {
        renderers.forEach(renderer -> {
            DevinRenderer dr = (DevinRenderer) renderer;
            float angleHeight[] = mapCartesianToCylinder(dr);
            float theta = angleHeight[0];

//                if (dr.x < 100 && dr.x > 0){ //an area of speakers and lights, can animate this area maybe?
            if(theta > TWO_PI * 0.5 && theta < TWO_PI * 0.75){
                    dr.setRGB(86, 207, 225);
                }
            if (dr.type == renderer.type.SPEAKER && dr.id == 1){
                dr.groupName = (String) oscMessage.getArg(0);
                Sample sample = SampleManager.randomFromGroup(dr.groupName);
                dr.gsp.setSample(sample);
            }
        });
    }

    public void blobSample3(OSCMessage oscMessage) {
        renderers.forEach(renderer -> {
            DevinRenderer dr = (DevinRenderer) renderer;
            float angleHeight[] = mapCartesianToCylinder(dr);
            float theta = angleHeight[0];

//                if (dr.x < 100 && dr.x > 0){ //an area of speakers and lights, can animate this area maybe?
                if(theta > TWO_PI * 0.75){
                    dr.setRGB (128, 255, 219);
                }
            if (dr.type == renderer.type.SPEAKER && dr.id == 0){
                dr.groupName = (String) oscMessage.getArg(0);
                Sample sample = SampleManager.randomFromGroup(dr.groupName);
                dr.gsp.setSample(sample);
            }
        });
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
    //</editor-fold>
}

