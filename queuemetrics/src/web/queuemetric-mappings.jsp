<%@ page
import="org.jivesoftware.openfire.plugin.Queuemetrics.QueuemetricsLoggerPlugin,
		org.jivesoftware.openfire.plugin.Queuemetrics.QueuemetricsProperties,
		org.jivesoftware.database.DbConnectionManager,
		org.jivesoftware.database.DefaultConnectionProvider,
		org.jivesoftware.openfire.XMPPServer,
		org.jivesoftware.openfire.container.PluginManager,
		org.jivesoftware.util.JiveGlobals,
		org.jivesoftware.util.Log,
		org.jivesoftware.util.ParamUtils,
		java.sql.PreparedStatement,
		java.sql.ResultSet,
		java.util.HashMap,
		java.util.Map"
errorPage="error.jsp"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%
	boolean isSave = request.getParameter("save") != null;
//	boolean qmEnabled = ParamUtils.getBooleanParameter(request, "qmEnabled", false);
//	boolean useAIM = ParamUtils.getBooleanParameter(request, "useAIM", false);
	boolean success = request.getParameter("success") != null;

	QueuemetricsLoggerPlugin plugin = (QueuemetricsLoggerPlugin) XMPPServer.getInstance().getPluginManager().getPlugin("queuemetrics");
	if (plugin == null) {
		// Complain about not being able to get the plugin
		String msg = "Unable to acquire the Queuemetric Logger plugin instance!";
		Log.error(msg);
		throw new IllegalStateException(msg);
	}

	Map<String, String> errors = new HashMap<String, String>();
	if (isSave) {
//		plugin.setEnabled(qmEnabled);
//		plugin.setUseAsteriskIM(useAIM);
		response.sendRedirect("queuemetric-mappings.jsp?success=true");
	}

	Exception exception = null;
//	qmEnabled = plugin.isEnabled();
//	useAIM = plugin.useAsteriskIM();
	java.sql.Connection con = null;
	PreparedStatement pstmtSelect = null;
	ResultSet rs = null;
	String dbSchema = "qmMappings";
	if (!plugin.getUseAsteriskIM()) {
		dbSchema = plugin.getDBN() + "." + plugin.getDBTable();
	}
	try {
		con = DbConnectionManager.getConnection();
		pstmtSelect = con.prepareStatement("SELECT pauseCode, presenceStatus FROM " + dbSchema + " ORDER BY pauseCode");
		rs = pstmtSelect.executeQuery();
	} catch (Exception e) {
//		Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
	}
%>

<html>
	<head>
		<title>Presence 'status' to Queuemetrics 'pause code' Mappings</title>
		<meta name="pageID" content="item-queuemetric-mappings"/>

		<style type="text/css">
			.div-border {
				border: 1px solid #CCCCCC;
				-moz-border-radius: 3px;
			}

			table.settingsTable {
				display: block;
				border: 1px solid #BBBBBB;
				margin: 5px 0px 15px 0px;
			}

			table.settingsTable thead th {
				background-color: #EAF1F8;
				border-bottom: 1px solid #BBBBBB;
				padding: 3px 8px 3px 12px;
				font-weight: bold;
				text-align: left;
			}

			table.settingsTable tbody tr td {
				padding: 5px 10px 5px 15px;
			}

			table.settingsTable tbody tr td p {
				padding: 10px 0px 5px 0px;
			}

			table.settingsTable tr {
				padding: 0px 0px 10px 0px;
			}
		</style>
	</head>
	<body>
		<div id="queuemetric-mappings">

		<p>
			Use the form below to map the a 'Presence' status to a QueueMetrics 'pause code'.
		</p>

<% if (exception != null) { %>
		<div class="jive-error" style="width: 600;">
			<table cellpadding="0" cellspacing="0" border="0">
				<tbody>
					<tr>
						<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16"
												border="0"></td>
						<td class="jive-icon-label">Error loading plugin, see error log for details.</td>
					</tr>
				</tbody>
			</table>
		</div>
	</body>
</html>
<% return;
} else if (success) { %>
		<div class="success" style="width: 600;">
			Mappings updated successfully. It will take a couple seconds to reload the settings.
		</div>
<% }
else if (errors.size() > 0) { %>
		<div class="jive-error" style="width: 600;">
			<table cellpadding="0" cellspacing="0" border="0">
				<tbody>
					<tr>
						<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16"
												border="0"></td>
						<td class="jive-icon-label">Error saving the service settings.</td>
					</tr>
				</tbody>
			</table>
		</div>
<% } %>

		<form action="queuemetric-mappings.jsp" method="get">
			<div class="div-border" style="background-color : #EAF1F8; width: 400px; padding: 4px; ">
<%
	String pauseCode = "";
	while (rs.next()) {%>
		<%=rs.getString(1)%>
		<%=rs.getString(2)%>
				<br/>
	<%}
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
%>
			</div>
			<br/>
			<br/>
			<input type="submit" name="save" value="Save"/>
		</form>
	</body>
</html>
