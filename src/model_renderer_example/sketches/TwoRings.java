package model_renderer_example.sketches;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.control.ControlScope;
import net.happybrackets.core.control.DynamicControl;
import net.happybrackets.core.control.FloatControl;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class TwoRings implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GenericSampleAndClockRenderer> renderers = new ArrayList<>();

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset(); //Clears any running code on the device
        rc.reset();
        rc.getInternalClock().stop();
        rc.getInternalClock().setInterval(100);
        rc.getInternalClock().start();

        //set up the RC
        rc.setRendererClass(GenericSampleAndClockRenderer.class);
        rc.loadHardwareConfigurationforUnity("config/hardware_setup_casula.csv");
        rc.renderers.forEach(renderer -> {
            renderers.add((GenericSampleAndClockRenderer) renderer);
        });

        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r -> {
                String section[] = r.name.split("-");
                if (clock.getNumberTicks() % 10 == 0 && section[1].substring(0, 1).matches("I")) {
                        r.decay(0.94f);
                        r.brightness(1f);
                        r.setRGB(hb.rng.nextInt(255), 255, 5);
                }
                if (clock.getNumberTicks() % 10 == 3 && section[1].substring(0, 1).matches("O")){
                        r.decay(0.94f);
                        r.brightness(1f);
                        r.setRGB(hb.rng.nextInt(255),5,255);
                    }
            });
            rc.sendSerialcommand();
        });

    }

    @Override
    public void doReset() {

    }
}