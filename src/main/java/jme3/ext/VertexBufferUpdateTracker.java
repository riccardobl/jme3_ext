package jme3.ext;


import java.nio.Buffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
/**
 * AppState for tracking VertexBuffers modifications
 * 
 * @author Riccardo Balbo
 */
public class VertexBufferUpdateTracker extends SharedBaseAppState{

	public static VertexBufferUpdateTracker getInstance(Application app){
		VertexBufferUpdateTracker dbv=app.getStateManager().getState(VertexBufferUpdateTracker.class);
		if(dbv==null){
			dbv=new VertexBufferUpdateTracker();
			app.getStateManager().attach(dbv);
		}
		return dbv;
	}
	
	
	public static class TrackingMode{
		public static final byte NONE      =  		0b00000;		
		public static final  byte CPU        =   		0b00001;
		public static final  byte GPU        =   		0b00010;		
		public static final  byte USAGE    =      	0b00100;		
		public static final  byte TYPE      =      	0b01000;	
		public static final  byte ID            =      	0b10000;	
		public static final  byte ALL         =      	0b11111;	
	}
	
	private static class BufferState{
		public VertexBuffer cpuState;
		public boolean gpuState;
		public Type type;
		public Usage usage;	
		public int id=-2;
	}	
	
	private static class TrackingUnit{
		public Byte mode;
		public Integer trackedBy;
	}
	
	private Map<VertexBuffer,TrackingUnit> trackedBuffers=new WeakHashMap<VertexBuffer,TrackingUnit>();
	private Map<VertexBuffer,BufferState> bufferState=new WeakHashMap<VertexBuffer,BufferState>();
	private Map<VertexBuffer,Long> buffersUpdateTimestamp=new WeakHashMap<VertexBuffer,Long>();
	private static final Logger log=Logger.getLogger(AbstractGeometryDebuggerAppState.class.getName());
	private static final Level logLevel=Level.FINEST;
	

	public byte getTrackingStatus(VertexBuffer buffer){
		TrackingUnit unit=trackedBuffers.get(buffer);
		if( unit==null)return TrackingMode.NONE;
		return unit.mode;
	}

	public void add(VertexBuffer buffer, int mode) {
		TrackingUnit unit=trackedBuffers.get(buffer);
		if(unit==null){
			log.log(logLevel,"Start tracking on {0}",buffer);		

			unit=new TrackingUnit();
			unit.mode=(byte)mode;
			unit.trackedBy=1;
			unit.mode=(byte)(unit.mode|mode);
			trackedBuffers.put(buffer,unit);
		}else unit.trackedBy++;
	}
	
	public void remove(VertexBuffer buffer){
		TrackingUnit unit=trackedBuffers.get(buffer);
		if(unit==null)return;		
		unit.trackedBy--;		
		if(unit.trackedBy==null)trackedBuffers.remove(buffer);
	}
	
	public long getLastUpdateTimestamp(VertexBuffer vb){
		Long out= buffersUpdateTimestamp.get(vb);
		return out==null?0:out;
	}
	
	private boolean doIDTracking(VertexBuffer vb){
		
		BufferState state=bufferState.get(vb);
		if(state==null){
			state=new BufferState();
			bufferState.put(vb,state);
		}
		
		if(vb.getId()!=state.id){
			log.log(logLevel,"Update detected: ID. From {0} to {1}",new Object[]{state.id,vb.getId()});

			buffersUpdateTimestamp.put(vb,System.currentTimeMillis());
			state.id=vb.getId();
			return true;
		}
		return false;
	}
	
	private boolean doGPUTracking(VertexBuffer vb,boolean step2){
		if(vb.getUsage()==Usage.CpuOnly)return false;
		
		BufferState state=bufferState.get(vb);
		if(state==null){
			state=new BufferState();
			bufferState.put(vb,state);
		}
		
		boolean newstate=vb.getId()==-1||vb.isUpdateNeeded();
		if(!step2){
//			log.log(logLevel,"GPU update tracking: step1 {0}",state.gpuState);

			state.gpuState=newstate;			
		}else{
//			log.log(logLevel,"GPU update tracking: step2 {0}",state.gpuState);

			if((state.gpuState&&!newstate)){ // From updateNeeded=true to updateNeeded=false == has been updated.
				log.log(logLevel,"Update detected: GPU! step1 {0}, step2 {1}",new Object[]{ state.gpuState,newstate});
				buffersUpdateTimestamp.put(vb,System.currentTimeMillis());
				state.gpuState=false;
				return true;
			}
		}
		return false;
	}
	
