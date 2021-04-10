package model_renderer_example.sketches;

import de.sciss.net.OSCMessage;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.Glide;
import net.beadsproject.beads.ugens.WavePlayer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.core.control.BooleanControl;
import net.happybrackets.core.control.ControlScope;
import net.happybrackets.core.control.DynamicControl;
import net.happybrackets.core.control.FloatControl;
import net.happybrackets.device.HB;
import net.happybrackets.rendererengine.*;

import java.awt.*;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//This explores the relationship between closely pitched sounds.
//Pitches that are close, but slightly different (a few hertz off)
//are played on different devices.
//pitches are calculated by multiplying the Device ID with a variable.
//sine waves are probably best for gallery install; most pleasant
//inspired by Tristan Perich Microtonal wall.

//improvement: make sliders to more quickly test out starting frequencies,
//relationships between frequencies (how many hertz), changes over time.
//improvement: map sounds from lowest to highest.
//improvement: otoacoustic
//improvement: change variables over time.

public class PenInkStain implements HBAction, HBReset {
    //renderer
    public static class HCRenderer extends Renderer {
        RendererController rc = RendererController.getInstance();

        //audio objects
        Gain g;
        Glide gain;
        public float frequency0 = 510;
        public float frequency1 = 512;

        /**you could add more frequencies here, provided you want them to be spatially seperate from the other frequencies
         * You can determine their spatialisation in the HBAction outside this renderer
         */

        WavePlayer wavePlayer0 = new WavePlayer(frequency0, Buffer.SAW);

        /**If you want more waveplayers/sample players etc you can add them here*/

        //light stuff
        double[] rgbD = new double[]{255, 255, 255};
        double masterBrightness = 0;

        //id for tracking event objects
        boolean audioIsSetup = false;

        //pulse stuff
        double pulsePos = 0;
        double pulseInterval = 100;

        @Override
        public void setupAudio() {
            audioIsSetup = true;
            gain = new Glide(0.1f);
            g = new Gain(1, gain);
            out.addInput(g);
        }
        public void useWavePlayer() { //used in boolean control
            if(audioIsSetup) {
                g.clearInputConnections();
                g.addInput(wavePlayer0);
            }
        }
        public void gain(float gain) {
            if (this.audioIsSetup) {
                this.gain.setValue(gain);
            }
        }
        public void stopWavePlayer(){
            g.clearInputConnections();
        } //used in boolean control


        @Override
        public void setupLight() {
            rc.addClockTickListener((offset, this_clock) -> {       //assumes clock is running at 20ms intervals for now
                triggerTick();
            });
        }
        public void triggerTick() {
            lightUpdate();
        }
        public void lightUpdate() {
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
        public double clip(double val, double min, double max) {
            return Math.min(Math.max(val, min), max);
        }
    }

    //code
    RendererController rc = RendererController.getInstance();
    HB hb;
    List<HCRenderer> renderers = new ArrayList<>();

    float binauralBeatSpeed;
    float TWO_PI = (float) (Math.PI * 2);

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset();
        rc.reset();

        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(20);
        rc.getInternalClock().start();
        rc.setRendererClass(HCRenderer.class);

        String computerName = null;  // test on home speakers
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,0,0,0,"left",0);
        rc.addRenderer(Renderer.Type.SPEAKER,computerName,300,0,0,"right",1);
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv");

        // setup
        rc.renderers.forEach(r -> {
            HCRenderer hr = (HCRenderer) r;
            renderers.add(hr);
        });

        /**This is where the structure is divided in half. In the forEach loop, you can use coordinates to divide them up
         * E.G. hr.x, hr.y, hr.type == speaker.
         * There's a video example of how to add properties to the renderers to map them by other properties.
         * Commented out below is a way to divide things up by angle
         */

        renderers.forEach(r->{
            HCRenderer hr = (HCRenderer) r;

            /** The x distance is 428, so this is where it is divided in half
             * You can see the coordinates in the csv Device/HappyBrackets/config/hardware_setup_casula.csv
             */

            if (hr.x < 214) {

                float hue = (880 / hr.frequency0); //normalise frequency 0-1;
                int c = Color.HSBtoRGB(hue, 1, 1);
                int red = (c >> 16) & 0xFF;
                int green = (c >> 8) & 0xFF;
                int blue = c & 0xFF;
                hr.setRGB(red, green, blue);
                hr.wavePlayer0.setFrequency(hr.frequency0);
            }
            if (hr.x > 214) {
                float hue = 880 / hr.frequency1 - 0.218f; //normalise frequency 0-1;
                int c = Color.HSBtoRGB(hue, 1, 1);
                int red = (c >> 16) & 0xFF;
                int green = (c >> 8) & 0xFF;
                int blue = c & 0xFF;
                hr.setRGB(red, green, blue);
                hr.wavePlayer0.setFrequency(hr.frequency1);
            }
            hr.useWavePlayer();
            binauralBeatSpeed = Math.abs(hr.frequency1 - hr.frequency0);

        });

//        rc.renderers.forEach(r->{
//            HCRenderer hr = (HCRenderer) r;
//            float angleHeight[] = mapCartesianToCylinder(hr);
//            float theta = angleHeight[0];
//
//            /** this sets up angles and heights.
//             * angleHeight[0] = the angle from 0-TWO_PI
//             * angleHeight[1] = the height from 0-1 (normalised instead of how it is with r.y)
//             */
//
//            if (theta < TWO_PI * 0.25){
//                hr.setRGB(255,0, (int) (angleHeight[1] * 255));
//                hr.wavePlayer0.setFrequency(hr.frequency0);
//            }
//
//            if (theta < TWO_PI * 0.5 && theta > TWO_PI * 0.25){
//                hr.setRGB((int) (angleHeight[1] * 255),255,0);
//                hr.wavePlayer0.setFrequency(hr.frequency0);
//            }
//
//            if (theta < TWO_PI * 0.75 && theta > TWO_PI * 0.5){
//                hr.setRGB(0,(int) (angleHeight[1] * 255),255);
//                hr.wavePlayer0.setFrequency(hr.frequency1);
//
//            }
//
//            if (theta > TWO_PI * 0.75){
//                hr.setRGB(255,(int) (angleHeight[1] * 255),255);
//                hr.wavePlayer0.setFrequency(hr.frequency1);
//            }
//
//            hr.useWavePlayer();
//            binauralBeatSpeed = Math.abs(hr.frequency1 - hr.frequency0);
//        });

        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r->{
                HCRenderer hr = (HCRenderer) r;
                hr.brightness(1f);
            });
            rc.sendSerialcommand();
        });


        /** All the flashing in time with the binaural beat is done if you want to use that.*/
        // do stuff
