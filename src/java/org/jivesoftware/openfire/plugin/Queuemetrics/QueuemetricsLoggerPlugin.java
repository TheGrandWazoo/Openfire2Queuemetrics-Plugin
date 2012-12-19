/**
 * Copyright (C) 2009 KSA Technologies, LLC. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 *
 */
//package com.ksatechnologies.openfire.plugin.Queuemetrics;
package org.jivesoftware.openfire.plugin.Queuemetrics;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;

import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


public class QueuemetricsLoggerPlugin implements Plugin
{
	private static final String SELECT_AGENT_DEVICE =
			" WHERE userID = ?;";
	private static final String SELECT_AGENTID =
			" WHERE username = ?;";
	private static final String INSERT_QUEUE_LOG =
			" (partition,time,callid,queuename,agent,event,UTCtime) VALUES (?,NOW(),'ofQueuemetricsPlugin','NONE',?,?,NOW());";
	private static final String INSERT_QUEUE_LOG_PAUSE =
			" (partition,time,callid,queuename,agent,event,data1,UTCtime) VALUES (?,NOW(),'ofQueuemetricsPlugin','NONE',?,?,?,NOW());";

	protected XMPPServer server;
	protected PresenceLayerer presenceHandler;
	private QueuemetricsLoggerSessionEventListener listener = new QueuemetricsLoggerSessionEventListener();
	private PropertyListener propertyListener;
	private Boolean isComponentReady;
	private Boolean pluginEnabled;
	private Boolean useAsteriskIM;

	/**
	* Constructor for the QueuemetricsLoggerPlugin.
	*/
	public QueuemetricsLoggerPlugin() {
		this.propertyListener = new PropertyListener();
		this.pluginEnabled = false;
		this.useAsteriskIM = false;
	}

