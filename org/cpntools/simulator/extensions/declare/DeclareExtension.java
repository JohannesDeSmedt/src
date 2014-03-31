package org.cpntools.simulator.extensions.declare;

/* 
 * Author: Johannes De Smedt (johannes.desmedt@kuleuven.be)
 * This code is meant for illustration purposes only. As such, it's not meant to be a full extension to CPN Tools 
 * but rather enables the possibility to add mixed-paradigm hierarchies for testing.
 * The author has no intentions to use this implementation commercially.
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import ltl2aut.automaton.AcceptabilityFlavor;
import ltl2aut.automaton.Automaton;
import ltl2aut.regexp.RegExp;

import org.clapper.util.classutil.ClassInfo;
import org.cpntools.accesscpn.engine.protocol.Packet;
import org.cpntools.simulator.extensions.AbstractExtension;
import org.cpntools.simulator.extensions.Channel;
import org.cpntools.simulator.extensions.Command;
import org.cpntools.simulator.extensions.Instrument;
import org.cpntools.simulator.extensions.Invocation;
import org.cpntools.simulator.extensions.Option;
import org.cpntools.simulator.extensions.graphics.Canvas;
import org.cpntools.simulator.extensions.graphics.Rectangle;
import org.cpntools.simulator.extensions.scraper.Arc;
import org.cpntools.simulator.extensions.scraper.Place;
import org.cpntools.simulator.extensions.scraper.Scraper;
import org.cpntools.simulator.extensions.scraper.Transition;
import org.cpntools.simulator.extensions.server.Handler;
import org.cpntools.simulator.extensions.utils.Discovery;

import dk.klafbang.tools.Pair;

/**
 * @author michael
 */
public class DeclareExtension extends AbstractExtension {
	/**
	 * 
	 */
	public static final int ID = 10001;
	private JDialog dialog;
	private Collection<DeclarePanel> extensions;
	private JTextArea packet;
	private JTabbedPane tabs;
	JFrame frame = new JFrame("Hierarchy Extension");
	Scanner iin = new Scanner (System.in);
	boolean launched=true;
	
	private final Map<String, Transition> transitions = new HashMap<String, Transition>();
	
	private final Map<String, Automaton> automata = new HashMap<String, Automaton>();
	
	@SuppressWarnings("unused")
	private final Option<Boolean> DATA_AWARE = Option.create(
			"Data-aware simulation", "data_aware", Boolean.class);

	private final Map<String, Module> modules = new HashMap<String, Module>();

	@SuppressWarnings("unused")
	private final Option<Boolean> SMART = Option.create("Smart simulation",
			"smart", Boolean.class);

	private final Map<String, Integer> states = new HashMap<String, Integer>();
	private final Map<String, Task> tasks = new HashMap<String, Task>();
	private final List<String> trace = new ArrayList<String>();
	int count = 0;
	private boolean isGenerated = false;

	
	Map<String, Map<String, Transition>> hiertrans = new HashMap<String,Map<String,Transition>>();
	Map<Integer,String> idmap = new HashMap<Integer,String>();
	/**
	 * 
	 */
	public DeclareExtension() {
		 addOption(SMART);
		// addOption(DATA_AWARE, SMART);
		addLazySubscription(
				new Command(400, 2), // Syntax check page
				new Command(500, 3), // Generate instances
				new Command(500, 4), // Update instances
				new Command(500, 11, true), // Start run
				new Command(500, 12), // Execute transition
				new Command(500, 13), // Check transition for enabledness
				new Command(500, 14), // Checked enabledness without scheduler
				new Command(500, 15), // Manual binding
				new Command(500, 20), // Init state
				new Command(500, 21), // Create + reset scheduler
				new Command(500, 35), // Check enabling of list of transitions
				new Command(500, 36), // Check enabling of transitions without
										// scheduler
				 new Command(10000,1202) //Check for B's Birthday
		// new Command(800, 1) // Set state space options
		);

		System.out.println("Declare Extension started");
		
				
		addObserver(new Observer() {

			@Override
			public void update(Observable source, Object value) {
				if (value instanceof Option) {
					Option o = (Option) value;
					if (o.getType() == Boolean.class) {
					}
				}
			}
		});
	}
	
	
	
