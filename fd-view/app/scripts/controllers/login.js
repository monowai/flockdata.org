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

/**
 * Created by Nabil on 09/08/2014.
 */

fdView.controller('LoginCtrl', ['$scope', '$stateParams', 'AuthenticationSharedService',
  function ($scope, $stateParams, AuthenticationSharedService) {
    $scope.login = function () {
      AuthenticationSharedService.login($scope.username, $scope.password)
        .then(function () {
          AuthenticationSharedService.getMyProfile().then(function (res) {
            $scope.setCurrentUser(res);
          });
        });
    };

    $scope.$on('event:auth-loginRequired', function () {
      if ($scope.username || $scope.password) {
        $scope.message = 'Login Error: The Username or Password you entered is incorrect.';
      }
    });
    $scope.$on('event:auth-notAuthorized', function () {
      $scope.message = 'Login Error: Forbidden';
    });

    $scope.logout = function () {
      AuthenticationSharedService.logout();
    };

}]);
