package com.dbash.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.dbash.models.IEventAction;

//EVENT BUS.
//You subscribe to an event with the subscriber (an Object) a string (event name) 
// and a block to run when that event occurs.
//The event bus is a map of arrays.  Every time there is an event that matches the key, it iterates through 
// the array and calls those blocks.
//The subscriber registers an object as the owner (probably itself).
//Then it can unsubscribe from that event .  unsubscribe(me, eventname);
//So there needs to be an EventListener that holds the owner and the block.  
// This is in the array that is mapped to the eventname key.
//In the case of a double subscribe to the same event from the same owner, and then an 
// unsubscribe, all listeners for that owner for that event will be unsubscribed.
public class EventBus {
	
	HashMap<String, ArrayList<Event>> eventListeners;
	
	private static EventBus defaultEventBus = null;

   public static EventBus getDefault() {
      if(defaultEventBus == null) {
    	  defaultEventBus = new EventBus();
      }
      return defaultEventBus;
   }
	
	public EventBus() {
		eventListeners = new  HashMap<String, ArrayList<Event>>();
	}
	
	public void onEvent(String eventType, Object owner, IEventAction action) {
		ArrayList<Event> list = getEventListeners(eventType);
		if (list == null) {
			list = new ArrayList<Event>();
			eventListeners.put(eventType, list);
		}
		Event event = new Event(owner, action);
		list.add(event);
	}
	
	private ArrayList<Event> getEventListeners(String eventType) {
		ArrayList<Event> list = eventListeners.get(eventType);
		if (list == null) {
			list = new ArrayList<Event>();
			eventListeners.put(eventType, list);
		}
		return list;
	}
	
	public void removeListener(String eventType, Object owner) {
		ArrayList<Event> list = getEventListeners(eventType);
		Iterator<Event> iterator = list.iterator();
		while (iterator.hasNext()) {
			Event event = iterator.next();
			if (event.owner == owner) {
				iterator.remove();
			}
		}
	}
	
	public void event(String eventType, Object param) {
		ArrayList<Event> list = getEventListeners(eventType);
		
		// create a copy of the list for traversal, in case the action wants to concurrently remove Listeners.
		ArrayList<Event> iterList = new ArrayList<Event>(list);
		for (Event event : iterList) {
			event.action.action(param);
		}
	}
	
	public void removeAll(Object thing) {
		for (String eventType : eventListeners.keySet()) {
			removeListener(eventType, thing);
		}
	}
	
	public void reset() {
		for (String eventType : eventListeners.keySet()) {
			ArrayList<Event> list = eventListeners.get(eventType);
			if (list != null) {
				list.clear();
			}
		}
	}
}
