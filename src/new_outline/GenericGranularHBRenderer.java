package new_outline;

import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;

public class GenericGranularHBRenderer extends HBBehaviour {

    @Override
    public void setupAudio() {
        GranularSamplePlayer gsp = new GranularSamplePlayer(null);
        //out.addInput(gsp);
        //etc.
    }

    @Override
    public void setupLight() {

    }

    @Override
    public void tick(Clock clock) {
        //loop shit here
    }

    @Override
    public void action(HB hb) {

    }
}
