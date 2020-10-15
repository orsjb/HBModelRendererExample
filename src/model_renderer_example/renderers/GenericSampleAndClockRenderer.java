package model_renderer_example.renderers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.*;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.util.ArrayList;
import java.util.List;

/**
 * This render allows you to:
 * load up a database of sounds
 * choose a sound to play
 * choose whether that sound is granular or straight
 */

public class GenericSampleAndClockRenderer extends Renderer {

    //list of sounds
    public static List<Sample> samples = new ArrayList<>();

    //the renderer controller
    RendererController rc = RendererController.getInstance();

    //audio objects
    boolean useGranular = true;
    GranularSamplePlayer gsp;
    SamplePlayer sp;
    Gain g;

    //lfo
    WavePlayer lfo;
    float lfoDepth;

    //audio controls
    Glide gain;
    Glide pitch;

    //other timing params
    int clockIntervalLock = 0;
    double clockLockPosition = 0;
    float clockDelayTicks = 0;

    //light data
    private double[] sparkleD = new double[]{0, 0, 0};
    double[] rgbD = new double[]{255, 255, 255};
    double masterBrightness = 0;
    double pulseBrightness = 1;
    double decay = 0.7f;
    double sparkle = 0;

    //id for tracking event objects
    public int currentSample = -1;
    int timeoutThresh = 50;
    int timeout = 0;
    boolean audioIsSetup = false;
    long timeOfLastTriggerMS = 0;

    @Override
    public void setupLight() {
        rc.addClockTickListener((offset, this_clock) -> {       //assumes clock is running at 20ms intervals for now
            int beatCount = (int)this_clock.getNumberTicks();
            if (clockIntervalLock > 0 && beatCount % clockIntervalLock == clockDelayTicks) {
                triggerBeat(beatCount);
            }
            triggerTick();
        });
    }

    @Override
    public void setupAudio() {
        audioIsSetup = true;
        //construct audio elements
        pitch = new Glide(1);
        gsp = new GranularSamplePlayer(1);
        sp = new SamplePlayer(1);
        setSample(0);
        gsp.setKillOnEnd(false);
        gsp.setInterpolationType(SamplePlayer.InterpolationType.LINEAR);
        gsp.setLoopType(SamplePlayer.LoopType.NO_LOOP_FORWARDS);
        gsp.getGrainSizeUGen().setValue(100);
        gsp.getRandomnessUGen().setValue(0.1f);
        gsp.setPitch(pitch);
        sp.setKillOnEnd(false);
        sp.setInterpolationType(SamplePlayer.InterpolationType.LINEAR);
        sp.setLoopType(SamplePlayer.LoopType.NO_LOOP_FORWARDS);
        sp.setPitch(pitch);
        //set up a clock
        rc.addClockTickListener((offset, this_clock) -> {       //assumes clock is running at 20ms intervals for now
            int beatCount = (int)this_clock.getNumberTicks();
            if (clockIntervalLock > 0 && beatCount % clockIntervalLock == clockDelayTicks) {
                triggerBeat(beatCount);
            }
        });
        gain = new Glide(0);
        g = new Gain(1, gain);
        useGranular(true);
        out.addInput(g);
        //lfo stuff
        lfoDepth = 1;
        lfo = new WavePlayer(50, Buffer.SINE);
    }

    public void triggerBeat(int beatCount) {
        lightLoopTrigger();
        if(audioIsSetup) {
            triggerSampleWithOffset(0);
        }
    }

