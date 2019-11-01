<style>
BODY, TABLE, TD, TH, P {
  font-family:Verdana,Helvetica,sans serif;
  font-size:11px;
  color:black;
}
</style>

<body>
<table>
  <tr><td colspan="2"><b style="font-size:150%;<% if(build.status=='SUCCESS'){%>color:#00897B;<%}%>">Deploy to ${build.environment}(${build.buildName}): ${build.status}</b></td></tr>
  <tr><td>URL:</td><td>${build.url}</td></tr>
  <tr><td>Report:</td><td>${build.reportUrl}</td></tr>
  <tr><td>Project:</td><td>${build.jobName}</td></tr>
  <tr><td>Date:</td><td>${build.start}</td></tr>
  <tr><td>Duration:</td><td>${build.duration}</td></tr>
  <tr><td>Cause:</td>
    <td>
    <% if(build.requestedUser){%>
      Requested by ${build.requestedUser}
      <%}else{%>
        Triggered by timer
      <%}%>
    </td>
  </tr>
  <tr><td>Force Deploy:</td><td>${build.force}</td></tr>
</table>
<br/>
<table>  
  <tr><td colspan="2"><b style="font-size:140%;">Summary</b></td></tr>
    <% modules.each(){ m -> %>
        <tr style="background-color:<%if(m.status=='success')
            {%>#DCEDC8<%}
            else if(m.status=='failure')
            {%>#ffcdd2<%}
            else{%>#F5F5F5<%}%>">
          <td>${m.appName}:</td>
          <td>${m.status}
            <% if(m.message){ %>
              <span class="message">
              (${m.message})
              </span>
            <% } %>
          </td>
        </tr>
    <%}%>
</table>
<br/>
<% modules.each(){ m -> %>
  <table width="100%">
      <tr style="background-color:<%if(m.status=='success')
            {%>#DCEDC8<%}
            else if(m.status=='failure')
            {%>#ffcdd2<%}
            else{%>#F5F5F5<%}%>"><td colspan="2"><b style="font-size:120%;">${m.appName} -- ${m.status}</b></td></tr>
      <tr><td>Source Build:</td>
      <td>
          <a href="${build.homeUrl}job/${m.srcJob.currentSrcBuild.parent.name}/${m.srcJob.currentSrcBuild.number}/">${m.srcJob.currentSrcBuild.parent.name}(${m.srcJob.currentSrcBuild.number})</a>
      </td></tr>
      <%if(m.status=='no-change'){%>
        <tr><td colspan="2" style="font-size:110%;">No source change was found since the last successful deployment.</td></tr>
      <%}else{%>
      <tr><td colspan="2" style="font-size:110%;">Changes</td></tr>
      <% if(m.srcJob.changeLogSetList.size()>0){
        m.srcJob.changeLogSetList.findAll{it!=null}.each(){ csl ->
          %>
          <tr><td colspan="2">--Build#:${csl.number}</td></tr>
          <% if(csl.changeSets&&csl.changeSets.size()>0){ 
            csl.changeSets.findAll{it!=null}.each(){cs-> %>
              <tr><td colspan="2">----Rev.${cs.revision}: <b>${cs.author}</b>(${cs.message})</td></tr>
          <%}
            }else{%><tr><td colspan="2">No change sets.</td></tr><%}%>
        <%}%>
      <% }else{ %>
        <tr><td colspan="2">No changes.</td></tr>
      <% }}%>
  </table>
  <br/>
<%}%>



</body>