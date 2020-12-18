package model_renderer_example.sketches;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import model_renderer_example.renderers.GenericSampleAndClockRenderer;
import net.beadsproject.beads.data.SampleManager;
import net.beadsproject.beads.ugens.SamplePlayer;
import net.happybrackets.core.Device;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.OSCUDPListener;
import net.happybrackets.device.HB;
import net.happybrackets.sychronisedmodel.Renderer;
import net.happybrackets.sychronisedmodel.RendererController;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExampleForASUFolks implements HBAction, HBReset {

    RendererController rc = RendererController.getInstance();
    HB hb;
    List<GenericSampleAndClockRenderer> renderers = new ArrayList();
    Map<GenericSampleAndClockRenderer, Integer> rendererIDs = new HashMap<>();

    int count = 0;

    @Override
    public void action(HB hb) {
        this.hb = hb;
        hb.reset();
        rc.reset();
        rc.getInternalClock().setInterval(20);
        rc.getInternalClock().stop();
        rc.getInternalClock().reset();
        rc.getInternalClock().start();
        //adding some samples
        //set up the RC
        rc.setRendererClass(GenericSampleAndClockRenderer.class);
        //set up the configuration of the system
//        rc.loadHardwareConfiguration("config/hardware_setup_casula_iml.csv");
        String hostname = Device.getDeviceName();
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   450,  200f, 450, "LED-W", 0, 16);
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   50, 100f, 0,  "LED-N", 1, 16);
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   50, 100f, 0,  "LED-S", 2, 16 );
        rc.addRenderer(Renderer.Type.LIGHT, hostname,   0, 100f, 0,  "LED-E", 3, 16);
        rc.addRenderer(Renderer.Type.SPEAKER, hostname, 0,  100f, 0, "SPEAKER-W", 0);
        rc.addRenderer(Renderer.Type.SPEAKER, hostname, 0, 100f, 0, "SPEAKER-E", 1);
        hb.setStatus(hostname + ", I Have " + rc.renderers.size() + " objects");

        SampleManager.group("guitar", "data/audio/Nylon_Guitar");

        //for convenience, grab the list of renderers cast to the class we are using
        rc.renderers.forEach(renderer -> {renderers.add((GenericSampleAndClockRenderer) renderer);});
        //some basic configuration
        renderers.forEach(r -> {    // <- this is like the setup() function in Processing
            //DO SET UP HERE
            r.useRegularSamplePlayer();
            r.gain(0);
            r.clockInterval(0);
            r.rate(1);
            r.loopType(SamplePlayer.LoopType.NO_LOOP_FORWARDS);
            r.decay(0.7f);
            int randomAssignment = hb.rng.nextInt(5);       //you might want to keep this line
            rendererIDs.put(r, randomAssignment);
            switch(randomAssignment) {
                case 0:
                    r.setSample(SampleManager.fromGroup("guitar", 0));
                    r.setRGB(200, 0, 50);
                    break;
                case 1:
                    r.setSample(SampleManager.fromGroup("guitar", 1));
                    r.setRGB(50, 200, 255);
                    break;
                case 2:
                    r.setSample(SampleManager.fromGroup("guitar", 2));
                    r.setRGB(255, 255, 255);
                    break;
                case 3:
                    r.setSample(SampleManager.fromGroup("guitar", 3));
                    r.setRGB(100, 200, 0);
                    break;
                case 4:
                    r.setSample(SampleManager.fromGroup("guitar", 4));
                    r.setRGB(60, 60, 255);
                    break;
            }
            //END OF SETUP CODE - CHANGE AS YOU LIKE
        });

        rc.addClockTickListener((v, clock) -> {
            renderers.forEach(r -> {      // <- this is like the draw() function in Processing
                //DO CLOCK STUFF HERE

                int randomAssignment = rendererIDs.get(r);

                switch(randomAssignment) {
                    case 0:
                        if(clock.getNumberTicks() % 10 == 0) {
                            r.setSample(SampleManager.randomFromGroup("guitar"));
                            r.gain(1);
                            r.position(0);
                            r.brightness(1);
                        }
                        break;
                    case 1:
                        if(clock.getNumberTicks() % 15 == 0) {
                            r.setSample(SampleManager.randomFromGroup("guitar"));
                            r.gain(1);
                            r.position(0);
                            r.brightness(1);

                        }
                        break;
                    case 2:
                        if(clock.getNumberTicks() % 20 == 0) {
                            r.setSample(SampleManager.randomFromGroup("guitar"));
                            r.gain(1);
                            r.position(0);
                            r.brightness(1);

                        }
                        break;
                    case 3:
                        if(clock.getNumberTicks() % 30 == 0) {
                            r.setSample(SampleManager.randomFromGroup("guitar"));
                            r.gain(1);
                            r.position(0);
                            r.brightness(1);

                        }
                        break;
                    case 4:
                        if(clock.getNumberTicks() % 40 == 0) {
                            r.setSample(SampleManager.randomFromGroup("guitar"));
                            r.gain(1);
                            r.position(0);
                            r.brightness(1);

                        }
                        break;
                }


                //END OF CLOCK CODE

            });
            rc.sendSerialcommand();
        });

        hb.addControllerListener(new OSCListener() {
            @Override
            public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                if(oscMessage.getName().equals("/reset_clock")) {
                    rc.getInternalClock().stop();
                    rc.getInternalClock().reset();
                    rc.getInternalClock().start();
                }
            }
        });



        // type osclistener to create this code
        OSCUDPListener oscudpListener = new OSCUDPListener(4000) {  //192.168.1.255
            @Override
            public void OSCReceived(OSCMessage oscMessage, SocketAddress socketAddress, long time) {

                //ADD ALL YOUR OSC STUFF HERE

                if(oscMessage.getName().equals("/rate")) {      //example message: "/rate 1.2"
                    renderers.forEach(r -> {
                        int randomAssignment = rendererIDs.get(r);
                        if(randomAssignment == 0) {
                            r.rate(hb.getFloatArg(oscMessage, 0) * (r.x / 450f));
                        }
                    });
                } else if(oscMessage.getName().equals("/red_tint")) {
                    renderers.forEach(r -> {
                        //do something to the RGB value
                    });
                }

                //END OF OSC STUFF
            }
        };

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

    @Override
    public void doReset() {
        rc.reset();
    }
    //</editor-fold>
}
