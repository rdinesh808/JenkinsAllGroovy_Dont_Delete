<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="ie=edge">
  <title>Deployment Report</title>
  <link rel="stylesheet" type="text/css" href="/userContent/styles/report.css">
</head>
<body>
<table class="info">
  <tr><td colspan="2" class="header"><b>Deploy to ${build.environment}(${build.buildName}): ${build.status}</b></td></tr>
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
<table class="summary">  
  <tr><td colspan="2" class="header">Summary</td></tr>
    <% modules.each(){ m -> 
    def _class = m.status?:'none'
    		%>
        <tr class="${_class}">
          <td>${m.appName}:</td>
          <td>${m.status}</td>
        </tr>
    <%}%>
</table>
<br>
<% modules.each(){ m -> 
def _class = m.status?:'none'
%>
  <hr>
  <br>
  <div class="_class">
  <table width="100%">
      <tr><td class="header">${m.appName} -- ${m.status}</td></tr>
      <tr class="source-build"><td>Deployed Source Build:</td>
      <td>
        <a href="${build.homeUrl}job/${m.srcJob.currentSrcBuild.parent.name}/${m.srcJob.currentSrcBuild.number}/">${m.srcJob.currentSrcBuild.parent.name}(${m.srcJob.currentSrcBuild.number})</a>
      </td></tr>
      <%if(m.status=='no-change'){%>
        <tr><td colspan="2" style="font-size:110%;">No source change was found since the last successful deployment.</td></tr>
      <%}else{%>
      <tr><td class="changes">Changes</td></tr>
      <% if(m.srcJob.changeLogSetList.size()>0){
        m.srcJob.changeLogSetList.findAll{it!=null}.each(){ csl ->
          %>
          <tr><td colspan="2" class="build">--Build#:${csl.number}</td></tr>
          <% if(csl.changeSets&&csl.changeSets.size()>0){ 
            csl.changeSets.findAll{it!=null}.each(){cs-> %>
              <tr><td colspan="2" class="commit">----Rev.${cs.revision}: <b>${cs.author}</b>(${cs.message})</td></tr>
            <% if(m.srcJob.changeLogSetList.size()>0){
        cs.paths.each(){ p-> %>
          <tr><td colspan="2" class="path">----${p}</td></tr>
          <%}}
          }
            }else{%><tr><td colspan="2" class="no-changeset">No change sets.</td></tr><%}%>
        <%}%>
      <% }else{ %>
        <tr><td colspan="2" class="no-changeset">No changes.</td></tr>
      <% }}%>
  </table>
    <% if(m.message){ %>
    <div class="message">
    Message: ${m.message}
    </div>
  <% } %>
  <% if(m.console){ %>
  	<details class="console">
  		<summary>Console log</summary>
  		 <div class="log">
  		 <% m.console.eachLine{ %>
  		 	${it}<br>
  		 <% } %>
  		 </div>
  	</details>
  <% } %>
  </div>
  <br>
<%}%>


</body>
</html>