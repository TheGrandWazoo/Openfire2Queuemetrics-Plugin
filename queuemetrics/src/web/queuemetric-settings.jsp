<%@ page
import="org.jivesoftware.openfire.plugin.Queuemetrics.QueuemetricsLoggerPlugin,
		org.jivesoftware.openfire.plugin.Queuemetrics.QueuemetricsProperties,
		org.jivesoftware.util.JiveGlobals,
		org.jivesoftware.util.Log,
		org.jivesoftware.util.ParamUtils,
		org.jivesoftware.openfire.XMPPServer,
		org.jivesoftware.openfire.container.PluginManager,
		java.util.HashMap,
		java.util.Map"
errorPage="error.jsp"%>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%
	boolean isSave = request.getParameter("save") != null;
	boolean qmEnabled = ParamUtils.getBooleanParameter(request, "qmEnabled", false);
	boolean useAIM = ParamUtils.getBooleanParameter(request, "useAIM", false);
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
		plugin.setEnabled(qmEnabled);
		plugin.setUseAsteriskIM(useAIM);
		response.sendRedirect("queuemetric-settings.jsp?success=true");
	}

	Exception exception = null;
	qmEnabled = plugin.isEnabled();
	useAIM = plugin.useAsteriskIM();
%>

<html>
	<head>
		<title>General Settings</title>
		<meta name="pageID" content="item-queuemetric-settings"/>

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
		<div id="queuemetric-settings">

		<p>
			Use the form below to enable Queuemetrics Logging and integration settings.
			Changing settings will cause the plugin to be reloaded.<br>
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
			Service settings updated successfully. It will take a couple seconds to reload the settings.
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

		<form action="queuemetric-settings.jsp" method="get">
			<div class="div-border" style="background-color : #EAF1F8; width: 400px; padding: 4px; ">
				<span style="font-weight:bold;">Queuemetrics Logger:</span>
				<span><input type="radio" name="qmEnabled"
							value="true" <%= qmEnabled ? "checked" : ""%> />Enabled</span>
				<span><input type="radio" name="qmEnabled"
							value="false" <%=!qmEnabled ? "checked" : ""%> />Disabled</span>
			</div>
			<br/>
			<div class="div-border" style="background-color : #EAF1F8; width: 400px; padding: 4px; ">
				<span style="font-weight:bold;">Use Asterisk-IM Talbles:</span>
				<span><input type="radio" name="useAIM"
							value="true" <%= useAIM ? "checked" : ""%> />Enabled</span>
				<span><input type="radio" name="useAIM"
							value="false" <%=!useAIM ? "checked" : ""%> />Disabled</span>
			</div>
			<br/>
			<br/>
			<input type="submit" name="save" value="Save"/>
		</form>
	</body>
</html>
