/*
 * PVLoggerWindow.java
 *
 * Created on Wed Dec 3 15:00:00 EDT 2003
 *
 * Copyright (c) 2003 Spallation Neutron Source
 * Oak Ridge National Laboratory
 * Oak Ridge, TN 37830
 */

package xal.app.pvlogger;

import xal.application.*;
import xal.smf.application.*;
import xal.service.pvlogger.*;
import xal.tools.data.*;

import java.util.Iterator;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.Vector;
import java.util.ArrayList;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;


/**
 * PVLoggerWindow
 *
 * @author  tap
 */
class PVLoggerWindow extends AcceleratorWindow implements SwingConstants, DataKeys, ScrollPaneConstants {
	/** required UID for serialization */
	static final long serialVersionUID = 1L;

	/** date formatter for displaying timestamps */
	static final protected DateFormat TIMESTAMP_FORMAT;
	
	/** Table of loggers running on the local network */
	protected JTable loggerTable;
	
	/** List of session group types */
	@SuppressWarnings( "rawtypes" )		// TODO: JList is only typed in Java 7 or later
	protected JList _groupTypesListView;
	
	/** action to publish snapshots for the selected PV Logger */
	protected Action _publishSnapshotsOnSelectionAction;
	
	/** Table selection action to restart the selected pvlogger */
	protected Action restartSelectionAction;
	
	/** Table selection action to shutdown the selected pvlogger */
	protected Action shutdownSelectionAction;
	
	/** Table selection action to stop the selected loggers from logging */
	protected Action stopLoggingSelectionAction;
	
	/** Table selection action to start the selected loggers logging */
	protected Action resumeLoggingSelectionAction;
	
	/** Field for entering and displaying the update period */
	protected JTextField periodField;
	
	/** Label for displaying the latest log event */
	protected JLabel latestLogDateField;
	
	/** Text view that displays the latest log output */
	protected JTextArea latestLogTextView;
	
	/** List view displaying connected PVs */
	@SuppressWarnings( "rawtypes" )		// TODO: JList is only typed in Java 7 or later
	protected JList _connectedPVList;
	
	/** List view displaying unconnected PVs */
	@SuppressWarnings( "rawtypes" )		// TODO: JList is only typed in Java 7 or later
	protected JList _unconnectedPVList;
	
	/** field displaying whether the logger is active */
	protected JLabel _loggingStatusField;
	
	/** field displaying whether the logger's logging period */
	protected JLabel _loggingPeriodField;
	
	/** button for taking a snapshot of the currently selected PV Logger group */
	protected JButton SNAPSHOT_BUTTON;
	
	/** snapshot comment field */
	protected JTextField SNAPSHOT_COMMENT_FIELD;
	
	/** snapshot field for displaying the resulting snaspshot ID */
	protected JLabel SNAPSHOT_RESULT_FIELD;
	
	/** main application wide model */
	protected LoggerModel _mainModel;
	
