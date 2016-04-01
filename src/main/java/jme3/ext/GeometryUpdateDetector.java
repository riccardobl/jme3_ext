package jme3.ext;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Transform;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import jme3.ext.VertexBufferUpdateTracker.TrackingMode;
import lombok.extern.log4j.Log4j2;

/**
 * Detect geometry updates
 * 
 * @author Riccardo Balbo
 */
@Log4j2
public class GeometryUpdateDetector extends SharedBaseAppState{
	
	public static GeometryUpdateDetector getInstance(SimpleApplication app){
		GeometryUpdateDetector dbv=app.getStateManager().getState(GeometryUpdateDetector.class);
		if(dbv==null){
			dbv=new GeometryUpdateDetector(app.getRootNode());
			app.getStateManager().attach(dbv);
		}
		return dbv;
	}
	
	public static class GeometryState{
		public static final byte NONE												=0x00;
		public static final byte NEW_GEOMETRY   						    =0x02;
		public static final byte  GEOMETRY_MESH_UPDATED          =0x04;
		public static final byte GEOMETRY_TRANFORM_UPDATED   = 0x08;
		public static final byte GEOMETRY_REMOVED 						 =0x10;
		
		public static boolean isSet(byte state,byte searched_state){
			if((state&searched_state)==state)return true;
			return false;
		}
	}

	public static interface GeometryUpdateListener{
		public boolean onUpdate(float tpf,Geometry g,byte state);
	}

	protected final Spatial _ROOT;
	protected Application APP;

	private byte UID=0;
	private List<Snapshot> SNAPSHOT=new LinkedList<Snapshot>();
	private VertexBufferUpdateTracker VBTRACKER;
	private final List<GeometryUpdateListener> _LISTENERS=new LinkedList<GeometryUpdateListener>();
	
	


	private static class VbSnapshot{
		public long timestamp;
		public VertexBuffer buffer;
	}
	private static class Snapshot{
		public Geometry geom;
		public byte update_id=-1;
		public List<VbSnapshot> vbsnapshots=new ArrayList<VbSnapshot>();
		public Transform transforms;
	}
	
	protected Snapshot doSnapshot(Geometry geom){
		LOGGER.debug("Do snapshot for {}",geom);
		Snapshot sn=new Snapshot();
		sn.geom=geom;
		sn.update_id=UID;
		sn.transforms=geom.getWorldTransform().clone();
		getGeometryState(geom,sn);
		SNAPSHOT.add(sn);
		return sn;
	}
	
	
	protected Snapshot getSnapshot(Spatial g){
		for(Snapshot sn:SNAPSHOT){
			if(sn.geom==g){
				sn.update_id=UID;
				return sn;
			}
		}
		LOGGER.debug("No available snapshot for {} {}",g.hashCode(),g.getName());
		if(LOGGER.isDebugEnabled()){
			for(Snapshot sn:SNAPSHOT){
				LOGGER.debug("- Snapshot of {} {}",sn.geom.hashCode(),sn.geom.getName());
			}
			
		}
		return null;
	}
	
	public GeometryUpdateDetector(Spatial rootNode){
		_ROOT=rootNode;
	}

	@Override
	public void initialize(Application app) {
		VBTRACKER=VertexBufferUpdateTracker.getInstance(app);		
		this.APP=app;		
	}

	@Override
	public void update(final float tpf) {
//		LOGGER.debug("{}",this.isEnabled());
		UID++;
		_ROOT.depthFirstTraversal(new SceneGraphVisitor(){
			@Override
			public void visit(Spatial spatial) {
				if(!(spatial instanceof Geometry)) return;

				Geometry geom=(Geometry)spatial;
				
				Snapshot snapshot=getSnapshot(spatial);
				if(snapshot==null){
					boolean processed=onUpdate(tpf,geom,GeometryState.NEW_GEOMETRY);
					if(processed)doSnapshot(geom);
				}else{
					byte action=getGeometryState(geom,snapshot);
					onUpdate(tpf,geom,action);
				}
			}
		});

		// Remove references to removed geometries.
		Iterator<Snapshot> snapshots_i=SNAPSHOT.iterator();
		while(snapshots_i.hasNext()){
			Snapshot entry=snapshots_i.next();
			if(entry.update_id!=UID){
				Geometry g=entry.geom;
				onUpdate(tpf,g,GeometryState.GEOMETRY_REMOVED);
				snapshots_i.remove();
				LOGGER.debug("{} is removed! {}!={}",g,entry.update_id,UID);
			}
		}
	}