    public void triggerTick() {
        lightUpdate();
        timeout++;
        if(timeout > timeoutThresh) {
            if(audioIsSetup) {
                gain.setValue(0);
            }
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
        sparkleD[0] = Math.random() * 255;
        sparkleD[1] = Math.random() * 255;
        sparkleD[2] = Math.random() * 255;
        rc.displayColor(this,
                (int)clip((rgbD[0] + sparkle * sparkleD[0] * masterBrightness), 0, 255),
                (int)clip((rgbD[1] + sparkle * sparkleD[1] * masterBrightness), 0, 255),
                (int) clip((rgbD[2] + sparkle * sparkleD[2] * masterBrightness), 0, 255)
        );
    }

    public void setRGB(int r, int g, int b) {
        rgbD[0] = r;
        rgbD[1] = g;
        rgbD[2] = b;

    }

    //audio controls

    public static void addSample(String samplename) {
        Sample sample = SampleManager.sample(samplename);
        if(sample != null) {
            samples.add(sample);
            System.out.println("Sample index: " + (samples.size() - 1) + ": " + samplename);
        } else {
            System.out.println("ERROR: there was a problem loading sample: " + samplename);
        }
    }

    public void useGranular(boolean yes) {
        if(audioIsSetup) {
            useGranular = yes;
            g.clearInputConnections();
            if (yes) {
                g.addInput(gsp);
            } else {
                g.addInput(sp);
            }
        }
    }

    public void setSample(int index) {
        if(gsp != null && samples.size() > index) {
            gsp.setSample(samples.get(index));
            sp.setSample(samples.get(index));
            currentSample = index;
        }
        timeout = 0;
    }

    public void rate(float rate) {
        if(audioIsSetup) {
            gsp.getRateUGen().setValue(rate);
            sp.getRateUGen().setValue(rate);
        }
        timeout = 0;
    }

    public void grainOverlap(float overlap) {
        if(audioIsSetup) {
            float interval = gsp.getGrainIntervalUGen().getValue();
            gsp.getGrainSizeUGen().setValue(interval * overlap);
        }
        timeout = 0;
    }

    public void grainInterval(float interval) {
        if(audioIsSetup) {
            gsp.getGrainIntervalUGen().setValue(interval);
        }
        timeout = 0;
    }

    public void gain(float gain) {
        if(audioIsSetup) {
            this.gain.setValue(gain);
        }
        timeout = 0;
    }

    public void random(float random) {
        if(audioIsSetup) {
            gsp.getRandomnessUGen().setValue(random);
        }
        timeout = 0;
    }

    public void pitch(float pitch) {
        if(audioIsSetup) {
            this.pitch.setValue(pitch);
        }
        timeout = 0;
    }

    public void loopType(SamplePlayer.LoopType type) {
        if(gsp != null) {
            gsp.setLoopType(type);
            sp.setLoopType(type);
        }
        timeout = 0;
    }

    public void loopStart(float start) {
        if(audioIsSetup) {
            gsp.getLoopStartUGen().setValue(start);
        }
        timeout = 0;
    }

    public void loopEnd(float end) {
        if(audioIsSetup) {
            gsp.getLoopEndUGen().setValue(end);
        }
        timeout = 0;
    }

    public void clockInterval(int interval) {
        clockIntervalLock = interval;
        timeout = 0;
    }

    public void clockDelay(float delayTicks) {
        clockDelayTicks = delayTicks;
        timeout = 0;
    }

    public void clockLockPosition(float positionMS) {
        clockLockPosition = positionMS;
    }

    public void lfoFreq(float freq) {
        if(audioIsSetup) {
            lfo.setFrequency(freq);
        }
        timeout = 0;
    }

    public void lfoDepth(float depth) {
        lfoDepth = depth;
        timeout = 0;
    }

    public void lfoWave(Buffer wave) {
        if(audioIsSetup) {
            lfo.setBuffer(wave);
        }
        timeout = 0;
    }

    public void position(double ms) {
        if(audioIsSetup) {
            gsp.setPosition(ms);
            sp.setPosition(ms);
        }
        timeout = 0;
    }

    public void brightness(float brightness) {
        this.masterBrightness = brightness;
        timeout = 0;
    }

    public void pulseBrightness(float pulseBrightness) {
        this.pulseBrightness = pulseBrightness;
        timeout = 0;
    }

    public void decay(float decay) {
        this.decay = decay;
        timeout = 0;
    }

    public void sparkle(float sparkle) {
        this.sparkle = sparkle;
        timeout = 0;
    }

    public void quiet() {
        brightness(0);
        gain(0);
    }

    public void triggerSampleWithOffset(double offset) {
        position(clockLockPosition + offset);
        timeOfLastTriggerMS = System.currentTimeMillis() - (int)offset;
    }

    //LFO controls

    public void setLFORingMod() {
        if(audioIsSetup) {
            //the LFO is multiplied to the combined signal of the GSP and SP.
            clearLFO();
            out.clearInputConnections();
            Function f = new Function(lfo, g) {
                @Override
                public float calculate() {
                    return (1 - x[0] * lfoDepth) * x[1];
                }
            };
            out.addInput(f);
        }
        timeout = 0;
    }

    public void clearLFO() {
        if(audioIsSetup) {
            out.clearInputConnections();
            out.addInput(g);
        }
        timeout = 0;
    }

    public long timeSinceLastTriggerMS() {
        return System.currentTimeMillis() - timeOfLastTriggerMS;
    }
    public double clip(double val, double min, double max) {
        return Math.min(Math.max(val, min), max);
    }
}
