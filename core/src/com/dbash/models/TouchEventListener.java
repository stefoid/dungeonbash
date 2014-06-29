package com.dbash.models;


public interface TouchEventListener {

	/**
	 * A Touch event has occurred that this listener might be interested in.
	 * 
	 * @param event
	 * @return true when the event has been handled and no other touch event
	 *         handler should be allowed to handle the event.
	 */
	boolean touchEvent(TouchEvent event);

}
