import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;

public class MyExample extends HBMultiplicitySketch<GenericSampleAndClockRenderer> {         //<--- this should implement HBAction and encapsulate OutputRenderer (in some way)

    //some of the fields that come with this base class:
    //HB, RC(?), clock, out, RGB value.

    //these static parameters are for the user to configure the system...
    @override static String installationConfig = "myInstall.csv"; //this should override an existing string, default value "installationConfig.csv"
    @override static RenderMode renderMode = RenderMode.UNITY;    //optional, default value = NORMAL.

    //the rest of the code should be dedicated to writing mappings between OSC messages and the renderer

    //All of this stuff below is OSC stuff that can be handled with reflection under the hood.

    @HBParam                //Augusto to look into use of annotations: e.g., we could have an HBParam annotation which flags things are params
    public float armX;      //This field becomes automatically exposed to OSC control > i.e., the message "/armX 0.5" would set the value of armX.
    //question: do you have HBGlobalParam versus HBLocalParam or could you use static.

    public float toeY = 0;
    @HBCommand
    public void legY(float val) {       //this is also exposed as an OSC command > i.e., the message "/legY 0.5" would call the function.
        //do some shit                  //in this case we match the arguments of the function to the expected OSC arguments.
        toeY = val * renderer.x;
    }

    @HBCommand
    public void someOtherThing(OSCMessage message) {    //this works too as an OSC message.
        message.getArg(0); //etc.
    }

    @HBCommand
    public void someOtherThing(Object... args) {    //this works too for a variable length response.
    }

    @override
    public void setup() {
        renderer.gain(0);
        renderer.useSamplePlayer();
        SampleManager.group("blah");
    }

    @override
    public void tick() {
        //whatever you want to happen here
        if(clock.getTickCount() % 4 == 0) {
            //??
        }
    }

    @override
    public void finaliseBeforeUpdate() {            //this happens just before
        renderer.brightness(toeY * a * b * c * toeY);
        renderer.hue(toeY + toeZ);
    }


}
