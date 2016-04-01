package jme3.ext;

import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import com.jme3.app.state.BaseAppState;

import lombok.extern.log4j.Log4j2;

@Log4j2
public  abstract class SharedBaseAppState extends BaseAppState{
	private final Map<Object,Boolean> _EP=new WeakHashMap<Object,Boolean>();
	
	public void setEnabled(Object pointer,boolean value){
		_EP.put(pointer,value);
		boolean r=false;
		for(Entry<Object,Boolean> e:_EP.entrySet())r|=e.getValue();
		if(r!=isEnabled()){
			LOGGER.debug("{} shared appstate: {}",r?"Enabled":"Disable",this.getClass());
		}
		super.setEnabled(r);
	}
}
