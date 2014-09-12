<ul class="dropdown-menu dropdown-menu-right" role="menu" style="left:auto">

  <li role="presentation">
    <a role="menuitem" tabindex="-1" href="http://continuuity.com/docs/loom/current/en/" target="_blank">
      <span class="fa fa-book"></span>
      Documentation
    </a>
  </li>

  <li role="presentation">
    <a role="menuitem" tabindex="-1" href="http://support.continuuity.com/knowledgebase/" target="_blank">
      <span class="fa fa-support"></span>
      Support
    </a>
  </li>



  <li role="presentation" class="divider"></li>

  <li role="presentation" class="dropdown-header">
    themes
  </li>

  <li role="presentation" ng-repeat="it in theme.list()" ng-class="{disabled:it==theme.current}">
    <a role="menuitem" href="" ng-click="theme.set(it)">
      <span class="fa" ng-class="it==theme.current ? 'fa-dot-circle-o' : 'fa-circle-o'"></span>
      {{it | myCapitalizeFilter}}
    </a>
  </li>



  <li role="presentation" class="divider"></li>

  <li role="presentation" class="dropdown-header" ng-show="currentUser">
    {{currentUser.username}}/{{currentUser.tenant}}
  </li>

  <li role="presentation" ng-show="currentUser.hasRole('superadmin')" ui-sref-active="disabled">
    <a role="menuitem" tabindex="-1" ui-sref="tenants.list">
      <span class="fa fa-user"></span>
      Tenants
    </a>
  </li>

  <li role="presentation" ng-show="currentUser">
    <a role="menuitem" tabindex="-1" href="" ng-click="logout()">
      <span class="fa fa-sign-out"></span>
      Logout
    </a>
  </li>

  <li role="presentation" ng-hide="currentUser" ui-sref-active="disabled">
    <a role="menuitem" tabindex="-1" href="" ui-sref="login">
      <span class="fa fa-sign-in"></span>
      Sign in
    </a>
  </li>

</ul>