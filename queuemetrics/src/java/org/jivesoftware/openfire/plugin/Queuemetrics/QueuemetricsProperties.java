/**
 * Copyright (C) 2009 KSA Technologies, LLC. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

//package com.ksatechnologies.openfire.plugin.Queuemetrics;
package org.jivesoftware.openfire.plugin.Queuemetrics;

public class QueuemetricsProperties {
	/**
	* The Openfire properites varialbe for the Queuemetrics partition
	*/
	public static final String ENABLED = "queuemetrics.logger.enabled";

	/**
	* The Openfire properites varialbe for the Queuemetrics partition
	*/
	public static final String DBN = "queuemetrics.logger.dbName";

	/**
	* The Openfire properites varialbe for the Queuemetrics partition
	*/
	public static final String DBTABLE = "queuemetrics.logger.dbTable";

	/**
	* The Openfire properites varialbe for the Queuemetrics partition
	*/
	public static final String DBPARTITION = "queuemetrics.logger.partition";

	/**
	* The Openfire properites variable for Queuemetrics to use the Asterisk-IM tables
	*/
	public static final String USEASTERISKIM = "queuemetrics.logger.useAsteriskIM";

	/**
	* The Openfire properites variable for Queuemetrics to use the Asterisk-IM tables
	*/
	public static final String DEFAULTPAUSECODE = "queuemetrics.logger.defaultPauseCode";
}
