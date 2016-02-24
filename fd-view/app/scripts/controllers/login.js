/**
 * Created by Nabil on 09/08/2014.
 */

fdView.controller('LoginController', function ($scope, $routeParams, AuthenticationSharedService) {
    $scope.login = function () {
      AuthenticationSharedService.login($scope.username, $scope.password);
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

  }
);
