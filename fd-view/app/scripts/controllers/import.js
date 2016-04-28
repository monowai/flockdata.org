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

fdView.controller('ImportCtrl', ['$scope', '$uibModal', 'QueryService', '$state', '$http', 'configuration',
  function ($scope, $uibModal, QueryService, $state, $http, configuration) {

    $scope.delim=',';
    $scope.hasHeader=true;

    $scope.loadFile = function(fileContent, fileName){
      $scope.fileName = fileName;
      $scope.csvContent = fileContent;
    };

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });

    $scope.selectFortress = function(fortress) {
      var query = [fortress];
      QueryService.query('documents', query).then(function (data) {
        $scope.documents = data;
        if(data.length>0) {
          $scope.type = $scope.documents[0].name;
          $scope.selectProfile($scope.type);
        }
      });
    };

    $scope.selectProfile = function (type) {
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

    $scope.createFortress = function() {
      var modalCreateDP = $uibModal.open({
        templateUrl: 'createFortressModal.html',
        resolve: {
          timezones: function() {
            return $http.get(configuration.engineUrl() + '/api/v1/fortress/timezones').then(function (response) {
              return response.data;
            })
          }
        },
        controller: ['$scope','$uibModalInstance','timezones',function($scope, $uibModalInstance, timezones) {
          $scope.timezones = timezones;
          $scope.timezone = $scope.timezones[0];
          $scope.close = $uibModalInstance.dismiss;          
          $scope.save = function() {
            var newFortress = {
              name: $scope.name,
              searchEnabled: $scope.searchable,
              storeEnabled: $scope.versionable,
              timeZone: $scope.timezone
            };
            $http.post(configuration.engineUrl()+'/api/v1/fortress/', newFortress).then(function(response){
              $uibModalInstance.close(response.data);
            });
          };
        }]
      });
      modalCreateDP.result.then(function(newDP){
        $scope.fortresses.push(newDP);
        $scope.fortress = newDP.name;
      });
    };

    $scope.createType = function() {
      if(!$scope.fortress) return;
      var modalCreateDP = $uibModal.open({
        templateUrl: 'createTypeModal.html',
        size: 'sm',
        resolve: {
          fortress: function() {
            return $scope.fortress.toLowerCase().replace(/\s+/g, '');
          }
        },
        controller: ['$scope','$uibModalInstance','fortress', function($scope, $uibModalInstance, fortress) {
          $scope.searchable = false;
          $scope.versionable = false;
          $scope.close = $uibModalInstance.dismiss;
          $scope.save = function(name) {
            // -- waiting for end point added
            // var newType = {
            //   name: $scope.name,
            //   searchEnabled: $scope.searchable,
            //   storeEnabled: $scope.versionable
            // };
            // $http.post(configuration.engineUrl()+'/api/v1/fortress/'+fortress+'/docs/',newType).then(function(response){
            //   $uibModalInstance.close(response.data);
            // });
            $http({
              method: 'PUT',
              url: configuration.engineUrl() + '/api/v1/fortress/' +fortress+'/'+name,
              dataType: 'raw',
              headers: {
                'Content-Type': 'application/json'
              },
              data: ''
            }).then(function(response){
                $uibModalInstance.close(response.data);
              });
          };
        }]
      });
      modalCreateDP.result.then(function(newDocType){
        console.log($scope.documents);
        $scope.documents.push(newDocType);
        $scope.type = newDocType.name;
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
        var csvParser = d3.dsv($scope.delim,'text/plain');
        csvParser.parse($scope.csvContent, function(data){
          $scope.data = data; 
          $scope.keys = d3.keys(data);
        });
        // d3.csv.parse($scope.csvContent, function(data){
        //   $scope.data = data; 
        //   $scope.keys = d3.keys(data);
        // });
        angular.element('[data-target="#profile"]').tab('show');
        $scope.editor.expandAll();
      }
    };

    $scope.reset = function(){
      $state.reload();
    };
 
    $scope.saveProfile = function() {
      var profile = $scope.editor.getText();
      $http.post(configuration.engineUrl() + '/api/v1/content/' + $scope.fortress+'/'+$scope.type, profile).then(function (response) {
        console.log(response);
        return response.statusText;
      });
    };

    $scope.validate = function(){
      // $http.put(configuration.engineUrl() + '/api/v1/fortress/' + someProfile.fortressName+'/'+someProfile.documentType.name).then(function (response) {
      //   console.log(response.data);
      //   return response.data;
      // });
    };

    $scope.editorOptions = { tree: {mode: "tree", modes:["tree","code","form"]}, text: {mode:"text", modes:["text","code"]}};
    $scope.onEditorLoad = function(instance){
      $scope.editor = instance;
    };

  }]);
