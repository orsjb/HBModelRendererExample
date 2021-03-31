package new_outline;

import net.happybrackets.core.HBAction;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.*;

import java.lang.invoke.MethodHandles;

public class ExampleHBBehaviour1 extends HBBehaviour {

    @HBNumberRange(min = 0, max = 100)
    public int test;

    public ExampleHBBehaviour1() {
        oscPort = 6000;
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