	protected byte getGeometryState(Geometry g,Snapshot snapshot) {
		byte state=GeometryState.NONE;
		if(snapshot.transforms==null||!snapshot.transforms.equals(g.getWorldTransform())){
			snapshot.transforms=g.getWorldTransform().clone();
			state|=GeometryState.GEOMETRY_TRANFORM_UPDATED;
			LOGGER.debug("Transform updated!");
		}
		
		Mesh m=g.getMesh();		
		List<VbSnapshot>	snapshottedBf=new LinkedList<VbSnapshot>();
		List<VbSnapshot>	vbsnapshots=snapshot.vbsnapshots;		
		boolean updateNeeded=false;
		VertexBuffer buffers[]=(VertexBuffer[])m.getBufferList().toArray();
		for(VertexBuffer b:buffers){
			VbSnapshot vbsnapshot=null;
			for(VbSnapshot s:vbsnapshots){
				if(s.buffer==b){
					vbsnapshot=s;
					snapshottedBf.add(s);
					break;
				}
			}
			if(vbsnapshot==null){ // If the buffer has never been snapshotted (mesh has a new buffer..)
				vbsnapshot=new VbSnapshot();
				vbsnapshot.buffer=b;
				VBTRACKER.add(b,TrackingMode.ALL&~TrackingMode.CPU);
				vbsnapshot.timestamp=VBTRACKER.getLastUpdateTimestamp(b); // Could be tracked by something else
				vbsnapshots.add(vbsnapshot);
				snapshottedBf.add(vbsnapshot);

				updateNeeded=true;
				LOGGER.debug("VertexBuffer {} has not available snapshot",b.getBufferType());
			}else{ // If buffer has already been snapshotted (this buffer is already known) 
				long updatets=VBTRACKER.getLastUpdateTimestamp(b);
				if(updatets>vbsnapshot.timestamp){ // If vb has been updated from latest snapshot
					LOGGER.debug("VertexBuffer {} has been updated. Old timestamp {} New timestamp {}",b.getBufferType(),vbsnapshot.timestamp,updatets);
					vbsnapshot.timestamp=updatets;
					updateNeeded=true;
				}
			}
		}
		Iterator<VbSnapshot> sn_i=vbsnapshots.iterator();
		while(sn_i.hasNext()){
			VbSnapshot sn=sn_i.next();
			if(!snapshottedBf.contains(sn))sn_i.remove();
		}
		if(updateNeeded)state|=GeometryState.GEOMETRY_MESH_UPDATED;
		return state;
	}

	public void addListener(GeometryUpdateListener listener){
		if(!_LISTENERS.contains(listener)){
			for(Snapshot sn:SNAPSHOT)listener.onUpdate(-1,sn.geom,GeometryState.NEW_GEOMETRY);
			_LISTENERS.add(listener);
		}
	}
	public void removeListener(GeometryUpdateListener listener){
		_LISTENERS.remove(listener);
	}
	
	private boolean onUpdate(float tpf,Geometry g,byte state){
		boolean p=false;
		for(GeometryUpdateListener l:_LISTENERS){
			p|=l.onUpdate(tpf,g,state);
		}
		return p;
	}


	@Override
	protected void cleanup(Application app) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void onEnable(){
		VBTRACKER.setEnabled(this,true);
	}
	
	@Override
	protected void onDisable(){
		VBTRACKER.setEnabled(this,false);

	}
	
}