	@Override
	public void setChannel(final Channel c) {
		super.setChannel(c);
		final Scraper s = c.getExtension(Scraper.class);
		s.addObserver(new Observer() {
			{
				s.notifyMe(this);
			}

			public void update(final Observable source, final Object value) {
		
				if (source == s && value instanceof Scraper.Event) {
					final Scraper.Event e = (org.cpntools.simulator.extensions.scraper.Scraper.Event) value;
					if (e.getType() == Scraper.EventType.ADDED
							&& e.getElm() instanceof Transition) {
						final Transition t = (Transition) e.getElm();
						
						count++;
						transitions.put(t.getId(),t);
						idmap.put(count,t.getId());
					}
					if (e.getType() == Scraper.EventType.ADDED
							&& e.getElm() instanceof Place) {
						final Place p = (Place) e.getElm();
						//System.out.println("Place: "+ p.getName() );		
						
					}
					if (e.getType() == Scraper.EventType.ADDED
							&& e.getElm() instanceof Arc) {
						final Arc a = (Arc) e.getElm();
						//System.out.println("Arc: "+ a.getTransition().getName() );		
						
					}
				}
			}
		});
	
	
		
	}
	
	public void update(final Observable arg0, final Object arg1) {
		if (arg0 == this) {
			if (arg1 instanceof Invocation) {
				final Invocation i = (Invocation) arg1;
				if ("debug".equals(i.getInstrument().getKey())) {
					dialog.setVisible(true);
				}
			}
		}
	}
	
	
	/**
	 * @see org.cpntools.simulator.extensions.Extension#getIdentifier()
	 */
	@Override
	public int getIdentifier() {
		return DeclareExtension.ID;
	}

	/**
	 * @see org.cpntools.simulator.extensions.Extension#getName()
	 */
	@Override
	public String getName() {
		return "Declare Altered";
	}

	/**
	 * @see org.cpntools.simulator.extensions.Extension#handle(org.cpntools.accesscpn.engine.protocol.Packet)
	 */
	
