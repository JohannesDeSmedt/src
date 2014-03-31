package org.cpntools.simulator.extensions.declare;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.cpntools.simulator.extensions.Channel;

/**
 * @author michael
 */
public abstract class DeclarePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Channel channel;
	protected DeclareExtension orphanage;

	/**
	 * 
	 */
	public DeclarePanel() {
		super(new BorderLayout());
	}

	/**
	 * @see java.awt.Component#getName()
	 */
	@Override
	public abstract String getName();

	/**
	 * @param orphanage
	 * @param c
	 */
	public void setChannel(final DeclareExtension orphanage, final Channel c) {
		this.orphanage = orphanage;
		channel = c;
	}

}
