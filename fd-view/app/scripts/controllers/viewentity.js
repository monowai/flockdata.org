/**
 * Created by macpro on 09/08/2014.
 */

fdView.controller('ViewEntityController', function ($scope, $routeParams, $modal, configuration, EntityService) {
  $scope.metaKey = $routeParams.metaKey;
  $scope.metaHeader = {};
  $scope.log = {};
  $scope.tags = [];

  $scope.log1 = {};
  $scope.log2 = {};

  $scope.selectedLog = [];

  $scope.showUnchangedFlag = false;
  $scope.showUnchanged = function () {
    console.log('$scope.showUnchanged : ', $scope.showUnchangedFlag);
    if ($scope.showUnchangedFlag) {
      jsondiffpatch.formatters.html.showUnchanged();
    }
    else {
      jsondiffpatch.formatters.html.hideUnchanged();
    }
  };


  $scope.init = function () {
    EntityService.getLogsForEntity($scope.metaKey).then(function (data) {
      $scope.metaHeader = data;
      if ($scope.metaHeader.changes[0] !== null) {
        EntityService.getJsonContentForLog($scope.metaKey, $scope.metaHeader.changes[0].id).then(function (data) {
          $scope.log = data;
        });
        $scope.logSelected = $scope.metaHeader.changes[0].id;
      }

      EntityService.getTagsForEntity($scope.metaKey).then(function (data) {
        $scope.tags = data;
      });
    });
  };

  $scope.selectAction = function () {
    //var logId1 = $scope.logSelected;
    var logId2 = $scope.myOption;

    // Getting Log2
    EntityService.getJsonContentForLog($scope.metaKey, logId2).then(function (data) {
      $scope.log2 = data;
      // Log One is already loaded
      $scope.log1 = $scope.log;
    });


  };

  $scope.openExplore = function () {
    EntityService.getEntityPK($scope.metaKey).then(function (id) {
        var url = configuration.exploreUrl() + 'graph.html?id=' + id;
        window.open(url);
      }
    );
  };


  // openPopup , openDeltaPopup , selectLog must not be duplicated

  $scope.openPopup = function (logId) {
    $scope.logSelected = logId;
    EntityService.getJsonContentForLog($scope.metaKey, logId).then(function (data) {
      $scope.log = data;
    });
    // reset Logs DELTA
    $scope.log1 = {};
    $scope.log2 = {};
  };

  $scope.openDeltaPopup = function () {
    var logId1 = $scope.selectedLog[0];
    var logId2 = $scope.selectedLog[1];


    // Getting Log1
    EntityService.getJsonContentForLog($scope.metaKey, logId1).then(function (data) {
      $scope.log1 = data;
    });

    // Getting Log2
    EntityService.getJsonContentForLog($scope.metaKey, logId2).then(function (data) {
      $scope.log2 = data;
    });

  };

  $scope.selectLog = function (logId) {
    if ($scope.selectedLog.indexOf(logId) === -1) {
      $scope.selectedLog.push(logId);
    }
    else {
      $scope.selectedLog.splice($scope.selectedLog.indexOf(logId), 1);
    }
  };

});
