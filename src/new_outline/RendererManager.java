package new_outline;

import de.sciss.net.OSCChannel;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCServer;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.List;

public class RendererManager {

    static RendererManager singleton = new RendererManager();

    HB hb;
    List<OutputRenderer> outputRenderers;
    Class<? extends HBBehaviour> behaviourClass;
    Clock masterClock;
    OSCServer server;

    public void setup(HBBehaviour hbBehaviour, HB hb) {
        behaviourClass = hbBehaviour.getClass();
        this.hb = hb;
        //setup OSC
        try {
            server = OSCServer.newUsing(OSCChannel.UDP);
            server.addOSCListener(new OSCListener() {
                @Override
                public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
                    //do your reflection here using behaviour class (incomplete)
                    try {
                        //add your annotation checks etc.
                        Method m = behaviourClass.getMethod(oscMessage.getName(), null); //<-- fix
                        outputRenderers.forEach(r -> {
                            //call function
                            try {
                                m.invoke(r, null);  //fix
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //set up the install configuration by querying the static fields in the behaviour class
        try {
            String installConfig = behaviourClass.getField("installationConfig").toString();    //NOTE this is probably wrong
            //loadConfiguration(installConfig, etc);
            outputRenderers.forEach(r -> {
                //etc.
            });
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        //now we have N renderers of the correct type ready to listen to OSC
        masterClock = hb.createClock(20);
        masterClock.addClockTickListener((v, clock) -> {
            outputRenderers.forEach(r -> {
                r.tick();
            });
            //here I think is where you then push all the serial stuff out to the board??
        }
        );
    }

    public static RendererManager getInstance() {
        return singleton;
    }


}
