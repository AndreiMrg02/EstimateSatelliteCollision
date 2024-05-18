package com.ucv.datamodel.satellite;

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;

public class SatelliteEventHandler implements EventHandler {

	private String satName;
	private List<AbsoluteDate[]> intervals;
	private AbsoluteDate startDate;
	private AbsoluteDate endDate;

	public SatelliteEventHandler(String satName) {
		this.satName = satName;
		this.intervals = new ArrayList<>();
	}

	public String getSatName() {
		return satName;
	}

	public List<AbsoluteDate[]> getIntervals() {
		return intervals;
	}

	@Override
	public void init(SpacecraftState initialState, AbsoluteDate target, EventDetector detector) {
		startDate = initialState.getDate();
		endDate = target;
		EventHandler.super.init(initialState, target, detector);
	}

	@Override
	public Action eventOccurred(SpacecraftState spacecraftState, EventDetector eventDetector, boolean increasing) {
		if (increasing) {
			AbsoluteDate[] pair = new AbsoluteDate[2];
			pair[0] = spacecraftState.getDate();
			pair[1] = endDate;
			intervals.add(pair);
		} else {
			if (intervals.isEmpty()) {
				AbsoluteDate[] pair = new AbsoluteDate[2];
				pair[0] = startDate;
				pair[1] = spacecraftState.getDate();
				intervals.add(pair);
			} else {
				AbsoluteDate[] pair = intervals.get(intervals.size() - 1);
				pair[1] = spacecraftState.getDate();
			}
		}
		return Action.CONTINUE;
	}

	// Cum ar trebui tratata chestia asta?
	@Override
	public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
		return EventHandler.super.resetState(detector, oldState);
	}
}
