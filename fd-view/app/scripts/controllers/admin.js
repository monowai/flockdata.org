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

fdView.controller('AdminCtrl', ['$scope', '$uibModal', 'QueryService', 'ProfileService', 'AuthenticationSharedService', '$state', '$http', '$timeout', 'configuration', 'USER_ROLES',
  function ($scope, $uibModal, QueryService, ProfileService, AuthenticationSharedService, $state, $http, $timeout, configuration, USER_ROLES) {

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });

    $http.get(configuration.engineUrl() + '/api/v1/fortress/timezones').then(function (response) {
      $scope.timezones = response.data;
    });

    ProfileService.getMyProfile().then(function (data) {
      $scope.profile = data;
    });

    $scope.isAdmin = function () {
      return !AuthenticationSharedService.isAuthorized(USER_ROLES.admin);
    };

    $scope.selectFortress = function(f) {
      var query = [f.name];
      $scope.typeOpen = true;
      $scope.fortress = f;
      QueryService.query('documents', query).then(function (data) {
        $scope.documents = data;
      });
      $http.get(configuration.engineUrl() + '/api/v1/fortress/'+f.code+'/segments').then(function (response) {
        $scope.segments = response.data;
      });
    };

    $scope.editFortress = function (f) {
      $scope.createDP=true;
      if(f) {
        $scope.action='Edit';
        $scope.name = f.name;
        $scope.searchable = f.searchEnabled;
        $scope.versionable = f.storeEnabled;
        $scope.timezone = f.timeZone;
      } else {
        $scope.action = 'Create';
      }
    };

    $scope.saveFortress = function () {
      var dp = {
        name: $scope.name,
        searchActive: $scope.searchable,
        storeActive: $scope.versionable,
        timeZone: $scope.timezone
      };
      $http.post(configuration.engineUrl()+'/api/v1/fortress/', dp).then(function(response){
        $scope.createDP = false;
        $scope.fortresses.push(response.data);
      });
    };

    $scope.deleteFortress = function (f) {
      $uibModal.open({
        templateUrl: 'delete-modal.html',
        size: 'sm',
        controller: ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
          $scope.cancel = $uibModalInstance.dismiss;
          $scope.type = 'Data Provider';
          $scope.name = f.name;
          $scope.delete = function () {
            $http.delete(configuration.engineUrl()+'/api/v1/admin/'+f.code).then(function () {
              $uibModalInstance.close();
            });
          }
        }]
      }).result.then(function (res) {
        console.log(res);
        $scope.fortresses.splice($scope.fortresses.indexOf(f), 1);
      });
    };

    $scope.rebuildFortress = function (f) {
      $http.post(configuration.engineUrl()+'/api/v1/admin/'+f.code+'/rebuild').then(function (res) {
        console.log(res);
      });
    };

    $scope.editType = function (doc) {
      $scope.createDT = true;
      if (doc) {
        $scope.taction = 'Edit';
        $scope.typeName = doc.name;
      } else {
        $scope.taction = 'Create';
        $scope.typeName = '';
      }
    };

    $scope.saveDocType = function (name) {
      $http({
        method: 'PUT',
        url: configuration.engineUrl() + '/api/v1/fortress/' +$scope.fortress.code+'/'+name,
        dataType: 'raw',
        headers: {
          'Content-Type': 'application/json'
        },
        data: ''
      }).then(function(response){
        $scope.createDT = false;
        console.log(response.data);
        $scope.documents.push(response.data);
      });
    };

    $scope.deleteDocType = function (f, dt) {
      $uibModal.open({
        templateUrl: 'delete-modal.html',
        size: 'sm',
        controller: ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
          $scope.cancel = $uibModalInstance.dismiss;
          $scope.type = 'Document Type';
          $scope.name = dt.name;
          $scope.delete = function () {
            $http({
              method: 'DELETE',
              url: configuration.engineUrl() + '/api/v1/admin/' +f.code+'/'+dt.name,
              dataType: 'raw',
              headers: {
                'Content-Type': 'application/json'
              }
            }).then(function(response) {
              console.log(response);
              $uibModalInstance.close();
            })
          }
        }]
      }).result.then(function (res) {
        $scope.documents.splice($scope.documents.indexOf(dt), 1);
      });
    };

    $scope.deleteSegment = function (seg) {
      $uibModal.open({
        templateUrl: 'delete-modal.html',
        size: 'sm',
        controller: ['$scope', '$uibModalInstance', function ($scope, $uibModalInstance) {
          $scope.cancel = $uibModalInstance.dismiss;
          $scope.type = 'Document Segment';
          $scope.name = seg.code;
          $scope.delete = function () {
            console.log('delete');
            $uibModalInstance.close();
          }
        }]
      }).result.then(function (res) {
        $scope.segments.splice($scope.segments.indexOf(seg), 1);
      });

    }

  }]);
