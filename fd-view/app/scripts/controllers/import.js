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

fdView.controller('ImportCtrl', ['$scope', '$uibModal', 'QueryService', '$window', '$http', 'configuration',
  function ($scope, $uibModal, QueryService, $window, $http, configuration) {

    $scope.delim=',';
    $scope.hasHeader=true;

    $scope.loadFile = function(fileContent, fileName){
      $scope.fileName = fileName;
      $scope.csvContent = fileContent;
    };

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });

    $scope.selectFortress = function() {
      var query = [$scope.fortress];
      QueryService.query('documents', query).then(function (data) {
        $scope.documents = data;
        $scope.type = $scope.documents[0].name;
        $scope.selectProfile($scope.type);
      });
    };

    $scope.selectProfile = function (type) {
      // var query = String('content/'+$scope.fortress+'/'+$scope.type);
      // QueryService.general(query).then(function(data){
      //   console.log(data);
      // });
      // return $http({
      //   method: "GET",
      //   url: configuration.engineUrl() + '/api/v1/content/' + $scope.fortress+'/'+$scope.type,
      //   headers: {'Content-Type': 'application/json'} 
      // }).then(function(response){
      //   console.log(response);
      // });
      $http.get(configuration.engineUrl() + '/api/v1/content/' + $scope.fortress+'/'+type)
        .success(function (data){
          console.log(data);
          $scope.contentProfile=data;
        })
        .error(function(){
          $http.get('testProfile.json').then(function(response){
            $scope.contentProfile=response.data;
          });
        });
    };

    $scope.checkProfile = function() {
      if (!$scope.fortress) {
        var modalError = $uibModal.open({
          templateUrl: 'errorModal.html',
          size: 'sm',
          controller: function($scope, $uibModalInstance){
            $scope.missing = 'Data Provider';
            $scope.ok = $uibModalInstance.dismiss;
          }
        }); 
      } else if (!$scope.type) {
        var modalError = $uibModal.open({
          templateUrl: 'errorModal.html',
          size: 'sm',
          controller: function($scope, $uibModalInstance){
            $scope.missing = 'Data Type';
            $scope.ok = $uibModalInstance.dismiss;
          }
        }); 
      } else if (!$scope.csvContent) {
        var modalError = $uibModal.open({
          templateUrl: 'errorModal.html',
          size: 'sm',
          controller: function($scope, $uibModalInstance){
            $scope.missing = 'CSV file';
            $scope.ok = $uibModalInstance.dismiss;
          }
        }); 
      } else {
        d3.csv.parse($scope.csvContent, function(data){
          $scope.data = data; 
          $scope.keys = d3.keys(data);
        })
        angular.element('[data-target="#profile"]').tab('show');
      }
    };

    $scope.reset = function(){
      console.log('lets reset');
    };
 
    $scope.saveProfile = function() {
      console.log($scope.contentProfile);
      $http.post(configuration.engineUrl() + '/api/v1/content/' + $scope.fortress+'/'+$scope.type, $scope.profile).then(function (response) {
        return response.data;
      });
    };

    $scope.validate = function(){
      // $http.put(configuration.engineUrl() + '/api/v1/fortress/' + someProfile.fortressName+'/'+someProfile.documentType.name).then(function (response) {
      //   console.log(response.data);
      //   return response.data;
      // });
    };

    $scope.editorOptions = { tree: {mode: "tree", expanded: true}, text: {mode:"text", modes:["text","code"]}};
    $scope.onEditorLoad = function(instance){
      $scope.editor = instance;
    };

  }]);
