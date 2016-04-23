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
 * Created by macpro on 09/08/2014.
 */

fdView.controller('ViewEntityCtrl', function ($scope, $stateParams, $uibModal, configuration, EntityService) {
  $scope.entityKey = $stateParams.entityKey;
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
    EntityService.getLogsForEntity($scope.entityKey).then(function (data) {
      $scope.metaHeader = data;
      console.log(data);

      if ($scope.metaHeader.changes[0] !== null) {
        EntityService.getJsonContentForLog($scope.entityKey, $scope.metaHeader.changes[0].id).then(function (data) {
          $scope.log = data;
        });
        $scope.logSelected = $scope.metaHeader.changes[0].id;
      }

      EntityService.getTagsForEntity($scope.entityKey).then(function (data) {
        $scope.tags = data;
      });
    });
  };

  $scope.selectAction = function () {
    //var logId1 = $scope.logSelected;
    var logId2 = $scope.myOption;

    // Getting Log2
    EntityService.getJsonContentForLog($scope.entityKey, logId2).then(function (data) {
      $scope.log2 = data;
      // Log One is already loaded
      $scope.log1 = $scope.log;
    });


  };

  $scope.openExplore = function () {
    EntityService.getEntityPK($scope.entityKey).then(function (id) {
        var url = configuration.exploreUrl() + 'graph.html?id=' + id;
        window.open(url);
      }
    );
  };


  // openPopup , openDeltaPopup , selectLog must not be duplicated

  $scope.openPopup = function (logId) {
    $scope.logSelected = logId;
    EntityService.getJsonContentForLog($scope.entityKey, logId).then(function (data) {
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
    EntityService.getJsonContentForLog($scope.entityKey, logId1).then(function (data) {
      $scope.log1 = data;
    });

    // Getting Log2
    EntityService.getJsonContentForLog($scope.entityKey, logId2).then(function (data) {
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
