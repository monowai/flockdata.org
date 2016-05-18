/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

fdView.controller('IndexCtrl', ['$scope', '$rootScope', '$state', 'AuthenticationSharedService', 'USER_ROLES',
  function ($scope, $rootScope, $state, AuthenticationSharedService, USER_ROLES) {

    AuthenticationSharedService.getMyProfile().then(function (res) {
      $scope.setCurrentUser(res);
    });

    $scope.userRoles = USER_ROLES;
    $scope.isAuthorized = AuthenticationSharedService.isAuthorized;

    $scope.setCurrentUser = function (user) {
      $scope.profile = user;
    };

    $scope.logout = function () {
      AuthenticationSharedService.logout();
    };

    $scope.isLogin = function() {
      return $state.is('login');
    };

}]);
