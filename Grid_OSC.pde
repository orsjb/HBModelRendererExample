import oscP5.*;
import netP5.*;

OscP5 oscP5;
NetAddress toPi;

int boxsize = 50;
int cols, rows;
color[][] colors;

void setup() {
  size(1700, 300);
  cols = width/boxsize;
  rows = height/boxsize;
  colors = new color[cols][rows];
  for (int i=0; i<cols; i++) {
    for (int j=0; j<rows; j++) {
      colors[i][j] = color(255);
    }
  }
  oscP5 = new OscP5(this, 5555);
  toPi = new NetAddress("192.168.1.104", 4444);
}

void draw() {
  background(255);
  for (int i=0; i<cols; i++) {
    for (int j=0; j<rows; j++) {
      int x = i*boxsize;
      int y = j*boxsize;
      if (mouseX > x && mouseX < (x + boxsize) && mouseY > y && mouseY < (y + boxsize)) {
        if (mousePressed && (mouseButton == LEFT)) {
            colors[i][j] = color(0);
            OscMessage myMessage = new OscMessage("/position");
            myMessage.add(i);
            myMessage.add(j);
            oscP5.send(myMessage, toPi);
            println("sent" + myMessage);
        }
        fill(colors[i][j]);
        rect(x, y, boxsize, boxsize);
      }
    }
  }
}