	private boolean compareVb(VertexBuffer a,VertexBuffer b){
		if(a.getNumComponents()!=b.getNumComponents())return false;
		if(a.getNumElements()!=b.getNumElements())return false;
		Buffer ab=a.getData();
		Buffer bb=b.getData();
		if(ab==null^bb==null)return false;
		return (ab==null&&bb==null)||ab.equals(bb);
	}
	
	private boolean doCPUTracking(VertexBuffer vb){
		BufferState state=bufferState.get(vb);
		if(state==null){
			state=new BufferState();
			bufferState.put(vb,state);
		}
		if(!compareVb(state.cpuState,vb)){
			buffersUpdateTimestamp.put(vb,System.currentTimeMillis());
			state.cpuState=vb.clone();
			log.log(logLevel,"Update detected: CPU");

			return true;
		}
		return false;
	}
	
	private boolean doTypeTracking(VertexBuffer vb){
		BufferState state=bufferState.get(vb);
		if(state==null){
			state=new BufferState();
			bufferState.put(vb,state);
		}
		if(state.type!=vb.getBufferType()){
			buffersUpdateTimestamp.put(vb,System.currentTimeMillis());
			log.log(logLevel,"Update detected: Type. From {0} to {1}",new Object[]{state.type,vb.getBufferType()});
			state.type=vb.getBufferType();
			return true;
		}
		return false;
	}
	
	private boolean doUsageTracking(VertexBuffer vb){
		BufferState state=bufferState.get(vb);
		if(state==null){
			state=new BufferState();
			bufferState.put(vb,state);
		}
		if(state.usage!=vb.getUsage()){
			buffersUpdateTimestamp.put(vb,System.currentTimeMillis());
			log.log(logLevel,"Update detected: Usage. From {0} to {1}",new Object[]{state.usage,vb.getUsage()});
			state.usage=vb.getUsage();

			return true;
		}
		return false;
	}
	
	public void render(RenderManager rm) {
		if(trackedBuffers.size()==0){
			log.log(logLevel,"No buffer to track");		
			return;
		}
		log.log(logLevel,"Tracking {0} buffers",bufferState.size());		
		for(Entry<VertexBuffer,TrackingUnit> bufferEntry:trackedBuffers.entrySet()){
			VertexBuffer buffer=bufferEntry.getKey();
			Byte trackingMode=bufferEntry.getValue().mode;
			if((trackingMode&TrackingMode.GPU)==TrackingMode.GPU)	doGPUTracking(buffer,false);			
		}
	}

	public void postRender() {
		if(trackedBuffers.size()==0){
			return;		
		}
		// Check change of state from updateNeeded=true to updateNeeded=false 		
		for(Entry<VertexBuffer,TrackingUnit> bufferEntry:trackedBuffers.entrySet()){
			VertexBuffer buffer=bufferEntry.getKey();
			Byte trackingMode=bufferEntry.getValue().mode;
			if((trackingMode&TrackingMode.ID)==TrackingMode.ID)doIDTracking(buffer);			
			if((trackingMode&TrackingMode.GPU)==TrackingMode.GPU)doGPUTracking(buffer,true);			
			if((trackingMode&TrackingMode.CPU)==TrackingMode.CPU)doCPUTracking(buffer);
			if((trackingMode&TrackingMode.USAGE)==TrackingMode.USAGE)doUsageTracking(buffer);
			if((trackingMode&TrackingMode.TYPE)==TrackingMode.TYPE)doTypeTracking(buffer);			
		}
	}

	

	@Override
	protected void initialize(Application app) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void cleanup(Application app) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onEnable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDisable() {
		// TODO Auto-generated method stub
		
	}
}