	public void showTrans() {
		Transition temptrans;
		
		for (int i=1; i<=transitions.size(); i++)
		{ 		  
		  temptrans = transitions.get(idmap.get(i));
		  System.out.println("Transition "+i+" named "+temptrans.getName()+" id "+temptrans.getId());	
		}
	}
	
	
	public void GUIIni(){
		final DefaultListModel listModel;
		Transition temptrans;
		int tranum = 0;
		
		for (String tra : transitions.keySet()) {
			tranum++;
			idmap.put(tranum, transitions.get(tra).getId());
		}

		String[][] transTable = new String[transitions.size()][2];
		int i = 0;
		for (String trans : transitions.keySet()) {
			transTable[i][0] = Integer.toString(i + 1);
			transTable[i][1] = transitions.get(trans).getName();
			i++;
		}

		String[] columnNames = { "Transition name", "Transition ID" };
		final JTable supertranstable = new JTable(transTable, columnNames);
		supertranstable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JPanel supertablePanel = new JPanel(new BorderLayout());
		supertablePanel.add(new JLabel("Select super-transition",
				SwingConstants.LEFT), BorderLayout.BEFORE_FIRST_LINE);
		supertablePanel.add(supertranstable);

		final JTable subtranstable = new JTable(transTable, columnNames);
		subtranstable
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JPanel subtablePanel = new JPanel(new BorderLayout());
		subtablePanel.add(new JLabel("Select sub-transition",
				SwingConstants.LEFT), BorderLayout.BEFORE_FIRST_LINE);
		subtablePanel.add(subtranstable);

		JList list = new JList();
		listModel = new DefaultListModel();
		list = new JList(listModel);
		JPanel hierPanel = new JPanel(new BorderLayout());
		hierPanel.add(new JLabel("Super-transitions installed: ",
				SwingConstants.LEFT), BorderLayout.NORTH);
		hierPanel.add(list);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		
		
		JButton continueBut = new JButton("Continue");
		continueBut.addActionListener(new ActionListener() {
			int dummint = 5;
			
			public void actionPerformed(ActionEvent e) {
			frame.setVisible(false);
		}});
		
			
		
		
		JButton cancelButton = new JButton("Add");
		cancelButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
			
				boolean subloop = false;
				boolean superloop = false;
				int[] subarray = new int[transitions.size()];
				subarray = subtranstable.getSelectedRows();

				int superarray = 0;
				superarray = supertranstable.getSelectedRow();

				Transition superTrans;
				superTrans = transitions.get(idmap.get(superarray + 1));
				Map<String, Transition> subtrans = new HashMap<String, Transition>();

				String listModelstring = "";

				for (int a = 0; a < subtranstable.getSelectedRowCount(); a++) {
					subtrans.put(idmap.get((subarray[a]) + 1),
							transitions.get(idmap.get((subarray[a]) + 1)));

					for (String trans : hiertrans.keySet()) {
						if (hiertrans.containsKey(transitions.get(
								idmap.get((subarray[a]) + 1)).getId())) {
							superloop = true;
							//System.out.println("Superloop is true");
						} else {
							for (String id : hiertrans.get(trans).keySet()) {
								if (transitions.get(
										idmap.get((subarray[a]) + 1)).getId() == hiertrans
										.get(trans).get(id).getId()
										|| (superTrans.getId() == hiertrans
												.get(trans).get(id).getId())) {
									subloop = true;
									//System.out.println("Subloop is true");
								}
							}
						}
					}

					if (a != 0) {
						listModelstring = listModelstring
								+ "/"
								+ transitions.get(idmap.get((subarray[a]) + 1))
										.getName();
					} else {
						listModelstring = listModelstring
								+ transitions.get(idmap.get((subarray[a]) + 1))
										.getName();
					}
				}
				
				if (subtrans.containsKey(superTrans.getId())) {
					JOptionPane.showMessageDialog(null, "Error, super-transition in sub-transition section");
				}
				else if (superloop) { JOptionPane.showMessageDialog(null, "Error, sub-transition is already super-transition");}
				else if (subloop) {JOptionPane.showMessageDialog(null, "Error, sub-transition already in hierarchy"); }
				else {
				hiertrans.put(superTrans.getId(), subtrans);
				listModelstring = superTrans.getName() + " ---- "
						+ listModelstring;
				listModel.addElement(listModelstring);
				}
				
				}

		});

		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
			Object toRemove;
			listModel.clear();				
			hiertrans.clear();
			}});
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(cancelButton);
		buttonPane.add(removeButton);
		buttonPane.add(continueBut);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		mainPanel.add(supertablePanel);
		mainPanel.add(subtablePanel);
		mainPanel.add(buttonPane, BorderLayout.PAGE_END);
		mainPanel.add(hierPanel);
		mainPanel.add(Box.createVerticalStrut(10));

		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(mainPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setSize(500, 650);
		//frame.setAlwaysOnTop(true);
		frame.setVisible(true);
	}
	
	
	@Override
	public Packet handle(final Packet p) {
		Scanner iin = new Scanner (System.in);
		int tint =0;
		int hint =1;
		int boolint = 0;
		Transition superTrans;		
		
		if (launched)	{GUIIni();
		launched=false;
		System.out.println("Press key + enter to continue");
		tint = iin.nextInt();
		}
		/*
		System.out.println("Add hierarchy (0/1)?");
		boolint = iin.nextInt();
		
		if (boolint==1){
		while (hint==1){	
		System.out.println("Please select supertransition : \n\n");
		showTrans();
		tint = iin.nextInt();
	
		superTrans = transitions.get(idmap.get(tint));
		System.out.print("tint = "+ tint+ "\n");
		Map<String,Transition> subtrans = new HashMap<String,Transition>();
		
		int subcount = 0;
		while(tint!=9999) {
		System.out.println("Please select subtransition(s) (9999 to exit) : \n");
		showTrans();
		tint = iin.nextInt();
		
		if (tint!=9999){
		subcount++;
		subtrans.put(transitions.get(idmap.get(tint)).getId(),transitions.get(idmap.get(tint)));
		}
		}
		hiertrans.put(superTrans.getId(), subtrans);
		
		System.out.println("Add another supertransition? (0/1)");
		hint = iin.nextInt();
			
		}
		
		System.out.println("Hierarchy added:\n");
		
		for (String sup: hiertrans.keySet()) {
			System.out.println("Supertransition: "+transitions.get(sup).getName());
			
			for (String subie: hiertrans.get(sup).keySet()){
				System.out.println(hiertrans.get(sup).get(subie).getName());
			}
			System.out.print("\n");
		}
		
		} */
		
		
		makeLazySubscriptions();
		p.reset();
		final int command = p.getInteger();
		final int extension = p.getInteger();
		final int subcommand = p.getInteger();
		//System.out.println("Command :"+command+" extension: "+" subcommand :"+subcommand);
		assert command == Handler.EXTERNAL_COMMAND;
		assert extension == DeclareExtension.ID;
		Packet result;
		switch (subcommand) {
		case 1:
			result = handleCheckPage(p);
			break;
		default:
			result = new Packet(7, -1);
			result.addString("Unknown Declare command");
			break;
		}
			
		return result;
	}

	

	/**
	 * @see org.cpntools.simulator.extensions.AbstractExtension#prefilter(org.cpntools.accesscpn.engine.protocol.Packet)
	 */
	@Override
	public Packet prefilter(final Packet p) {
		//System.out.println(p.toString());
		p.reset();
		final int command = p.getInteger();
		if (command == 500) {
			final int subcommand = p.getInteger();
			switch (subcommand) {
			case 11:
				generate();
				return p;
			}
		} else if (command == 800) {
			final int subcommand = p.getInteger();
			switch (subcommand) {
			case 1:
				generate();
				return p;
			}

		}
		return p;
	}

	/**
	 * @see org.cpntools.simulator.extensions.AbstractExtension#handle(org.cpntools.accesscpn.engine.protocol.Packet,
	 *      org.cpntools.accesscpn.engine.protocol.Packet)
	 */
	@Override
	public Packet handle(final Packet p, final Packet response) {
		//System.out.println("Anything happening, perhaps here?");
		//System.out.println(p.toString());
		p.reset();
		final int command = p.getInteger();
		//System.out.println("Command :"+command);
		if (command == 500) {
			final int subcommand = p.getInteger();
			//System.out.println("Subcommand: "+ subcommand);
			switch (subcommand) {
			case 11:
				generate();
				return response;
			case 12:
				execute(p);
				return response;
			case 13:
			case 14:
				return enabled(p, response);
			case 20:
			case 21:
				reset();
				return response;
			case 35:
			case 36:
				return multipleEnabled(p, response);
			}
		} else if (command == 800) {
			final int subcommand = p.getInteger();
			switch (subcommand) {
			case 1:
				generate();
				return response;
			}

		}
		return null;
	}

	private void generate() {
		System.out.println("Anything happening before the weird concatenation?");
		final List<String> order = new ArrayList<String>(automata.keySet());
		try {
			// channel.evaluate("let exception E in if (CPN'Sim.has_filter(\"org.cpntools.simulator.extensions.declare\")) then raise E else () end");
			if (!isGenerated) {
				StringBuilder sb = new StringBuilder();
				sb.append("local val CPN'state = ref (");
				for (int i = 0; i < automata.size(); i++) {
					if (i != 0) {
						sb.append(", ");
					}
					sb.append("0");
				}
				sb.append(")\n");
				int i = 0;
				for (final String label : order) {
					final Automaton a = automata.get(label);
					i++;
					final List<Object> ts = new ArrayList<Object>(
							a.getTransitions());
					ts.remove(Automaton.OTHERWISE);
					boolean first = true;
					for (final Object t : ts) {
						if (first) {
							sb.append("fun CPN'next'");
							first = false;
						} else {
							sb.append("  | CPN'next'");
						}
						sb.append(i);
						sb.append(" \"");
						sb.append(t);
						sb.append("\" = #[");
						for (int s = a.getInit(); s < a.lastState(); s++) {
							int ss = a.next(s, t);
							if (ss < 0) {
								ss = a.next(s, Automaton.OTHERWISE);
							}
							if (s != 0) {
								sb.append(",");
							}
							sb.append(ss);
						}
						sb.append("]\n");
					}
					if (first) {
						sb.append("fun CPN'next'");
					} else {
						sb.append("  | CPN'next'");
					}
					sb.append(i);
					sb.append(" _ = #[");
					for (int s = a.getInit(); s < a.lastState(); s++) {
						final int ss = a.next(s, Automaton.OTHERWISE);
						if (s != 0) {
							sb.append(",");
						}
						sb.append(ss);
					}
					sb.append("]\n");
				}
				sb.append("fun CPN'next' (CPN't, CPN'state) = (");
				for (i = 1; i <= automata.size(); i++) {
					if (i != 1) {
						sb.append(", ");
					}
					sb.append("Vector.sub(CPN'next'");
					sb.append(i);
					sb.append(" CPN't, ");
					if (automata.size() > 1) {
						sb.append("#");
						sb.append(i);
					}
					sb.append(" CPN'state)");
				}
				sb.append(")\n");
				i = 0;
				for (final Automaton a : automata.values()) {
					i++;
					boolean first = true;
					for (int s = a.getInit(); s < a.lastState(); s++) {
						if (AcceptabilityFlavor.isImpossible(a, s)) {
							if (first) {
								sb.append("fun CPN'accept'");
								first = false;
							} else {
								sb.append("  | CPN'accept'");
							}
							sb.append(i);
							sb.append(" ");
							sb.append(s);
							sb.append(" = false\n");
						}
					}
					if (first) {
						sb.append("fun CPN'accept'");
					} else {
						sb.append("  | CPN'accept'");
					}
					sb.append(i);
					sb.append(" _ = true\n");
				}
				sb.append("fun CPN'accept' CPN'state = ");
				for (i = 1; i <= automata.size(); i++) {
					if (i != 1) {
						sb.append(" andalso ");
					}
					sb.append("(CPN'accept'");
					sb.append(i);
					sb.append(" (");
					if (automata.size() > 1) {
						sb.append("#");
						sb.append(i);
					}
					sb.append(" CPN'state))");
				}
				sb.append("\n");
				sb.append("in\nfun CPN'check' (CPN't, _) = CPN'accept' (CPN'next' (CPN't, !CPN'state))\n");
				sb.append("fun CPN'execute' (CPN't, _) = CPN'state := (CPN'next' (CPN't, !CPN'state))\n");
				sb.append("fun CPN'get_state () = !CPN'state\n");
				sb.append("fun CPN'set_state CPN's = CPN'state := CPN's\n");
				sb.append("fun CPN'reset' () = CPN'state := (");
				for (i = 0; i < automata.size(); i++) {
					if (i != 0) {
						sb.append(", ");
					}
					sb.append("0");
				}
				sb.append(")\n");

				sb.append("end;\n");
				sb.append("CPN'Sim.add_filter (\"org.cpntools.simulator.extensions.declare\", { check = CPN'check', execute = CPN'execute', reset = CPN'reset' })");
				// System.out.println(sb);
				try {
					channel.evaluate(sb.toString());
				} catch (final Exception e) {
					e.printStackTrace();
				}
				isGenerated = true;
				sb = new StringBuilder();
				sb.append("CPN'set_state (");
				boolean first = true;
				for (final String label : order) {
					if (!first) {
						sb.append(", ");
					}
					first = false;
					sb.append(states.get(label));
				}
				sb.append(")");
				System.out.println(sb.toString());
				try {
					channel.evaluate(sb.toString());
				} catch (final Exception e) {
					// Ignore
				}
			}
		} catch (final Exception _) {
			// Ignore
		}
	}

	private boolean acceptable(final Automaton a, final int state,
			final Object transition) {
		final int next = execute(transition, a, state);
		if (next < 0)
			return false;
		return !AcceptabilityFlavor.isImpossible(a, next);
	}

	private Packet enabled(final Packet p, final Packet response) {
		//System.out.println(p.toString());
		if (isGenerated)
			return response;
		response.reset();
		p.reset();
		// System.out.println("-----------------------------------");
		if (response.getBoolean()) {
			final Packet result = new Packet(7, 1);
			result.addBoolean(enabled(p.getString(), p.getInteger()));
			// System.out.println("-----------------------------------");
			return result;
		}
		// System.out.println("Rejecting " + p.getString());
		// System.out.println("-----------------------------------");
		return response;
	}

	private boolean enabled(final String string, final int integer) {
		//System.out.print("Enabled? " + string + " - ");
		final Object task = getTask(string);
		for (final String pageId : new ArrayList<String>(automata.keySet())) {
			final Automaton a = automata.get(pageId);
			final int state = states.get(pageId);
			//System.out.print(state + " ");
			if (!acceptable(a, state, task))  {//System.out.println("= false");
				return false;}
		}
		//System.out.println("= true");
		return true;
	}

	private void execute(final Packet p) {
		//System.out.println(p.toString());
		p.reset();
		final String taskId = p.getString();
		//System.out.println("Command "+p.getCommand());
	    //System.out.print(taskId + ": (");
		trace.add(taskId);
		if (isGenerated)
			return;
		final Object task = getTask(taskId);
		// boolean first = true;
		for (final String pageId : new ArrayList<String>(automata.keySet())) {
			final Automaton a = automata.get(pageId);
			final int state = states.get(pageId);
			final int next = execute(task, a, state);
			//System.out.println(state + " -> " + next);
			// if (!first) {
			// System.out.print(", ");
			// }
			// first = false;
			states.put(pageId, next);
		}
		// System.out.println(")");
	}

	Object getTask(final String taskId) {
		Object task = tasks.get(taskId);
		if (task == null) {
			task = Automaton.OTHERWISE;
		}
		return task;
	}

	int execute(final Object task, final Automaton a, final int state) {
		int next = a.next(state, task);
		//System.out.println(a.toString());
		
		if (next < 0) {
			next = a.next(state, Automaton.OTHERWISE);
		}
		return next;
	}

	private Packet handleCheckPage(final Packet p) {

		
	
		
		final Packet result = new Packet(7, 1);
		try {
			channel.evaluate("CPN'Sim.remove_filter \"org.cpntools.simulator.extensions.declare\"");
			isGenerated = false;
			p.reset();
			p.getInteger(); // command
			p.getInteger(); // extension
			p.getInteger(); // subcmd
			final int count = p.getInteger();
			final String pageId = p.getString();
			Module m = modules.get(pageId);
			if (m == null) {
				m = new Module();
				modules.put(pageId, m);
			}
			
			//GUIIni();
			//frame.setVisible(false);
			for (int i = 0; i < count; i++) {
				String ta = null;
				String tb = null;
				Task taska = null;
				Task taskb = null;

				int k=0;
				
				final int parameters = p.getInteger();
				final String id = p.getString();
				final String name = p.getString();
				final String formula = p.getString();
				final String inscription = p.getString(); // inscription
		/******///		System.out.println("ID : "+id+" Name: -"+name+"- formula: "+formula+" parameters: "+parameters+" inscription: "+inscription);
				final Constraint c = new Constraint(name, formula, parameters);
								
								
				for (int j = 0; j < parameters; j++) {
					final String tid = p.getString();
									
					Task t = tasks.get(tid);
					if (t == null) {
						t = new Task();
						t.setName(tid);
						tasks.put(tid, t);
					}
					if (parameters == 2 && j == 0) {
						ta = tid;
						taska = t;
					}
					if (parameters == 2 && j == 1) {
						tb = tid;
						taskb = t;
					}

					c.setParameters(j, t);
					k = j;
				}
				m.addConstraint(id, c);
				
				if (hiertrans.get(ta) != null && !name.equals("init") && !name.equals("absence")) {
			
					
					Map<String,Transition> supermap = new HashMap<String, Transition>();
					Transition subtrans;
		
					supermap = hiertrans.get(ta);
					
					for (String transname : supermap.keySet()) {
						Task tsub = new Task();

						subtrans = (Transition) supermap.get(transname);
						/*System.out.println("Sub transition "
								+ subtrans.getName());*/
						if (tasks.get(subtrans.getId()) != null) {
							// System.out.println("We have a match");
							tsub = tasks.get(subtrans.getId());
						} else {
							tsub.setName(subtrans.getId());
							tasks.put(subtrans.getId(), tsub);
						}
						Constraint csub = new Constraint(name, formula,
								parameters);
						if (k == 0) {
							csub.setParameters(0, tsub);
						} else {
							csub.setParameters(0, tsub);
							csub.setParameters(1, taskb);
						}
						m.addConstraint(subtrans.getId() + transname + i, csub);

						if (hiertrans.get(tb) != null) {
							Map<String,Transition> supermapb = new HashMap<String, Transition>();
							Transition subtransb;
							supermapb = hiertrans.get(tb);
							/*System.out.println("Supermapb size is "
									+ supermapb.size());*/
							
							for (String transbname: supermapb.keySet()){
								Task tsubb = new Task();
								subtransb = (Transition) supermapb.get(transbname);
								/*System.out.println("Sub transition "
										+ subtransb.getName());*/
								if (tasks.get(subtransb.getId()) != null)
									tsubb = tasks.get(subtransb.getId());
								else {
									tsubb.setName(subtransb.getId());
									tasks.put(subtransb.getId(), tsubb);
								}
								Constraint csubb = new Constraint(name,
										formula, parameters);
								csubb.setParameters(0, tsub);
								csubb.setParameters(1, tsubb);								
								m.addConstraint(subtrans.getId() + transbname + i, csubb);
							}
						}
						
					}
				}
			
			}
								
			final int removeCount = p.getInteger();
			for (int i = 0; i < removeCount; i++) {
				m.removeConstraint(p.getString());
			}
			if (m.count() == 0) {
				modules.remove(pageId);
				states.remove(pageId);
				automata.remove(pageId);
				result.addBoolean(true);
				result.addInteger(0);
				return result;
			}
			final Map<RegExp<Task>, Constraint> formulae = Translator.INSTANCE
					.parse(m);
			final Automaton automaton = Translator.INSTANCE
					.translateRaw(formulae);
			Translator.INSTANCE.colorAutomaton(automaton);
			int state = automaton.getInit();
			for (final String taskId : trace) {
				final Object task = getTask(taskId);
				//System.out.println("Try it from here: "+task.toString());
				state = execute(task, automaton, state);
			}
			states.put(pageId, state);
			automata.put(pageId, automaton);
			result.addBoolean(true);
			result.addInteger(0);
			//System.out.println(automaton);
		} catch (final Exception e) {
			e.printStackTrace();
			result.addBoolean(false);
			result.addString(e.toString());
		}
		return result;
	}

	private Packet multipleEnabled(final Packet p, final Packet response) {
		p.reset();
		response.reset();
		if (response.getInteger() != 1)
			return response;
		final Packet result = new Packet(7, 1);
		p.getInteger();
		p.getInteger(); // Skip command and subcmd
		final int count = p.getInteger();
		result.addInteger(count);
		
		// System.out.println("=================================== " + count);
		for (int i = 0; i < count; i++) {
			if (response.getBoolean()) {
				result.addBoolean(enabled(p.getString(), p.getInteger()));
			} else {
				p.getString();
				p.getInteger();
				result.addBoolean(false);
				// System.out.println("Rejecting " + taskId);
			}
			result.addString(response.getString());
		}
		 //System.out.println("=================================== " + count);
		return result;
	}

	private void reset() {
		 System.out.println("Reset");
		trace.clear();
		for (final String page : new ArrayList<String>(modules.keySet())) {
			states.put(page, automata.get(page).getInit());
		}
	}

}
