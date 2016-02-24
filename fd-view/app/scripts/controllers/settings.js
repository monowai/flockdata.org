fdView.controller('SettingController', function ($scope, $controller, configuration) {
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

  }
);

