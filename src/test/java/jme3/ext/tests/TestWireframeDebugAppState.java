package jme3.ext.tests;


import com.jme3.math.ColorRGBA;

import jme3.ext.impl.WireframeDebugAppState;

/**
 * @author Riccardo Balbo
 */
public class TestWireframeDebugAppState extends TestTanBnNDebugAppState{

	public static void main(String[] args) {
		TestWireframeDebugAppState app=new TestWireframeDebugAppState();
		app.start();
	}

	@Override
	public void applyAppState() {
		WireframeDebugAppState appstate=new WireframeDebugAppState();
		appstate.setColor(ColorRGBA.Yellow); // Default color is blue
		stateManager.attach(appstate);
	}
}
