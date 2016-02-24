'use strict';

fdView.controller('HeaderController', function ($scope, $rootScope, AuthenticationSharedService) {

    $scope.search = function () {
      $rootScope.$broadcast('META_HEADER_LOADED', $scope.searchTerm);
    };

    $scope.open = function () {
      // TODO Technical DEBT :  ANTI-PATTERN_1 (Don't manipulate DOM in the controller)
      $('body').toggleClass('hidden-menu');
    };

    $scope.logout = function () {
      AuthenticationSharedService.logout();
    };

  }
);


fdView.controller('LeftBarController', function ($scope, $rootScope, ProfileService) {

    ProfileService.getMyProfile().then(function (data) {
      $scope.profile = data;
    });

    $scope.open = function () {
      // TODO Technical DEBT :  ANTI-PATTERN_1 (Don't manipulate DOM in the controller)
      $('body').toggleClass('minified');
    };
  }
);

// New Search Controller
fdView.controller('MetaHeaderCtrl', function ($scope, EntityService, $modal, configuration) {
    //ToDo: Why do I need to Explore functions?
    $scope.advancedSearch = false;
    $scope.searchResultFound = false;
    $scope.logResultFound = false;
    $scope.seeLogsAction = false;
    $scope.searchResults = [];
    $scope.logsResults = [];
    $scope.selectedLog = [];
    $scope.fragments = [];

    $scope.openGraphExplorer = function (metaKey) {
      EntityService.getEntityPK(metaKey).then(function (id) {
        var url = configuration.exploreUrl() + 'graph.html?id=' + id;
        window.open(url);
      });
    };

    $scope.openDetailsView = function (metaKey) {
      window.open('#/view/' + metaKey);
    };

    $scope.showAdvancedSearch = function () {
      $scope.advancedSearch = !$scope.advancedSearch;
    };

    $scope.search = function (searchText, company, fortress, types) {
      var typesToBeSend = [];
      for (var type in types) {
        typesToBeSend.push(types[type].text);
      }

      EntityService.search(searchText, company, fortress, typesToBeSend).then(function (data) {
        console.log(data);
        $scope.searchResults = data;
        angular.forEach(data, function (d) {
          d.resources = [];
          var uniqueList = [];
          _.find(d.fragments, function (ele, k) {
            var uniqueEle = _.difference(_.uniq(ele), uniqueList);
            if (uniqueEle.length > 0) {
              d.resources.push({key: k, value: uniqueEle});
              uniqueList = _.union(uniqueEle, uniqueList);
            }
          });
        });
        $scope.searchResultFound = true;
        $scope.logResultFound = false;
      });

    };

    $scope.findLogs = function (metaKey, index) {
      if ($scope.searchResults[index]['logs'] === null && !$scope.searchResults[index]['seeLogsAction']) {
        $scope.metaheaderSelected = metaKey;
        EntityService.getLogsForEntity(metaKey).then(function (data) {
          $scope.searchResults[index]['logs'] = data.changes;
          $scope.searchResults[index]['seeLogsAction'] = true;
          console.log('$scope.searchResults : ', $scope.searchResults);
          $scope.logsResults = data.changes;
          $scope.fragments = data.fragments;
          $scope.logResultFound = true;
        });
      }
      else {
        $scope.searchResults[index]['seeLogsAction'] = !$scope.searchResults[index]['seeLogsAction'];
      }
    };

    $scope.openPopup = function (logId) {
      $scope.log1 = EntityService.getJsonContentForLog($scope.metaheaderSelected, logId);
//            .then(function(data){
//            $scope.log1 = data;
//        });

      var modalInstance = $modal.open({
        templateUrl: 'singleLogModalContent.html',
        controller: ModalInstanceSingleLogCtrl,
        resolve: {
          log1: function () {
            return $scope.log1;
          }
        }
      });

      modalInstance.result.then(function (selectedItem) {
        $scope.selected = selectedItem;
      }, function () {
        //$log.info('Modal dismissed at: ' + new Date());
      });
    };

    $scope.openDeltaPopup = function () {
      var logId1 = $scope.selectedLog[0];
      var logId2 = $scope.selectedLog[1];


      // Getting Log1
      $scope.log1 = EntityService.getJsonContentForLog($scope.metaheaderSelected, logId1);

      // Getting Log2
      $scope.log2 = EntityService.getJsonContentForLog($scope.metaheaderSelected, logId2);

      var modalInstance = $modal.open({
        templateUrl: 'myModalContent.html',
        controller: ModalInstanceCtrl,
        resolve: {
          log1: function () {
            return $scope.log1;
          },
          log2: function () {
            return $scope.log2;
          }
        }
      });

      modalInstance.result.then(function (selectedItem) {
        $scope.selected = selectedItem;
      }, function () {
        //$log.info('Modal dismissed at: ' + new Date());
      });
    };

    $scope.selectLog = function (logId) {
      if ($scope.selectedLog.indexOf(logId) === -1) {
        $scope.selectedLog.push(logId);
      }
      else {
        $scope.selectedLog.splice($scope.selectedLog.indexOf(logId), 1);
      }
    }

  }
)
;


var ModalInstanceCtrl = function ($scope, $modalInstance, log1, log2) {
  $scope.log1 = log1;
  $scope.log2 = log2;
  $scope.showUnchangedFlag = false;
  $scope.showUnchanged = function () {
    if (!$scope.showUnchangedFlag) {
      jsondiffpatch.formatters.html.showUnchanged();
    }
    else {
      jsondiffpatch.formatters.html.hideUnchanged();
    }
    $scope.showUnchangedFlag = !$scope.showUnchangedFlag;
  };
  $scope.ok = function () {
    $modalInstance.dismiss('cancel');
  };
};

var ModalInstanceSingleLogCtrl = function ($scope, $modalInstance, log1) {
  $scope.log1 = log1;
  $scope.ok = function () {
    $modalInstance.dismiss('cancel');
  };
};
