package new_outline;

import net.beadsproject.beads.ugens.Gain;

public abstract class OutputRenderer {

    enum Type {
        SPEAKER, LED
    }

    Gain out;
    double[] position;
    Type type;

    void setupLight() {

    }

    void setupSound() {

    }

    void tick() {

    }

}
