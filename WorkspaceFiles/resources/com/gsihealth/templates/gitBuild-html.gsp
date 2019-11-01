<style>
BODY, TABLE, TD, TH, P {
  font-family:Verdana,Helvetica,sans serif;
  font-size:11px;
  color:black;
}
</style>

<% com.gsihealth.jenkins.runner.GitBuildTask _task = task %>

<body>
<table>
    <tr><td colspan="2">
      <b style="font-size:150%;<% if(build.result=='SUCCESS'){%>color:#00897B;<%}%>">Project: ${_task.run.parent.name}</b>
    </td></tr>

    <tr style="background-color:<%if(_task.result=='SUCCESS')
    {%>#DCEDC8<%}
    else if(_task.result=='FAILURE')
    {%>#ffcdd2<%}
    else{%>#F5F5F5<%}%>">
        <td>Result:</td>
        <td>${_task.result}
        </td>
    </tr>

    <tr><td>Branch:</td><td>${_task.checkoutResult.GIT_BRANCH}</td></tr>
    <tr><td>Revision:</td><td>${_task.checkoutResult.GIT_COMMIT}</td></tr>
    <tr><td>Build URL:</td><td>${_task.url}</td></tr>
    <tr><td>Date:</td><td>${_task.startDate}</td></tr>
    <tr><td>Duration:</td><td>${_task.duration}</td></tr>
    <tr><td>Cause:</td>
    <td>
    <% if(_task.requestedUser){%>
      Requested by ${_task.requestedUser}
      <%}else{%>
        Triggered by timer
      <%}%>
    </td>
    </tr>

    <% if(_task.developers) { %>
    <tr>
        <td>Related Developers: </td>
        <td>
            <%_task.developers.each(){ %> ${it} <br/> <%}%>
        </td>

    </tr>

    <%}%>

    <% if(_task.culprits) { %>
    <tr>
        <td>Possible Culprits: </td>
        <td>
            <%_task.culprits.each(){ %> ${it} <br/> <%}%>
        </td>
    </tr>
    <%}%>

</table>

<br/>
See attached log for more details.

</body>