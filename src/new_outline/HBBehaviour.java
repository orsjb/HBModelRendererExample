package new_outline;

import net.happybrackets.core.Device;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.instruments.SampleModule;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;

public class HBBehaviour extends Renderer implements HBAction {


    public static String installationConfig = "config/hardware_setup_casula.csv";
    public static int oscPort = 5555;
    public static RendererController.RenderMode renderMode = RendererController.RenderMode.UNITY;
    public static int clockInterval = 100;
    HB hb;
    RendererController rc = RendererController.getInstance();

    public boolean step1Finished = false;
    public boolean step2Finished = false;

    public static boolean global_step2Finished = false;

    @Override
    public void setupAudio() {

        System.out.println("setupAudio: |" + this.name + "|");
            final String sample_name = "data/audio/long/1979.wav";
            SampleModule sampleModule = new SampleModule();
            if (this.name.contains("Group 1-Outer_Section_1-2_1 Speaker 1") && sampleModule.setSample(sample_name)) {
                sampleModule.connectTo(out);
                sampleModule.setGainValue(0.2f);
                sampleModule.setLoopStart(0);
                sampleModule.setLoopEnd(20000);
            }

    }

    @Override
    public void setupLight() {
        System.out.println("setupLight: " + this.name);
        rc.displayColor(this, 255,255,255);
        colorMode(ColorMode.RGB, 255);
    }

    @Override
    public void tick(Clock clock) {
        System.out.println("tick: " + clock.getNumberTicks() + " device: " + this.name);

        if (type == Type.LIGHT) {
            if (rgb[0] < 50 && !step1Finished) {
                changeBrigthness(2);
            } else {
                step1Finished = true;
            }
            if (step1Finished && !step2Finished) {
                changeHue(2);
                if (rgb[0] > 250 && rgb[1] < 4 && rgb[2] < 4) {
                    step2Finished = true;
                }
            }
            if (step1Finished && step2Finished) {
                changeBrigthness(-2);
                if (rgb[0] == 0) {
                    rgb[0] = 1;
                    step1Finished = step2Finished = false;
                }
            }

            if (id == 0) {
                //System.out.println(id + " - red: " + rgb[0] + " green: " + rgb[1] + " blue: " + rgb[2]);
                //System.out.println(step1Finished +   " " + step2Finished);
            }

//                 After calculating the new color. Push it to the serial 'queue'
            rc.pushLightColor(this);
        }
    }

    @Override
    public void action(HB hb) {
        this.hb = hb;

        hb.reset();
        hb.setStatus(this.getClass().getSimpleName() + " Loaded");

        rc.getInternalClock().setInterval(1000);
        rc.getInternalClock().start();

        rc.addRenderer(Renderer.Type.SPEAKER, Device.getDeviceName(),120,200, 0,"Speaker-Left", 0);
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
