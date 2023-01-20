import oscP5.*;
import netP5.*;
import controlP5.*;

ControlP5 cp5;
OscP5 oscP5;
NetAddress local;

void setup() {
  size(400, 400);
  frameRate(25);

  oscP5 = new OscP5(this, 5555); //have to listen annoyingly
  local = new NetAddress("localhost", 5555); //the local host can be 127.0.0.1 or 127.0.1.1 sometimes

  cp5 = new ControlP5(this);

  cp5.addToggle("/isOn")
    .setPosition(width * 0.5, height * 0.5)
    .setSize(50, 20)
    .setValue(true)
    .setMode(ControlP5.SWITCH)
    ;

  cp5.addSlider("/printOrange")
    .setPosition(100, 50)
    .setRange(0, 255)
    ;
}

void draw() {
  background(0);
}

public void controlEvent(ControlEvent theEvent) {
  println(theEvent.getController().getName() + " " + theEvent.getController().getValue());

  OscMessage myMessage = new OscMessage(theEvent.getController().getName());
  myMessage.add(theEvent.getController().getValue());
  oscP5.send(myMessage, local);
  println(myMessage);
}
