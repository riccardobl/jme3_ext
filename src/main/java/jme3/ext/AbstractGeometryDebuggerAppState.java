package jme3.ext;


import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

import jme3.ext.GeometryUpdateDetector.GeometryUpdateListener;
import lombok.extern.log4j.Log4j2;

/**
 * 
 * @author Riccardo Balbo
 */

@Log4j2
public abstract class AbstractGeometryDebuggerAppState extends BaseAppState implements GeometryUpdateListener{
	private DebugViewPortAppState VIEWPORT;
	private GeometryUpdateDetector GEOM_UPD;
	
	private Spatial  C_A;
	public AbstractGeometryDebuggerAppState(){
		
	}
	public AbstractGeometryDebuggerAppState(Spatial root){
		C_A=root;
	}
	
	@Override
	public void initialize(Application app){
		if(C_A==null){
			if(app instanceof SimpleApplication){
				GEOM_UPD=GeometryUpdateDetector.getInstance((SimpleApplication)(app));
			}else return;
		}else{
			GEOM_UPD=new GeometryUpdateDetector(C_A);		
			app.getStateManager().attach(GEOM_UPD);
			C_A=null;
		}
		VIEWPORT=DebugViewPortAppState.getInstance(app);		
	}
	
	@Override
	protected void onEnable(){
		GEOM_UPD.addListener(this);
		GEOM_UPD.setEnabled(this,true);
		VIEWPORT.setEnabled(this,true);
	}
	
	@Override
	protected void onDisable(){
		GEOM_UPD.removeListener(this);
		GEOM_UPD.setEnabled(this,false);
		VIEWPORT.setEnabled(this,false);

	}
	
	public void attachSpatial(Geometry g){
		LOGGER.debug("Attach {} to debug viewport",g);

		VIEWPORT.getRoot().attachChild(g);
	}
	
	public void detachSpatial(Geometry g){
		LOGGER.debug("Detach {} from debug viewport",g);
		g.removeFromParent();
	}
	
	@Override
	protected void cleanup(Application app){
	
	}

}