	/** model for this window's document */
	protected DocumentModel _model;
	
	
	/**
	 * static initializer
	 */
	static {
		TIMESTAMP_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
	}
	
	
    /** Creates a new instance of MainWindow */
    public PVLoggerWindow(PVLoggerDocument aDocument, LoggerTableModel loggerTableModel) {
        super(aDocument);
        setSize(900, 600);
		_model = aDocument.getModel();
		_mainModel = _model.getMainModel();
		
		SNAPSHOT_BUTTON = new JButton( "Take Snapshot" );
		SNAPSHOT_BUTTON.setEnabled( false );
		SNAPSHOT_COMMENT_FIELD = new JTextField( "", 40 );
		SNAPSHOT_COMMENT_FIELD.setMaximumSize( SNAPSHOT_COMMENT_FIELD.getPreferredSize() );
		SNAPSHOT_RESULT_FIELD = new JLabel( "" );

		makeContent(loggerTableModel);
		handleLoggerEvents();
		_mainModel.updateServiceList();
    }
	
	
	/** Listen for new logger status events and update the views accordingly */
	protected void handleLoggerEvents() {
		_model.addDocumentModelListener( new DocumentModelListener() {
			/**
			 * Notification that a new logger has been selected
			 * @param source the document model managing selections
			 * @param handler the latest handler selection or null if none is selected
			 */
			public void handlerSelected(DocumentModel source, LoggerHandler handler) {
				updateLoggerInspector();
				updateControls();
		}
	
	
			/**
			 * Notification that a new logger session has been selected
			 * @param source the document model managing selections
			 * @param handler the latest session handler selection or null if none is selected
			 */
			public void sessionHandlerSelected(DocumentModel source, LoggerSessionHandler handler) {
				updateChannelsInspector();
				updateLogText();
				updateLoggerInfo();
				updateSnapshotView();
			}
			
			
			/**
			 * Notification that the channels of the selected logger have changed
			 * @param model the document model managing selections
			 * @param channelRefs the latest channel refs containing the channel information
			 */
			public void channelsChanged(DocumentModel model, java.util.List channelRefs) {
				updateChannelsInspector();
			}
			
			
			/**
			 * Notification that a new machine snapshot has been published
			 * @param model the document model managing selections
			 * @param timestamp the timestamp of the latest machine snapshot
			 * @param snapshotDump the textual dump of the latest machine snapshot
			 */
			public void snapshotPublished(DocumentModel model, Date timestamp, String snapshotDump) {
				updateLogText();
			}
			
			
			/**
			 * Notification that a logger record has been updated
			 * @param model the document model managing selections
			 * @param record the updated logger record
			 */
			public void recordUpdated(DocumentModel model, GenericRecord record) {}
	
	
			/**
			 * Notification that a logger session has been updated
			 * @param model the document model managing selections
			 * @param source the updated logger session
			 */
			public void loggerSessionUpdated(DocumentModel model, LoggerSessionHandler source) {
				updateLoggerInfo();
			}
		});
	}
	
	
	/**
	 * Determine whether to display the toolbar.
	 * @return true to display the toolbar and false otherwise.
	 */
	public boolean usesToolbar() {
		return true;
	}
	
	
	/** Update the controls to reconcile it with the model. */
	protected void updateControls() {
		int oldPeriod;
		try {
			oldPeriod = Integer.parseInt( periodField.getText() );
		}
		catch(Exception exception) {
			oldPeriod = 0;
		}
		
		final int period = _mainModel.getUpdatePeriod();
		if ( period != oldPeriod ) {
			periodField.setText( String.valueOf(period) );
			periodField.selectAll();
		}
		
		boolean hasSelectedHandler = _model.getSelectedHandler() != null;
		_publishSnapshotsOnSelectionAction.setEnabled( hasSelectedHandler );
		shutdownSelectionAction.setEnabled( hasSelectedHandler );
		restartSelectionAction.setEnabled( hasSelectedHandler );
		stopLoggingSelectionAction.setEnabled( hasSelectedHandler );
		resumeLoggingSelectionAction.setEnabled( hasSelectedHandler );
	}
	
	
	/** Update the logger inspector for the selected logger */
	protected void updateLoggerInspector() {
		updateGroupListView();
		updateLoggerInfo();
		updateChannelsInspector();
		updateLogText();
		updateSnapshotView();
	}
	
	
	/** update the snapshot tab */
	protected void updateSnapshotView() {
		final LoggerSessionHandler session = _model.getSelectedSessionHandler();
		final boolean isLogging = session != null ? session.isLogging() : false;
		SNAPSHOT_BUTTON.setEnabled( isLogging );
		SNAPSHOT_COMMENT_FIELD.setText( "" );
		SNAPSHOT_RESULT_FIELD.setText( "" );
	}
	
	
	/** Update information about the remote logger including logging period and logger state. */
	protected void updateLoggerInfo() {
		LoggerSessionHandler session = _model.getSelectedSessionHandler();
		
		String status = (session != null) ? String.valueOf( session.isLogging() ) : "false";
		_loggingStatusField.setText(status);
		
		String periodText = (session != null) ? String.valueOf( session.getLoggingPeriod() ) : "0";
		_loggingPeriodField.setText(periodText);
	}
	
	
	/** Update the list of connected and disconnected channels for the selected logger handler */
	@SuppressWarnings( "unchecked" )		// TODO: JList is only typed in Java 7 or later
	protected void updateChannelsInspector() {
		LoggerSessionHandler handler = _model.getSelectedSessionHandler();
		if ( handler != null ) {
			final Vector<ChannelRef> connectedPVs = new Vector<ChannelRef>();
			final Vector<ChannelRef> unconnectedPVs = new Vector<ChannelRef>();
			final Collection<ChannelRef> channelRefs = handler.getChannelRefs();
			for ( final ChannelRef channelRef : channelRefs ) {
				if ( channelRef.isConnected() ) {
					connectedPVs.add(channelRef);
				}
				else {
					unconnectedPVs.add(channelRef);
				}
			}
			Collections.sort( connectedPVs );
			Collections.sort( unconnectedPVs );
			_connectedPVList.setListData( connectedPVs );
			_unconnectedPVList.setListData( unconnectedPVs );
		}
		else {
			_connectedPVList.setListData( new Vector<ChannelRef>() );
			_unconnectedPVList.setListData( new Vector<ChannelRef>() );
		}
	}
	
	
	/** Update the the text of the latest log from the selected logger handler */
	protected void updateLogText() {
		LoggerSessionHandler handler = _model.getSelectedSessionHandler();
		if ( handler != null ) {
			latestLogTextView.setText( handler.getLastPublishedSnapshotDump() );
			latestLogTextView.setCaretPosition(0);
			
			String dateText = TIMESTAMP_FORMAT.format( handler.getTimestampOfLastPublishedSnapshot() );
			latestLogDateField.setText(dateText);
		}
		else {
			latestLogTextView.setText("");
			latestLogDateField.setText("");
		}
	}
	
	
	/** Update the list of logger sessions identified by group */
	@SuppressWarnings( "unchecked" )		// TODO: JList is only typed in Java 7 or later
	protected void updateGroupListView() {
		final LoggerHandler handler = _model.getSelectedHandler();
		final Vector<String> groups = ( handler != null ) ? new Vector<String>( handler.getGroupTypes() ) : new Vector<String>();
		Collections.sort( groups );
		_groupTypesListView.setListData( groups );
	}
	
	
	/**
	 * Build the component contents of the window.
	 * @param loggerTableModel The table model for the logger view
	 */
	protected void makeContent(final LoggerTableModel loggerTableModel) {
		Box mainView = new Box(VERTICAL);
		getContentPane().add(mainView);
		
		mainView.add( makePeriodView() );
		Box loggerPanel = new Box(HORIZONTAL);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		splitPane.setOneTouchExpandable(true);
		splitPane.setTopComponent( makeLoggerTable(loggerTableModel) );
		splitPane.setBottomComponent( makeLoggerInspector() );
		splitPane.setResizeWeight(0.5);
		loggerPanel.add(splitPane);
		mainView.add(loggerPanel);
		
		updateControls();
		updateLoggerInspector();
	}
	
	
	/**
	 * Make a view that displays the period of updates and allows the user to change that period
	 * @return the period view
	 */
	protected JComponent makePeriodView() {
		JPanel periodPanel = new JPanel();
		periodPanel.setLayout( new FlowLayout(FlowLayout.LEFT) );
		periodPanel.add( new JLabel("update period (sec): ") );
		periodField = new JTextField(2);
		periodField.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				applyPeriodSetting();
			}
		});
		periodField.addFocusListener( new FocusAdapter() {
			public void focusGained(FocusEvent event) {
				periodField.selectAll();
			}
			public void focusLost(FocusEvent event) {
				updateControls();
				periodField.setCaretPosition(0);
				periodField.moveCaretPosition(0);
			}
		});
		
		periodField.setHorizontalAlignment(RIGHT);
		periodPanel.add(periodField);
		periodPanel.setBorder( new EtchedBorder() );
		periodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10+periodField.getHeight()) );
		
		return periodPanel;
	}
	
	
	/**
	 * Make a table that lists the currently running loggers
	 * @param tableModel The table model
	 * @return the table view
	 */
	protected JComponent makeLoggerTable(final LoggerTableModel tableModel) {
		loggerTable = new JTable(tableModel);
		loggerTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		TableCellRenderer numericCellRenderer = makeNumericCellRenderer();
		TableColumnModel columnModel = loggerTable.getColumnModel();
    
        JScrollPane loggerScrollPane = new JScrollPane(loggerTable);
		loggerScrollPane.setColumnHeaderView( loggerTable.getTableHeader() );
		loggerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        loggerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		return loggerScrollPane;
	}
	
	
	/**
	 * Make an inspector for a selected logger
	 * @return the inspector view
	 */
	protected JComponent makeLoggerInspector() {
		JTabbedPane tabPane = new JTabbedPane();
		
		loggerTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if ( event.getValueIsAdjusting() )  return;
				int selectedRow = loggerTable.getSelectedRow();
				if ( selectedRow >= 0 ) {
					LoggerTableModel tableModel = (LoggerTableModel)loggerTable.getModel();
					String id = tableModel.getRecord(selectedRow).stringValueForKey(ID_KEY);
					_model.setSelectedHandler( _mainModel.getHandler(id) );
				}
				else {
					_model.setSelectedHandler(null);
				}
			}
		});
		
		tabPane.addTab( "Info", makeInfoTab() );
		tabPane.addTab( "Latest Log", makeLatestLogTab() );
		tabPane.addTab( "PVs", makePVTab() );
		tabPane.addTab( "Snapshot", makeSnapshotTab() );
		
		JSplitPane inspector = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, makeSessionListView(), tabPane);
		
		return inspector;
	}
	
	
	/**
	 * Make the session list view which displays the list of logger sessions for the selected
	 * logger handler.
	 * @return the view which displays the logger sessions
	 */
	@SuppressWarnings( "rawtypes" )		// TODO: JList is only typed in Java 7 or later
	protected JComponent makeSessionListView() {
		Box view = new Box(BoxLayout.Y_AXIS);
		
		Box labelRow = new Box(BoxLayout.X_AXIS);
		labelRow.add( Box.createHorizontalGlue() );
		labelRow.add( new JLabel("Logger Groups") );
		labelRow.add( Box.createHorizontalGlue() );
		view.add(labelRow);
		
		_groupTypesListView = new JList();
		view.add( new JScrollPane(_groupTypesListView) );
		_groupTypesListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		_groupTypesListView.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if ( !event.getValueIsAdjusting() ) {
					String selectedGroup = (String)_groupTypesListView.getSelectedValue();
					LoggerHandler handler = _model.getSelectedHandler();
					LoggerSessionHandler sessionHandler = ( handler != null ) ? handler.getLoggerSession(selectedGroup) : null;
					_model.setSelectedSessionHandler(sessionHandler);
				}
			}
		});
		
		return view;
	}
	
	
	/** make and return the tab for taking a snapshot */
	protected JComponent makeSnapshotTab() {
		final Box view = new Box( VERTICAL );
		
		final Box commentRow = new Box( HORIZONTAL );
		view.add( commentRow );
		commentRow.add( new JLabel("Comment: ") );
		commentRow.add( SNAPSHOT_COMMENT_FIELD );
		commentRow.add( Box.createHorizontalGlue() );
				
		final Box buttonRow = new Box( HORIZONTAL );
		SNAPSHOT_BUTTON.addActionListener( new ActionListener() {
			 public void actionPerformed( final ActionEvent event ) {
				 final String userComment = SNAPSHOT_COMMENT_FIELD.getText();
				 final String comment = userComment != null ? userComment : " ";
				 final int snapshotID = takeAndPublishSnapshot( event, comment );
			 }
		});
		buttonRow.add( SNAPSHOT_BUTTON );
		buttonRow.add( SNAPSHOT_RESULT_FIELD );
		buttonRow.add( Box.createHorizontalGlue() );
		view.add( buttonRow );
		
		return view;
	}
	
	
	/** take and publish a snapshot for the currently selected group */
	private int takeAndPublishSnapshot( final ActionEvent event, final String comment ) {
		final LoggerSessionHandler session = _model.getSelectedSessionHandler();
		final boolean isLogging = session != null ? session.isLogging() : false;
		if ( isLogging ) {
			final int snapshotID = session.takeAndPublishSnapshot( comment );
			if ( snapshotID > 0 ) {
				SNAPSHOT_RESULT_FIELD.setText( " Snapshot: " + snapshotID );
			}
			else {
				SNAPSHOT_RESULT_FIELD.setText( " Failed snapshot!" );
			}
			return snapshotID;
		}
		else {
			return 0;
		}
	}
	
	
	/**
	 * Make a tab that displays the information about the latest log
	 * @return the tab view
	 */
	protected JComponent makeLatestLogTab() {
		Box view = new Box(VERTICAL);
		
		Box row = new Box(HORIZONTAL);
		row.add( new JLabel("Time: ") );
		latestLogDateField = new JLabel();
		row.setMinimumSize( new Dimension(0, 3*row.getPreferredSize().height) );
		row.setMaximumSize( new Dimension(Integer.MAX_VALUE, 3*row.getPreferredSize().height) );
		row.add( latestLogDateField );
		row.add( Box.createGlue() );
		view.add(row);
		
		latestLogTextView = new JTextArea();
		latestLogTextView.setEditable(false);
		view.add( new JScrollPane(latestLogTextView, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER) );
		
		return view;
	}
	
	
	/**
	 * Make a tab that displays the pvs being logged and distinguishes those that are connected
	 * from those that are not connected.
	 * @return the tab view
	 */
	@SuppressWarnings( "rawtypes" )		// TODO: JList is only typed in Java 7 or later
	protected JComponent makePVTab() {
		Box view = new Box(HORIZONTAL);
		
		Box pvBox = new Box(VERTICAL);
		
		pvBox.add( new JLabel("Connected PVs:") );
		_connectedPVList = new JList();
		pvBox.add( new JScrollPane(_connectedPVList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER) );
		view.add(pvBox);
		
		pvBox = new Box(VERTICAL);
		pvBox.add( new JLabel("Unconnected PVs:") );
		_unconnectedPVList = new JList();
		pvBox.add( new JScrollPane(_unconnectedPVList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER) );
		view.add(pvBox);
		
		return view;
	}
	
	
	/**
	 * Make a tab that displays basic information about the pvlogger session.
	 * @return the tab view
	 */
	protected JComponent makeInfoTab() {
		final Box tabView = new Box( BoxLayout.Y_AXIS );
		
		final Box loggingStatusRow = new Box( BoxLayout.X_AXIS );
		tabView.add( loggingStatusRow );
		loggingStatusRow.add( new JLabel( "Logging Active: " ) );
		_loggingStatusField = new JLabel();
		_loggingStatusField.setForeground( Color.blue );
		loggingStatusRow.add( _loggingStatusField );
		loggingStatusRow.add( Box.createHorizontalGlue() );
		
		final Box loggingPeriodRow = new Box( BoxLayout.X_AXIS );
		tabView.add( loggingPeriodRow );
		loggingPeriodRow.add( new JLabel( "Logging Period(sec): " ) );
		_loggingPeriodField = new JLabel();
		_loggingPeriodField.setForeground( Color.blue );
		loggingPeriodRow.add( _loggingPeriodField );
		loggingPeriodRow.add( Box.createHorizontalGlue() );
		
		final JButton reloadButton = new JButton( "Reload" );
		reloadButton.setToolTipText( "Reload from the database the logger properties and channels for this group." );
		reloadButton.addActionListener( new ActionListener() { 
			public void actionPerformed( final ActionEvent event ) { reloadSelectedLoggerSession(); }
		} );
		final Box buttonRow = new Box( BoxLayout.X_AXIS );
		buttonRow.add( reloadButton );
		buttonRow.add( Box.createHorizontalGlue() );
		tabView.add( buttonRow );
		
		return tabView;
	}
	
	
	/** reload the properties and channels for the currently selected logger session */
	public void reloadSelectedLoggerSession() {
		final LoggerSessionHandler session = _model.getSelectedSessionHandler();
		if ( session != null ) {
			session.reloadFromDatabase();
		}
	}
	
	
	/**
	 * Convenience method for getting the document as an instance of HistoryDocument.
	 * @return The document cast as an instace of HistoryDocument.
	 */
	public PVLoggerDocument getDocument() {
		return (PVLoggerDocument)document;
	}
	
	
	/**
	 * Get the logger model
	 * @return The logger model
	 */
	public DocumentModel getModel() {
		return getDocument().getModel();
	}
	
	
	/**
	 * Apply to the model the period setting from the period field.
	 */
	protected void applyPeriodSetting() {
		try {
			int period = Integer.parseInt( periodField.getText() );
			period = Math.max(period, 1);
			period = Math.min(period, 99);
			_mainModel.setUpdatePeriod(period);
		}
		catch(NumberFormatException exception) {
			Toolkit.getDefaultToolkit().beep();
		}
		updateControls();
	}
    
    
    /**
     * Right justify text associated with numeric values.
     * @return A renderer for numeric values.
     */
    private TableCellRenderer makeNumericCellRenderer() {
        return new DefaultTableCellRenderer() {
			/** required UID for serialization */
			static final long serialVersionUID = 1L;
			
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(RIGHT);
                return label;
            }
        };
    }
    
    
    /**
     * Register actions specific to this window instance. 
     * @param commander The commander with which to register the custom commands.
     */
    protected void customizeCommands(Commander commander) {		
		// setup the start logging selection action
        resumeLoggingSelectionAction = new AbstractAction("resume-logging-selections") {
			/** required UID for serialization */
			static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent event) {
				resumeLoggingSelections();
            }
		};
		commander.registerAction(resumeLoggingSelectionAction);
		
		// setup the stop logging selection action
        stopLoggingSelectionAction = new AbstractAction("stop-logging-selections") {
			/** required UID for serialization */
			static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent event) {
				stopLoggingSelections();
            }
		};
		commander.registerAction(stopLoggingSelectionAction);
		
		// publish snapshots on the selected PV Logger
        _publishSnapshotsOnSelectionAction = new AbstractAction( "publish-snapshots-selections" ) {
			/** required UID for serialization */
			static final long serialVersionUID = 1L;
			
			public void actionPerformed( final ActionEvent event ) {
				publishSnapshots();
            }
		};
		commander.registerAction( _publishSnapshotsOnSelectionAction );
				
		
		// setup the restart selection action
        restartSelectionAction = new AbstractAction("restart-selections") {
			/** required UID for serialization */
			static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent event) {
				final String message = "Are you sure you want to restart the selected services?";
				int result = JOptionPane.showConfirmDialog(PVLoggerWindow.this, message, "Careful!", JOptionPane.YES_NO_OPTION);
				if ( result == JOptionPane.YES_OPTION ) {
					restartSelections();
				}
            }
		};
		commander.registerAction(restartSelectionAction);
		
		// setup the shutdown selection action
        shutdownSelectionAction = new AbstractAction("shutdown-selections") {
			/** required UID for serialization */
			static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent event) {
				final String message = "Are you sure you want to shutdown the selected services?";
				int result = JOptionPane.showConfirmDialog(PVLoggerWindow.this, message, "Careful!", JOptionPane.YES_NO_OPTION);
				if ( result == JOptionPane.YES_OPTION ) {
					shutdownSelections();
				}
            }
		};
		commander.registerAction(shutdownSelectionAction);
	}
	
	
	/** Shutdown the loggers corresponding to the selected rows of the logger table. */
	public void shutdownSelections() {
		((LoggerTableModel)loggerTable.getModel()).shutdownSelections( loggerTable.getSelectedRows() );
	}
	
	
	/** publish snapshots on the selected PV Loggers */
	public void publishSnapshots() {
		((LoggerTableModel)loggerTable.getModel()).publishSnapshots( loggerTable.getSelectedRows() );
	}
	
	
	/** Restart the selected loggers by stopping them, reloading their groups and restarting them. */
	public void restartSelections() {
		((LoggerTableModel)loggerTable.getModel()).restartSelections( loggerTable.getSelectedRows() );
	}
	
	
	/** Start the selected loggers logging. */
	public void resumeLoggingSelections() {
		((LoggerTableModel)loggerTable.getModel()).resumeLoggingSelections( loggerTable.getSelectedRows() );
	}
	
	
	/** Stop the selected loggers logging. */
	public void stopLoggingSelections() {
		((LoggerTableModel)loggerTable.getModel()).stopLoggingSelections( loggerTable.getSelectedRows() );
	}
}