	/**
	* Routine called from the PluginManager to initialize plugin.
	*/
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		Log.info("Queuemetrics-Logger plugin initializer");
		try {
			PropertyEventDispatcher.addListener(propertyListener);
			Boolean enabled = getEnabled();
			if (enabled != isEnabled()) {
				doEnable(enabled);
			}
		} catch (Throwable e) {
			// Make sure we catch all exceptions show we can Log anything that might be
			// going on
			Log.error("Queuemetrics-Logger plugin not Initializing because of errors", e);
		}
	}

	/**
	* Common initialize method for the plugin.
	*/
	public void initialize(Boolean isEnabled) throws Exception {
		Log.info("Initializing Queuemetrics-Logger plugin");
		if (!isEnabled) {
			return;
		}
		server = XMPPServer.getInstance();
		this.presenceHandler = new PresenceLayerer(server.getServerInfo().getXMPPDomain());
		// Register a packet interceptor for handling on phone presence changes
		InterceptorManager.getInstance().addInterceptor(presenceHandler);
		SessionEventDispatcher.addListener(listener);
		Boolean aimEnabled = getUseAsteriskIM();
		if (aimEnabled != useAsteriskIM()) {
			doUseAsteriskIM(aimEnabled);
		}
		start();
	}

	/**
	* Disables the plugin via the destory Plugin class event.
	*/
	public void destroyPlugin() {
		PropertyEventDispatcher.removeListener(propertyListener);
		destroy();
	}

	/**
	* Common destory method for the plugin.
	*/
	public void destroy() {
		shutdown();
		Log.info("Unloading Queuemetrics-Logger plugin");
		InterceptorManager.getInstance().removeInterceptor(presenceHandler);
		SessionEventDispatcher.removeListener(listener);
		listener = null;
		server = null;
	}

	/**
	* Enables or disables the plugin.
	*
	* @param shouldEnable true to enable and false to disable
	*/
	private void doEnable(final Boolean shouldEnable) {
		try {
			if (shouldEnable) {
				pluginEnabled = true;
				initialize(pluginEnabled);
			} else {
				pluginEnabled = false;
				destroy();
			}
		} catch (Exception e) {
			Log.error("Error enabling or disabling plugin.", e);
		}
	}

	/**
	* Enables or disables the use of Asterisk-IM database tables.
	*
	* @param shouldEnable true to enable and false to disable
	*/
	private void doUseAsteriskIM(final Boolean shouldEnable) {
		try {
			if (shouldEnable) {
				useAsteriskIM = true;
				Log.info("Queuemetrics-Logger is using the Asterisk-IM tables");
			} else {
				useAsteriskIM = false;
				Log.info("Queuemetrics-Logger is using the Openfire qm<Name> tables");
			}
		} catch (Exception e) {
			Log.error("Error enabling or disabling plugin.", e);
		}
	}



	/**
	* Writes a record to the Asterisk queue log.
	*
	* @param agentID The numeric ID of the agent from the database
	* @param verb The Queuemetices/Asterisk Queue Log verb
	*/
	public void writeToQueueLog(Integer agentID, String verb) {
		java.sql.Connection con = null;
		PreparedStatement pstmtSelect = null;
		PreparedStatement pstmtInsert = null;
		String SelectDBSchema = "phoneDevice";
		String InsertDBSchema = getDBN() + "." + getDBTable();
		if (!getUseAsteriskIM()) {
			SelectDBSchema = InsertDBSchema;
		}
		try {
			con = DbConnectionManager.getConnection();
			pstmtSelect = con.prepareStatement("SELECT device FROM " + SelectDBSchema + SELECT_AGENT_DEVICE);
			pstmtSelect.setInt(1, agentID);
			ResultSet rs = pstmtSelect.executeQuery();
			while (rs.next()) {
				String agentDevice = rs.getString("device");
				pstmtInsert = con.prepareStatement("INSERT INTO " + InsertDBSchema + INSERT_QUEUE_LOG);
				pstmtInsert.setString(1, getPartition());
				pstmtInsert.setString(2, agentDevice);
				pstmtInsert.setString(3, verb);
				pstmtInsert.executeUpdate();
			}
		} catch (Exception e) {
			Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
		} finally {
			try {
				if (pstmtSelect != null) {
					pstmtSelect.close();
				}
				if (pstmtInsert != null) {
					pstmtInsert.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
		}
	}

	/**
	* Writes a record to the Asterisk queue log with pause code
	*
	* @param agentID The numeric ID of the agent from the database
	* @param verb The Queuemetices/Asterisk Queue Log verb
	* @param pauseCode The numeric code for the reason for pausing
	*/
	public void writeToQueueLog(Integer agentID, String verb, String pauseCode) {
		java.sql.Connection con = null;
		PreparedStatement pstmtSelect = null;
		PreparedStatement pstmtInsert = null;
		String SelectDBSchema = "phoneDevice";
		String InsertDBSchema = getDBN() + "." + getDBTable();
		if (!getUseAsteriskIM()) {
			SelectDBSchema = InsertDBSchema;
		}
		try {
			con = DbConnectionManager.getConnection();
			pstmtSelect = con.prepareStatement("SELECT device FROM " + SelectDBSchema + SELECT_AGENT_DEVICE);
			pstmtSelect.setInt(1, agentID);
			ResultSet rs = pstmtSelect.executeQuery();
			while (rs.next()) {
				String agentDevice = rs.getString("device");
				pstmtInsert = con.prepareStatement("INSERT INTO " + InsertDBSchema + INSERT_QUEUE_LOG_PAUSE);
				pstmtInsert.setString(1, getPartition());
				pstmtInsert.setString(2, agentDevice);
				pstmtInsert.setString(3, verb);
				pstmtInsert.setString(4, pauseCode);
				pstmtInsert.executeUpdate();
			}
		} catch (Exception e) {
			Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
		} finally {
			try {
				if (pstmtSelect != null) {
					pstmtSelect.close();
				}
				if (pstmtInsert != null) {
					pstmtInsert.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
		}
	}


//	public void resourceBound(Session session) {
		// Do nothing.
//	}

	private Integer getAgentID(String agent) {
		java.sql.Connection con = null;
		PreparedStatement pstmtSelect = null;
		Integer userID = 0;
		String dbSchema = "phoneUser";
		if (!getUseAsteriskIM()) {
			dbSchema = getDBN() + "." + getDBTable();
		}
		try {
			con = DbConnectionManager.getConnection();
			pstmtSelect = con.prepareStatement("SELECT userID FROM " + dbSchema + SELECT_AGENTID);
			pstmtSelect.setString(1, agent);
			ResultSet rs = pstmtSelect.executeQuery();
			if (rs.next()) {
				userID = rs.getInt("userID");
			}
		} catch (Exception e) {
			Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
		}
		finally {
			try {
				if (pstmtSelect != null) {
					pstmtSelect.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
			try {
				if (con != null) {
					con.close();}
			} catch (Exception e) {
				 Log.error(e);
			}
		}
		return(userID);
	}


	/**
	* The Session Event Listener.  This will execute when a session is either created,
	* or destroyed.  This is all we care about now.
	*/
	private class QueuemetricsLoggerSessionEventListener implements SessionEventListener {
		public void sessionCreated(Session session) {
			if (isEnabled()) {
				String agent = session.getAddress().getNode();
				Integer agentID = getAgentID(agent);
				if (!agentID.equals(0)) {
					Log.info("Queuemetrics Agent '" + session.getAddress().getNode() + "' logged on");
					writeToQueueLog(agentID, "AGENTLOGIN");
				}
			}
		}

		public void sessionDestroyed(Session session) {
			if (isEnabled()) {
				String agent = session.getAddress().getNode();
				Integer agentID = getAgentID(agent);
				if (!agentID.equals(0)) {
					Log.info("Queuemetrics Agent '" + session.getAddress().getNode() + "' logged off");
					writeToQueueLog(agentID, "AGENTLOGOFF");
				}
			}
		}

		public void resourceBound(Session session) {
			// Do nothing.
		}

		public void anonymousSessionCreated(Session session) {
			//ignore
		}

		public void anonymousSessionDestroyed(Session session) {
			//ignore
		}
	}

	/**
	* The Property Listener.  This will be executed when a property in the Openfire
	* properties collection is changed.  We will only worry about Enable for now.
	*/
	private class PropertyListener implements PropertyEventListener {
		public void propertySet(String property, Map params) {
			if (QueuemetricsProperties.ENABLED.equals(property)) {
				Object value = params.get("value");
				if (value != null) {
					handleEnable(Boolean.valueOf(value.toString()));
				}
			} else if (QueuemetricsProperties.USEASTERISKIM.equals(property)) {
				Object value = params.get("value");
				if (value != null) {
					handleUserAsteriskIM(Boolean.valueOf(value.toString()));
				}
			}
		}

		public void propertyDeleted(String property, Map params) {
			if (QueuemetricsProperties.ENABLED.equals(property)) {
				doEnable(false);
			} else if (QueuemetricsProperties.USEASTERISKIM.equals(property)) {
				setUseAsteriskIM(false);
			}
		}

		public void xmlPropertySet(String property, Map params) {
			// Do nothing.
		}

		public void xmlPropertyDeleted(String property, Map params) {
			// Do nothing.
		}

		private void handleEnable(Boolean shouldEnable) {
			try {
				Boolean isCurrentlyEnabled = isEnabled();
				if (isCurrentlyEnabled != shouldEnable) {
					doEnable(shouldEnable);
				}
			} catch (Exception ex) {
				Log.info("Bad things");
				/* Do Nothing as this exception is logged in isEnabled() */
			}
		}

		private void handleUserAsteriskIM(Boolean shouldEnable) {
			try {
				Boolean useAsteriskIMEnabled = useAsteriskIM();
				if (useAsteriskIMEnabled != shouldEnable) {
					doUseAsteriskIM(shouldEnable);
				}
			} catch (Exception ex) {
				Log.info("Bad things");
				/* Do Nothing as this exception is logged in isEnabled() */
			}
		}

	}


	private String matchStatusToQM(String status) {
		java.sql.Connection con = null;
		PreparedStatement pstmtSelect = null;
		String pauseCode = getDefaultPauseCode();
		String dbSchema = "qmMappings";
		if (!getUseAsteriskIM()) {
			dbSchema = getDBN() + "." + getDBTable();
		}
		try {
			con = DbConnectionManager.getConnection();
			pstmtSelect = con.prepareStatement("SELECT pauseCode FROM " + dbSchema + " WHERE presenceStatus = ?");
			pstmtSelect.setString(1, status);
			ResultSet rs = pstmtSelect.executeQuery();
			if (rs.next()) {
				pauseCode = rs.getString("pauseCode");
			}
		} catch (Exception e) {
			Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
		}
		finally {
			try {
				if (pstmtSelect != null) {
					pstmtSelect.close();
				}
			} catch (Exception e) {
				Log.error(e);
			}
			try {
				if (con != null) {
					con.close();}
			} catch (Exception e) {
				 Log.error(e);
			}
		}
		return(pauseCode);
	}


	/**
	* The PresenceLayerer Method.  Intercepts presence packets and writes them to the db
	*/
	public class PresenceLayerer implements PacketInterceptor {
		private String serverName;

		public PresenceLayerer(String serverName) {
			this.serverName = serverName;
		}

		public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) throws PacketRejectedException {
			if (processed || !incoming || !(packet instanceof Presence)) {
				return;
			}
			// Pull out the presence section of the packet.
			final Presence presence = (Presence) packet;
			final JID from = presence.getFrom();
			// Only process packets sent by users on our server
			if (!serverName.equals(from.getDomain())) {
				return;
			}
			// If the presence type is error we should just ignore it
			if (Presence.Type.error.equals(presence.getType())) {
				return;
			}
			// We should also ignore probe presence types
			if (Presence.Type.probe.equals(presence.getType())) {
				return;
			}
			// If this presence is directed to an individual then ignore it
			if (presence.getTo() != null) {
				return;
			}
			// If the agent is anonymous, or is a service
			final String agentName = from.getNode();
			if (agentName == null || "".equals(agentName)) {
				return;
			}
			Integer agentID = getAgentID(agentName);
			if (!agentID.equals(0)) {
				Log.info("--->Received presence packet - Agent: '" + agentName + 
					"'	Incoming: '" + incoming + "'	Type: '" + presence.getType() + "'");
				if (!(presence.getType() == null) && (!(presence.getType().equals(Presence.Type.unavailable)))) {
					return;
				}
				final String status = (presence.getStatus() == null) ?
						"unavailable" :
							presence.getStatus().toLowerCase().trim();
				Log.info(" -->Received presence for user '" + agentName + "': " + status);
				String qmPauseCode = matchStatusToQM(status);
				if (!qmPauseCode.equals("-1")) {
					Log.info("  ->Presence to QM pause code: '" + status + "' => '" + qmPauseCode + "'");
					writeToQueueLog(agentID, "PAUSEREASON", qmPauseCode);
				}
			}
		}
	}

	/**
	* sets isComponentReady to true so we start accepting requests
	*/
	public void start() {
		Log.info("Queuemetrics-Logger plugin started");
		isComponentReady = true;
	}

	/**
	* Sets isComponentReady to false we will quit accepting requests
	*/
	public void shutdown() {
		Log.info("Queuemetrics-Logger plugin shuting down");
		isComponentReady = false;
	}

	/**
	* Sets isComponentReady to false we will quit accepting requests
	*/
	public Boolean isComponentReady() {
		return isComponentReady;
	}

	/**
	* Sets the property varaible 'plugin.queuemetrics.enabled' to true or false
	*/
	public void setEnabled(Boolean enable) {
		JiveGlobals.setProperty(QueuemetricsProperties.ENABLED, enable ? Boolean.toString(true) : Boolean.toString(false));
	}

	/**
	* Gets the property varaible 'plugin.queuemetrics.enabled'
	*/
	public Boolean getEnabled() {
		return JiveGlobals.getBooleanProperty(QueuemetricsProperties.ENABLED, false);
	}

	/**
	* Checks the private variable to see if it is set
	*/
	public Boolean isEnabled() {
		return pluginEnabled;
	}

	/**
	* Sets isComponentReady to false we will quit accepting requests
	*/
	public void setDBN(String dbn) {
		JiveGlobals.setProperty(QueuemetricsProperties.DBN, dbn);
	}

	/**
	* Sets isComponentReady to false we will quit accepting requests
	*/
	public String getDBN() {
		return JiveGlobals.getProperty(QueuemetricsProperties.DBN, "asterisk");
	}

	/**
	* Sets the table name to table
	*/
	public void setDBTable(String dbtable) {
		JiveGlobals.setProperty(QueuemetricsProperties.DBTABLE, dbtable);
	}

	/**
	* Gets the queue log table name
	*/
	public String getDBTable() {
		return JiveGlobals.getProperty(QueuemetricsProperties.DBTABLE, "queue_log");
	}

	/**
	* Sets the Queuemetrics database partition in 'plugin.queuemetrics.partition'
	*/
	public void setPartition(String dbpart) {
		JiveGlobals.setProperty(QueuemetricsProperties.DBPARTITION, dbpart);
	}

	/**
	* Gets the Queuemetric Database partition from 'plugin.queuemetrics.partition'
	*/
	public String getPartition() {
		return JiveGlobals.getProperty(QueuemetricsProperties.DBPARTITION, "P001");
	}

	/**
	* Sets the Queuemetrics database partition in 'plugin.queuemetrics.useAsteriskIM'
	*/
	public void setUseAsteriskIM(Boolean useAsteriskIM) {
		JiveGlobals.setProperty(QueuemetricsProperties.USEASTERISKIM, useAsteriskIM ? Boolean.toString(true) : Boolean.toString(false));
	}

	/**
	* Gets the Queuemetric Database partition from 'plugin.queuemetrics.useAsteriskIM'
	*/
	public Boolean getUseAsteriskIM() {
		return JiveGlobals.getBooleanProperty(QueuemetricsProperties.USEASTERISKIM, false);
	}

	/**
	* Gets the Queuemetric Database partition from 'plugin.queuemetrics.useAsteriskIM'
	*/
	public Boolean useAsteriskIM() {
		return useAsteriskIM;
	}

	/**
	* Sets the Queuemetrics database partition in 'plugin.queuemetrics.partition'
	*/
	public void setDefaultPauseCode(String pauseCode) {
		JiveGlobals.setProperty(QueuemetricsProperties.DEFAULTPAUSECODE, pauseCode);
	}

	/**
	* Gets the Queuemetric Database partition from 'plugin.queuemetrics.partition'
	*/
	public String getDefaultPauseCode() {
		return JiveGlobals.getProperty(QueuemetricsProperties.DEFAULTPAUSECODE, "");
	}
}
