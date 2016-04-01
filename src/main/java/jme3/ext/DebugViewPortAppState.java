package jme3.ext;

import com.jme3.app.Application;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

import lombok.Getter;

/**
 * Appstate that provide a "debug scene"
 * 
 * @author Riccardo Balbo
 */

public class DebugViewPortAppState extends SharedBaseAppState{

	public static DebugViewPortAppState getInstance(Application app){
		DebugViewPortAppState dbv=app.getStateManager().getState(DebugViewPortAppState.class);
		if(dbv==null){
			dbv=new DebugViewPortAppState();
			app.getStateManager().attach(dbv);
		}
		return dbv;
	}
	
	
	private @Getter ViewPort DEBUG_VIEWPORT;
	private @Getter Node ROOT=new Node();

	
	@Override
	protected void initialize(Application app) {
	}

	@Override
	public void render(RenderManager rm) {
		ROOT.updateGeometricState();
		rm.renderScene(ROOT,DEBUG_VIEWPORT);
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);
		ROOT.updateLogicalState(tpf);
	}
	
	@Override
	public void postRender() {
//		ROOT.updateGeometricState();
	}

	@Override
	protected void onEnable() {
		DEBUG_VIEWPORT=getApplication().getRenderManager().createMainView("Debug viewport",getApplication().getCamera());
		DEBUG_VIEWPORT.attachScene(ROOT);
		setDrawAlwaysOnTop(drawOnTop);
	}

	@Override
	protected void onDisable() {
		ROOT.removeFromParent();
		getApplication().getRenderManager().removeMainView(DEBUG_VIEWPORT);		
	}

	@Override
	protected void cleanup(Application app) {}

	protected boolean drawOnTop;
	public void setDrawAlwaysOnTop(boolean v) {
		drawOnTop=v;
		if(DEBUG_VIEWPORT!=null) DEBUG_VIEWPORT.setClearFlags(false,v,false);
	}
}
