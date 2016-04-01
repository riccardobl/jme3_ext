package jme3.ext.impl;


import java.util.HashMap;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;

import jme3.ext.AbstractGeometryDebuggerAppState;
import jme3.ext.GeometryUpdateDetector.GeometryState;

import com.jme3.scene.Spatial;

import lombok.extern.log4j.Log4j2;

/**
 * AppState that shows wireframes for debugging purposes
 * 
 * @author Riccardo Balbo
 */
@Log4j2
public class WireframeDebugAppState extends AbstractGeometryDebuggerAppState{

	private Map<Geometry,Geometry> generatedGeometries=new HashMap<Geometry,Geometry>();
	private Material mat;
	private ColorRGBA color=ColorRGBA.Blue;

	public WireframeDebugAppState(){
		super();
	}

	public WireframeDebugAppState(Spatial rootNode){
		super(rootNode);
	}

	@Override
	public void initialize(Application app) {
		super.initialize(app);
		mat=new Material(app.getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color",color);
	}

	@Override
	protected void onDisable(){
		for(Geometry g:generatedGeometries.values()){
			detachSpatial(g);
		}
		generatedGeometries.clear();
		super.onDisable();
	}
	
	
	public void setColor(ColorRGBA c) {
		color=c;
		if(isInitialized()){
			mat.setColor("Color",color);
		}
	}

	@Override
	public boolean onUpdate(float tpf, Geometry g, byte state) {
		if(GeometryState.isSet(state,GeometryState.NONE))return false;
		Mesh mesh=g.getMesh();


		
		if(GeometryState.isSet(state,GeometryState.GEOMETRY_REMOVED)||GeometryState.isSet(state,GeometryState.GEOMETRY_MESH_UPDATED)){
			Geometry generated=generatedGeometries.get(g);
			if(generated!=null){
				detachSpatial(generated);
				generatedGeometries.remove(g);
			}else{
				LOGGER.debug("A remove or regen action has been triggered on {0}, but there is no generated geometry.",g);
			}
		}		
		


		if(GeometryState.isSet(state,GeometryState.NEW_GEOMETRY)||GeometryState.isSet(state,GeometryState.GEOMETRY_MESH_UPDATED)){

			// Works only with triangle based meshes
			if(mesh.getMode()!=Mode.Triangles) return false;
	
			Geometry generated=g.clone();
			generated.setName(generated.getName()+"~Wireframe");
			generated.setMaterial(mat);
	
			generated.setLocalTransform(g.getWorldTransform());
	
			attachSpatial(generated);
			generatedGeometries.put(g,generated);
			return true;		

		}
		
		if(GeometryState.isSet(state,GeometryState.GEOMETRY_TRANFORM_UPDATED)){
			Geometry generated=generatedGeometries.get(g);
			generated.setLocalTransform(g.getWorldTransform());
			return true;
		}
		
		return false;
	}

	@Override
	protected void cleanup(Application app) {
		// TODO Auto-generated method stub
		
	}



}