//        rc.addClockTickListener((v, clock) -> {
//
//            hb.setStatus("speed: "+ binauralBeatSpeed + " in ms: " + 1000/binauralBeatSpeed + " in bpm: " + 60000/(1000/binauralBeatSpeed));
//            //get the interval time of the clock here
//            double interval = clock.getClockInterval();
//            //now the Renderer needs to keep track of a position in the cycle
//            renderers.forEach(r -> {
//                HCRenderer hr = (HCRenderer) r;
//
//                binauralBeatSpeed = Math.abs(hr.frequency1 - hr.frequency0);
//
//                double pulseValue = Math.sin(hr.pulsePos * 2 * Math.PI) * 0.5 + 0.5;   //range 0-1
//
//                hr.brightness((float) pulseValue);
//
//                if(binauralBeatSpeed == 0) {
//                    hr.pulseInterval = 1000000;
//                    hr.brightness(1f);
//                } else {
//                    hr.pulseInterval = 1000 / binauralBeatSpeed; //ms
//                }
//
//                hr.pulsePos += interval / hr.pulseInterval;//some increment based on interval and pulseRate.
//
//                hr.pulsePos = hr.pulsePos % 1;
//            });
//            rc.sendSerialcommand();
//        });

        //controls


        FloatControl theFirstFrequencyandHue = new FloatControl(this, "Frequency 0", 512) {
            @Override
            public void valueChanged(double control_val) {// Write your DynamicControl code below this line
                renderers.forEach(r->{
                    HCRenderer hr = (HCRenderer) r;
                    hr.frequency0 = (float) control_val;
                    hr.frequency1 = (float) control_val + binauralBeatSpeed;
                    if (hr.x < 214) {
                        float hue = (float) (880 / hr.frequency0); //normalise frequency 0-1;
                        int c = Color.HSBtoRGB(hue, 1, 1);
                        int red = (c >> 16) & 0xFF;
                        int green = (c >> 8) & 0xFF;
                        int blue = c & 0xFF;
                        hr.setRGB(red, green, blue);
                        hr.wavePlayer0.setFrequency(hr.frequency0);
                    } else {
                        float hue = (float) (880 / hr.frequency1) - 0.218f; //normalise frequency 0-1;
                        int c = Color.HSBtoRGB(hue, 1, 1);
                        int red = (c >> 16) & 0xFF;
                        int green = (c >> 8) & 0xFF;
                        int blue = c & 0xFF;
                        hr.setRGB(red, green, blue);
                        hr.wavePlayer0.setFrequency(hr.frequency1);
                    }
                });
            }
        }.setDisplayRange(440, 880, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY).setControlScope(ControlScope.GLOBAL);// End DynamicControl freq0 code

        FloatControl Freq1Multiplier = new FloatControl(this, "Frequency 1 Multiplier", 512/510f) {
            @Override
            public void valueChanged(double control_val) {// Write your DynamicControl code below this line
                renderers.forEach(r->{
                    HCRenderer hr = (HCRenderer) r;
                    hr.frequency1 = (float) (hr.frequency0 * control_val);
                    if (hr.x > 214) {
                        hr.wavePlayer0.setFrequency(hr.frequency1);
                    }
                });
            }
        }.setDisplayRange(0.975, 1.025, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY);// End DynamicControl Freq1Multiplier code

        FloatControl gainControl = new FloatControl(this, "Gain", 0.1f) {
            @Override
            public void valueChanged(double control_val) {
                renderers.forEach(r->{
                    HCRenderer hr = (HCRenderer) r;
                    hr.gain((float) control_val);
                });
            }
        }.setDisplayRange(0, 1, DynamicControl.DISPLAY_TYPE.DISPLAY_ENABLED_BUDDY);// End DynamicControl gainControl code

        BooleanControl pauseWavePlayers = new BooleanControl(this, "On/ Off", true) {
            @Override
            public void valueChanged(Boolean control_val) {/* Write your DynamicControl code below this line */
                // We will pause if checkbox is off
                if(!control_val){
                    renderers.forEach(r->{
                        r.stopWavePlayer();
                        rc.getInternalClock().stop();
//                        r.setRGB(0,0,0);

                    });
                } else if (control_val){
                    renderers.forEach(r->{
                        r.useWavePlayer();
                        rc.getInternalClock().start();
                    });
                }

                /* Write your DynamicControl code above this line */
            }
        };/* End DynamicControl booleanControl code */

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

    @Override
    public void doReset() {
        rc.turnOffLEDs();
        rc.reset();
    }
}



