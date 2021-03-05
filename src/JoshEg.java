import net.beadsproject.beads.data.Sample;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class JoshEg implements HBAction {

    float powerDataInterval = 100;

    Map<Sample, float[]> powerValuesForSample = new HashMap<>();

    @Override
    public void action(HB hb) {
        hb.reset(); //Clears any running code on the device
        //Write your sketch below



        Sample s = SampleManager.sample("data/audio/long/1979.wav");
        SamplePlayer sp  = new SamplePlayer(s);
        hb.sound(sp);

        powerValuesForSample.put(s, new float[200]);


        // To create this, just type clockTimer
        Clock clock = HB.createClock(500).addClockTickListener((offset, this_clock) -> {// Write your code below this line

            // Write your code above this line

            if(true) {
                sp.setPosition(0);
            }

            double currentTime = sp.getPosition();
            float newPowerValue = powerValuesForSample.get(s)[(int)(currentTime / powerDataInterval)];


        });

        clock.start();// End Clock Timer


        // write your code above this line
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
