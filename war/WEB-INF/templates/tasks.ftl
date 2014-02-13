<#include "_header.ftl">

<div class="container">

<div class="row">
  <div class="col-md-12">
    <div class="panel panel-default">
      <div class="panel-heading">
        <div class="panel-title">${msg.allTasks}</div>
      </div>
      <div class="panel-body">
        <table class="table table-bordered table-condensed table-hover table-tasks">
            <tr>
              <th>${msg.creationDate}</th>
              <th>${msg.status}</th>
              <th>${msg.outcome}</th>
              <th>${msg.delete}</th>
            </tr>
          </thead>
          <tbody>
          <#list tasks?sort_by("creationDate")?reverse as task>

          <#if task.status == "PENDING">
            <#assign statusLabel = "default">
          <#elseif task.status == "RUNNING">
            <#assign statusLabel = "info">
          <#elseif task.status == "ABORTED">
            <#assign statusLabel = "warning">
          <#elseif task.status == "TIMEOUT">
            <#assign statusLabel = "danger">
          <#elseif task.status == "ERROR">
            <#assign statusLabel = "danger">
          <#else>
            <#assign statusLabel = "success">
          </#if>

          <#if task.resultOutcome??>
            <#if task.resultOutcome == "NOT_YET_STARTED">
              <#assign outcomeLabel = "default">
            <#elseif task.resultOutcome == "UNKNOWN">
              <#assign outcomeLabel = "warning">
            <#elseif task.resultOutcome == "FALSE">
              <#assign outcomeLabel = "danger">
            <#else>
              <#assign outcomeLabel = "success">
            </#if>
          </#if>

          <tr>
            <td>
              <a href="/tasks/${task.key}" class="hover-decorate">${task.creationDate?string("yyyy-MM-dd @ HH:mm:ss")}</a>
            </td>
            <td>
              <a href="/tasks/${task.key}">
                <span class="label label-${statusLabel}">${task.status}</span>
              </a>
            </td>
            <td>
              <a href="/tasks/${task.key}">
              <#if task.resultOutcome??>
                <span class="label label-${outcomeLabel}">${task.resultOutcome}</span>
              <#else>
                &nbsp;
              </#if>
              </a>
            </td>
            <td>
              <form action="/tasks/${task.key}?method=delete" method="post" style="display:inline">
            	 <button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span> ${msg.delete}</button>
              </form>
            </td>
          </tr>
          </#list>
        </tbody>
      </table>
    </div>
  </div>
</div>

</div>

<#include "_footer.ftl">