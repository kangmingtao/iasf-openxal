//
// MonitorController.java
// Open XAL
//
// Created by Pelaia II, Tom on 8/29/12
// Copyright 2012 Oak Ridge National Lab. All rights reserved.
//

package xal.app.launcher;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;

import xal.application.Application;
import xal.tools.bricks.*;
import xal.tools.swing.KeyValueFilteredTableModel;
import xal.tools.dispatch.*;


/** MonitorController */
public class MonitorController implements MonitorModelListener {
	/** The main model of this document */
	final private LaunchModel LAUNCH_MODEL;

	/** model for monitoring remote applications */
	final private MonitorModel MONITOR_MODEL;

	/** table of live applications to monitor */
	final private JTable REMOTE_APPS_TABLE;

	/** table model for displaying the applications */
	final private KeyValueFilteredTableModel<RemoteAppRecord> APP_TABLE_MODEL;

	/** filter field */
	final private JTextField FILTER_FIELD;

	/** timer for refreshing the data */
	final private DispatchTimer REFRESH_TIMER;


	/** Constructor */
	public MonitorController( final LaunchModel launchModel, final WindowReference windowReference ) {
		LAUNCH_MODEL = launchModel;

		MONITOR_MODEL = new MonitorModel();
		MONITOR_MODEL.addMonitorModelListener( this );

		final JButton liveAppsFilterClearButton = (JButton)windowReference.getView( "LiveAppsFilterClearButton" );
		liveAppsFilterClearButton.addActionListener( clearFilterAction() );

		final JButton forceQuitAppButton = (JButton)windowReference.getView( "ForceQuitAppButton" );
		forceQuitAppButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				for ( final RemoteAppRecord record : getSelectedRemoteAppRecords() ) {
					try {
						record.forceQuit( 0 );
					}
					catch ( Exception exception ) {
						exception.printStackTrace();
						Application.displayError( "Force Quit App Failed!", "Failed to force app to quit due to an internal exception!", exception );
					}
				}
			}
		});

		final JButton revealAppButton = (JButton)windowReference.getView( "RevealAppButton" );
		revealAppButton.addActionListener( new ActionListener() {
			public void actionPerformed( final ActionEvent event ) {
				for ( final RemoteAppRecord record : getSelectedRemoteAppRecords() ) {
					try {
						record.showAllWindows();
					}
					catch ( Exception exception ) {
						exception.printStackTrace();
						Application.displayError( "Reveal App Failed!", "Failed to reveal app due to an internal exception!", exception );
					}
				}
			}
		});

		FILTER_FIELD = (JTextField)windowReference.getView( "LiveAppsFilterField" );
		FILTER_FIELD.putClientProperty( "JTextField.variant", "search" );
		FILTER_FIELD.putClientProperty( "JTextField.Search.Prompt", "Application Filter" );

		REMOTE_APPS_TABLE = (JTable)windowReference.getView( "LiveAppsTable" );
		REMOTE_APPS_TABLE.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
		REMOTE_APPS_TABLE.setAutoCreateRowSorter( true );
		REMOTE_APPS_TABLE.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		
		APP_TABLE_MODEL = new KeyValueFilteredTableModel<RemoteAppRecord>( new ArrayList<RemoteAppRecord>(), "applicationName", "hostName", "totalMemory", "launchTime", "lastUpdate" );
		APP_TABLE_MODEL.setMatchingKeyPaths( "applicationName", "hostName" );
		APP_TABLE_MODEL.setColumnName( "applicationName", "Application" );
		APP_TABLE_MODEL.setInputFilterComponent( FILTER_FIELD );
		REMOTE_APPS_TABLE.setModel( APP_TABLE_MODEL );

		REFRESH_TIMER = new DispatchTimer( DispatchQueue.getMainQueue(), new Runnable() {
			public void run() {
				final int rowCount = APP_TABLE_MODEL.getRowCount();
				if ( rowCount > 0 ) {
					APP_TABLE_MODEL.fireTableRowsUpdated( 0, rowCount - 1 );
				}
			}
		});
		REFRESH_TIMER.startNowWithInterval( 5000, 0 );	// refresh the table every 5 seconds
	}


	/** event indicating that the list of remote apps has changed */
	public void remoteAppsChanged( final MonitorModel model, final List<RemoteAppRecord> remoteApps ) {
		DispatchQueue.getMainQueue().dispatchAsync( new Runnable() {
			public void run() {
				APP_TABLE_MODEL.setRecords( remoteApps );
			}
		});
	}


	/** Get the remote app records selected by the user */
	private RemoteAppRecord[] getSelectedRemoteAppRecords() {
		final int[] selectedRows = REMOTE_APPS_TABLE.getSelectedRows();
		final RemoteAppRecord[] selectedRecords = new RemoteAppRecord[ selectedRows.length ];

		int recordIndex = 0;
		for ( final int row : selectedRows ) {
			final int modelRow = REMOTE_APPS_TABLE.convertRowIndexToModel( row );
			selectedRecords[ recordIndex++ ] = APP_TABLE_MODEL.getRecordAtRow( modelRow );
		}

		return selectedRecords;
	}
	

	/** action to clear the filter field */
	private AbstractAction clearFilterAction() {
		return new AbstractAction() {
            private static final long serialVersionUID = 1L;

			public void actionPerformed( final ActionEvent event ) {
				FILTER_FIELD.setText( "" );
			}
		};
	}
}
