package org.cpntools.simulator.extensions.declare;

import java.util.HashMap;
import java.util.Map;

/**
 * @author michael
 */
public class Module {
	public final Map<String, Constraint> constraints = new HashMap<String, Constraint>();

	/**
	 * 
	 */
	public Module() {
	}

	/**
	 * @param id
	 * @param c
	 */
	public void addConstraint(final String id, final Constraint c) {
		/*if (c.parameterCount()==2) 
		System.out.println("Constraint "+id+" added with 0 "+ c.getParameters(0)+" and 1 "+c.getParameters(1)+" "+c.getFormula());
		else System.out.println("Constraint "+id+" added with 0 "+ c.getParameters(0)+" "+c.getFormula());
		//System.out.println("Already? "+constraints.containsKey(id));*/
		constraints.put(id, c);
		/*		System.out.print("\n");	
		for (String name: constraints.keySet()){
		Constraint value = constraints.get(name);
		if (constraints.get(name).parameterCount()==2)
		System.out.println(name + " 0 "+ value.getParameters(0)+" 1 "+value.getParameters(1)+ " name "+value.getFormula());		
		
		else System.out.println(name + " 0 "+ value.getParameters(0)+" name "+value.getFormula());	
		}*/
		}

	/**
	 * @return
	 */
	public Iterable<Constraint> constraints() {
		return constraints.values();
	}

	/**
	 * 
	 */
	public void removeAllConstraints() {
		constraints.clear();
	}

	/**
	 * @param id
	 */
	public void removeConstraint(final String id) {
		constraints.remove(id);
	}

	/**
	 * @return
	 */
	public int count() {
		return constraints.size();
	}
}
