# HBModelRendererExample
Project example of the new concept of Model / Renderer inside HappyBrackets

## HB.jar - Model / Renderer version
This repository contains a modified version of the HappyBrackets HB.jar that has Model / Renderer features.

## New classes
This version of HappyBrackets contains a package called `net.happybrackets.sychronisedmodel`

This package creates an interface to create models and renderers that will play in sync across multiple devices.

## Model
The models have a few common properties that were made to seemingly communicate with the renderers, they are:
- They operate in a 2D space with Height x Weight;
- This 2D space is a field which the model 'disturbs';
- You can access the disturbance intensity of each x,y position of field or the average intensity of an area of this field;
- The models change over time and have a frame count controlling its state;
- The multiple devices can exchange their model state by:
  - Sharing the frame count
  - Sharing the model state (when the model has additional properties)
  - Sharing the field state
- You can add DIADs to the model, with specific positions, and retrieve the intensity information from the DIAD perspective.

There are 3 available models but you can extend the `SynchronisedModels` class and create your own.
- IncrementalModel: each x,y position intensity is an increment (+1) of its previous neighbor's intensity added to the frame count
  - `frameCount+x+y`
- SineWaveModel: each intensity is a SineWave function of its position and frame count
  - `intensity = (Math.sin( x *Math.sin(frameCount)/10 * TWO_PI / width) + Math.sin((float)(y + frameCount) * TWO_PI / height));`
- FlockingModel
  - Classic flocking model. This model has additional properties:
    - Number of boids
    - Random seed
  - Despite being an agent-based model, it will execute the same across all devices because the random seed is the same. 
  - Besides retrieving the intensity of the model's field you can also retrieve information on how many boids are in an area of the field and which specific boid(s) are them.


## Renderer
The renderers are like a monitor for the computer, they interpret information from the model and 'render' this information according to the renderer design.
However, the renderers can also be used without the models.
The renderer concept has two parts:
- Renderer Controller;
- Renderers (at the moment it can be a light or a speaker).

### RendererController
The `RendererController` (RC) is a singleton class that coordinates the execution of each renderer. 
There is only one RendererController object in each HappyBrackets instance (DIAD or Pi).

You can access the singleton by using:
`RendererController rc = RendererController.getInstance();`

Next, you need to reset the RC in case it has been used before
`rc.reset();`

This method will:
- clear the renderers list;
- turn off the lights (if any);
- flush and disable the serial port (if enabled);
- clear the clock listener list;
- start the clock (if stopped).

Next, you should attach the current HB sketch's parent to the RC:
`rc.setHB(hb);`

Next, you should define which type of Renderer the RC will create:
`rc.setRendererClass(SimpleRenderer.class);`

Next, you define and add renderer objects to the RC:
```
rc.addRenderer(Renderer.Type.SPEAKER, "hb-b827eb999a03",120,200, 0,"Speaker-Left", 0);
rc.addRenderer(Renderer.Type.SPEAKER,"hb-b827eb999a03",460,200, 0,"Speaker-Right", 1);
rc.addRenderer(Renderer.Type.LIGHT,"hb-b827eb999a03",120,90, 0,"Light-1", 0);
rc.addRenderer(Renderer.Type.LIGHT,"hb-b827eb999a03",120,310, 0,"Light-2", 1);
```

You must retrieve the RC master clock to add new tick listeners to it, just like in HappyBrackets. The RC clock is started by default.
```
Clock clock = rc.getInternalClock();
clock.setInterval(50);
rc.addClockTickListener((offset, this_clock) -> {/* Add your code here */});
```

### Renderers
The renderers (`net.happybrackets.sychronisedmodel.Renderer`) represent a Speaker or a Light in the real world. (We can add other types in the future)
The `Renderer` class needs to be extended to be used. They have common attributes that will be used by the extended classes:
```
public String hostname;
public float x;
public float y;
public float z;
public String name;
public int id;
public enum Type {
    SPEAKER,
    LIGHT
}

public UGen out;
public int[] rgb;

```

As shown before, the constructor of the Renderer receives as parameters, in this order:
- Renderer Type
- Hostname: the hostname tell the RC to which DIAD/Pi this renderer is physically located in the real world. This renderer will only be effectively added to the RC if the current hostname matches this name. 
There is a special hostname name 'Unity' which serves to simulate LIGHT type renderers in the Unity Simulator.
- x,y,z position
- name: add a name to the device to easily distinguish them. In 'Unity' this name is used to match with the Unity object names.
- id: This number determined which channel/position this object is attached. For speakers is channel 0/1, for light is the light position in the serial port.


The extended Renderer class must be a separate file that will be added to the startup folder inside each DIAD/Pi.

**WARNING:** If you have an anonymous class inside your Renderer, you must copy them manually to the startup folder, the HB Plugin will not do this for you.
[Check this video](https://www.youtube.com/watch?v=TkkaPl8Hfjo) on how to Save Additional classes into your HB.

When you extend the Renderer class you can Override the basic setup methods:
```
public class SimpleRenderer extends Renderer {
    public SimpleRenderer() {
    }

    @Override
    public void setupLight() {
    }
    
    @Override
    public void setupAudio() {
    }
}
```

This setup method will be called once when the Renderer object is added to the RC.

The Renderer magic happens inside your HB sketch, where you write your tick listeners.
`rc.addClockTickListener((offset, this_clock) -> {/* Add your code here */});`

You can browse this GitHub repository to see examples of Renderer class and sketches.

[Watch this video](https://www.youtube.com/watch?v=h_xj5PdrtgU) to learn how to create start with HappyBrackets


### Unity Simulator:
Tutorial videos [here](https://drive.google.com/drive/folders/1KwE97ASManrqIXJ4T6D2xpfxX3sDE2vw?usp=sharing)

#### How to:
Use this version of this HB project
https://github.com/orsjb/HBModelRendererExample

Download unity hub and make an account, you don't need to start a project or get a specific editor version yet.
https://unity3d.com/get-unity/download

Download the-mind-at-work-unity repo 
https://github.com/gutosantos82/The-Mind-at-Work-Unity
You can download it as a zip and extract it somewhere you can find it.

Run unity hub and make sure you're signed in.

Add the the-mind-at-work-unity repo.
Download the correct editor from the warning prompt.
Go to Assets>Scenes in the project panel, drag and drop SampleScene to the heirarchy panel.
Delete the default untitled scene in the heirarchy panel.

Open intelliJ and run simulator from the HappyBrackets menu.
Recompile and send the OrbitExample to the simulator.

There are some sliders here to see how things work. Double click your machine name in the HappyBrackets Plugin window to see them. 
You can also control these parameters with OSC messages from Max/MSP or something if you like.
