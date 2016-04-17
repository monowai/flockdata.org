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

fdView.controller('ImportController', ['$scope', 'QueryService', '$window', '$controller', '$http', 'configuration',
  function ($scope, QueryService, $window, $controller, $http, configuration) {

    $scope.showContent = function($fileContent){
      d3.csv.parse($fileContent, function(data){
        $scope.data = data; 
        $scope.keys = d3.keys(data);
      })
      angular.element('[data-target="#profile"]').tab('show');
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
        .success(function (response){
          console.log(response);
          $scope.profile=response.data;
        })
        .error(function(){
          $http.get('testProfile.json').then(function(response){
            $scope.profile=response.data;
          });
        });
    };
 
    $scope.saveProfile = function() {
      console.log($scope.profile);
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
