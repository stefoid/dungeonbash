package com.dbash.models;

public class Event {

	public Object owner;
	public IEventAction action;
	public Event(Object owner, IEventAction action) {
		this.owner = owner;
		this.action = action;
	}
}
