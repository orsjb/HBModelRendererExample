package new_outline;

import net.happybrackets.device.HB;

public class ExampleHBBehaviour1 extends HBBehaviour {

    @annotated
    float armx;

    @annotated
    int clusterGroup;

    @Override
    public void setupSound() {

    }

    @Override
    public void setupLight() {

    }

    @Override
    public void tick() {

    }

    @augustosannotation
    public void setRedLight(float val) {
        r = x + val;
    }

}
