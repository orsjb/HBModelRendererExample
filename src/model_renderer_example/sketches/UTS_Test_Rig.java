package model_renderer_example.sketches;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.beadsproject.beads.ugens.WavePlayer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.control.ControlScope;
import net.happybrackets.core.control.DynamicControl;
import net.happybrackets.core.control.FloatControl;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class UTS_Test_Rig implements HBAction, HBReset {

    public static class WavePlayerRenderer extends Renderer {
        RendererController rc = RendererController.getInstance();

        //audio objects
        public Gain g;

        public WavePlayer wp;
        public SamplePlayer sp;

        boolean audioIsSetup = false;

        //light data
        public double[] rgbD = new double[]{255, 255, 255};
        public double masterBrightness = 0;
        public double pulseBrightness = 1;
        public double decay = 0.95f;


        //id for tracking event objects
        public int timeoutThresh = -1;
        public int timeoutCount = 0;
        long timeOfLastTriggerMS = 0;

        @Override
        public void setupAudio() {
            audioIsSetup = true;
            g = new Gain(1, 0.1f);
            wp = new WavePlayer(666, Buffer.TRIANGLE);
            sp = new SamplePlayer(1);
            out.addInput(g);
        }

        public void useWavePlayer() {
            if(audioIsSetup) {
                g.clearInputConnections();
                g.addInput(wp);
            }
        }

        public void useSamplePlayer(){
            if(audioIsSetup) {
                g.clearInputConnections();
                g.addInput(sp);
            }
        }

        public void setPosition(float position){
            if(audioIsSetup){

            }
        }


        public void stopWavePlayer(){
            g.clearInputConnections();
        }

        @Override
        public void setupLight() {
            rc.addClockTickListener((offset, this_clock) -> {       //assumes clock is running at 20ms intervals for now
                int beatCount = (int)this_clock.getNumberTicks();

                triggerTick();
            });
        }

        public void triggerTick() {
            lightUpdate();
            timeoutCount++;
            if(timeoutThresh >= 0 && timeoutCount > timeoutThresh) {
                masterBrightness = 0;
            }
        }

        //light behaviours
        public void lightLoopTrigger() {
            masterBrightness = pulseBrightness;
        }

        public void lightUpdate() {
            //decay
            masterBrightness *= decay;
            //sparkle

            rc.displayColor(this,
                    (int)clip(((rgbD[0] ) * masterBrightness), 0, 255),
                    (int)clip(((rgbD[1] ) * masterBrightness), 0, 255),
                    (int)clip(((rgbD[2] ) * masterBrightness), 0, 255)
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

        public double clip(double val, double min, double max) {
            return Math.min(Math.max(val, min), max);
        }


    }

    RendererController rc = RendererController.getInstance();
    List<WavePlayerRenderer> renderers = new ArrayList<>();
    String hostname;
    HB hb;

    int count = 0;

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(20); //ms time of each tick
        rc.getInternalClock().start();



        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        rc.setRendererClass(WavePlayerRenderer.class);
        rc.loadHardwareConfiguration("config/hardware_setup_ccs.csv");

        //Can use the below for one pi:
//        rc.addRenderer(Renderer.Type.LIGHT,hostname,0,0,0,"light",0);
//        rc.addRenderer(Renderer.Type.LIGHT,hostname,10,0,0,"light0",1);
//        rc.addRenderer(Renderer.Type.LIGHT,hostname,20,0,0,"light1",2);
//        rc.addRenderer(Renderer.Type.LIGHT,hostname,30,0,0,"light2",3);
//        rc.addRenderer(Renderer.Type.SPEAKER,hostname,0,0,0,"spk ",0);
//        rc.addRenderer(Renderer.Type.SPEAKER,hostname,0,10,10,"spk_2 ",1);

//        Sample s = SampleManager.sample("data/audio/long/DrumLoop130.wav");
        Sample s = SampleManager.sample("data/audio/long/1979.wav");


        rc.renderers.forEach(renderer -> {
            WavePlayerRenderer wpr = (WavePlayerRenderer) renderer;
            renderers.add(wpr);
        });

//        hb.setStatus("my name is " + hostname + "I have " + renderers.size() + " renderers " + "the count is: " + count);

        renderers.forEach(r-> {
            WavePlayerRenderer wpr = (WavePlayerRenderer) r;
            if(wpr.audioIsSetup) {

                wpr.useSamplePlayer(); //you don't need to update audio in the clock
                wpr.sp.setSample(s);

//                wpr.useWavePlayer();
//                wpr.wp.setFrequency(Pitch.mtof(61));

            }
        });

        //step through each light
        rc.addClockTickListener((v, clock) -> { //lights need to be updated on the clock
            renderers.forEach(r -> {

                WavePlayerRenderer wpr = (WavePlayerRenderer) r;
                if (wpr.x < count && wpr.x > count -5) { //define a region for the light
                    wpr.setRGB(60, 60, 60); //some of the globes on this rig have red and green backwards (all three colours to make white)
                    wpr.brightness(0.1f); //this sends a flash, use wpr.decay to change the fade out time.
                    if(wpr.audioIsSetup){
                        wpr.sp.setPosition(hb.rng.nextFloat() * 10000);
                        wpr.sp.setLoopType(SamplePlayer.LoopType.LOOP_BACKWARDS);
                    }
                }


            });

            count ++; //just stepping though values
            if (count > 160) count = 0; //the x value is in the csv file hardware_setup_ccs.csv, the renderers are just in a line. You can change this and add new columns to group stuff however you like.
            rc.sendSerialcommand(); //send this every clock tick
        });


        //cycle through hues
//            rc.addClockTickListener((v, clock) -> {
//                renderers.forEach(r -> {
//                    WavePlayerRenderer wpr = (WavePlayerRenderer) r;
//
//                    float hue = (float) (count * 0.01 % 100);
//                    int c = Color.HSBtoRGB(hue, 1, 1);
//                    int red = (c >> 16) & 0xFF;
//                    int green = (c >> 8) & 0xFF;
//                    int blue = c & 0xFF;
//                    wpr.setRGB(red, green, blue);
//                    wpr.brightness(0.1f);
//
//                });
//                count++;
//                rc.sendSerialcommand();
//            });


        //order the lights in rgbp
//        rc.addClockTickListener((v, clock) -> {
//            renderers.forEach(r -> {
//
//                WavePlayerRenderer wpr = (WavePlayerRenderer) r;
//                wpr.brightness(0.3f);
//
//                if(wpr.id == 0) {
//                    wpr.setRGB(60, 0, 0);
//                }
//
//                if(wpr.id == 1) {
//                    wpr.setRGB(0, 60, 0);
//                }
//                if(wpr.id == 2) {
//                    wpr.setRGB(0, 0, 60);
//                }
//
//                if(wpr.id == 3) {
//                    wpr.setRGB(30, 0, 30);
//                }
//            });
//            rc.sendSerialcommand();
//        });

//
//        rc.addClockTickListener((v, clock) -> {
//            if(clock.getNumberTicks() % 50 == 0) {
//                renderers.forEach(r -> {
//
//                    WavePlayerRenderer wpr = (WavePlayerRenderer) r;
//                    wpr.brightness(0.3f);
//                    wpr.setRGB(0,0, 60;
//
//                });
//
//            }
//            rc.sendSerialcommand();
//        });



//
//        FloatControl brightness = new FloatControl(this, "brightness", 0.1f) {
//            @Override
//            public void valueChanged(double control_val) {// Write your DynamicControl code below this line
//                renderers.forEach(r -> {
//
//                    WavePlayerRenderer wpr = (WavePlayerRenderer) r;
//                    wpr.decay(1.0f);
//
//                    wpr.brightness((float) control_val);
//
//
//
//                });
//                // Write your DynamicControl code above this line
//            }
//        }.setDisplayRange(0f, 0.7f, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY).setControlScope(ControlScope.GLOBAL);// End DynamicControl gain code
//
//
//        FloatControl gain = new FloatControl(this, "gain", 0.1f) {
//            @Override
//            public void valueChanged(double control_val) {// Write your DynamicControl code below this line
//                renderers.forEach(r -> {
//
//                    WavePlayerRenderer wpr = (WavePlayerRenderer) r;
//                    wpr.g.setValue((float) control_val);
//
//
//
//                });
//                // Write your DynamicControl code above this line
//            }
//        }.setDisplayRange(0f, 0.7f, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY).setControlScope(ControlScope.GLOBAL);// End DynamicControl gain code

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
