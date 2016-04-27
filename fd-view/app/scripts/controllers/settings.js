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

fdView.controller('SettingsCtrl', ['$scope', '$controller', 'configuration', function ($scope, $controller, configuration) {
    $scope.setting = {};

    $scope.apply = function () {
      configuration.setEngineUrl($scope.setting.fdEngineUrl);
      configuration.setExploreUrl($scope.setting.exploreUrl);
      configuration.setDevMode($scope.setting.devMode);
      $scope.applyResult = true;
    };

    $scope.clear = function () {
      configuration.setEngineUrl('');
      configuration.setExploreUrl('');
      $scope.setting = {};
    };

    $scope.init = function () {
      $scope.setting.fdEngineUrl = configuration.engineUrl();
      $scope.setting.exploreUrl = configuration.exploreUrl();
      $scope.setting.devModeChecked = configuration.devMode();
    };

}]);

