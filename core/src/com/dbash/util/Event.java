package com.dbash.util;

import com.dbash.models.IEventAction;

public class Event {

	public Object owner;
	public IEventAction action;
	public Event(Object owner, IEventAction action) {
		this.owner = owner;
		this.action = action;
	}
}
