package new_outline;

import net.beadsproject.beads.ugens.GranularSamplePlayer;
import net.happybrackets.device.HB;

public class GenericGranularHBRenderer extends HBBehaviour {

    @Override
    public void setupSound() {
        GranularSamplePlayer gsp = new GranularSamplePlayer(null);
        out.addInput(gsp);
        //etc.
    }

    @Override
    public void setupLight() {

    }

    @Override
    public void tick() {
        //loop shit here
    }
}
