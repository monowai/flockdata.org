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

fdView.controller('ImportCtrl', ['$scope', '$rootScope', '$uibModal', 'QueryService', 'ContentProfile', '$state', '$http', '$timeout', '$compile', 'configuration',
  function ($scope, $rootScope, $uibModal, QueryService, ContentProfile, $state, $http, $timeout, $compile, configuration) {
    //$state.transitionTo('import.load');

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });

    ContentProfile.getAll().then(function (res) {
      $scope.cplist = res.data;
      console.log(res.data)
    });

    $scope.createProfile = function () {
      $uibModal.open({
        templateUrl: 'create-profile.html',
        scope: $scope,
        controller: function ($uibModalInstance) {
          $scope.new = {};

          $scope.cancel = $uibModalInstance.dismiss;

          $scope.selectFortress = function(fortress) {
            console.log(fortress);
            var query = [fortress];
            QueryService.query('documents', query).then(function (data) {
              $scope.documents = data;
              console.log(data);
              if(data.length>0) {
                $scope.new.type = $scope.documents[0].name;
                // $scope.selectProfile($scope.type);
              }
            });
          };

          $scope.createFortress = function() {
            $uibModal.open({
              templateUrl: 'create-fortress-modal.html',
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
            }).result.then(function(newDP){
              $scope.fortresses.push(newDP);
              $scope.new.fortress = newDP.name;
            });
          };

          $scope.createType = function(f) {
            if(!f) {return;}
            $uibModal.open({
              templateUrl: 'create-type-modal.html',
              size: 'sm',
              resolve: {
                fortress: function() {
                  return f.toLowerCase().replace(/\s+/g, '');
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
                    console.log(response);
                    $uibModalInstance.close(response.data);
                  });
                };
              }]
            }).result.then(function(newDocType){
              console.log($scope.documents);
              $scope.documents.push(newDocType);
              $scope.new.type = newDocType.name;
            });
          };

          $scope.createEmpty = function(profile) {
            ContentProfile.createEmpty(profile);
            $uibModalInstance.close(profile);
          }
        }
      }).result.then(function (profile) {
        angular.element('[data-target="#editor"]').tab('show');
        $scope.editProfile(profile);
      });
    };

    $scope.editProfile = function (profile) {
      angular.element('[data-target="#structure"]').tab('show');
      if(profile) {
        ContentProfile.getProfile(profile).then(function (res) {
          $scope.contentProfile = res.data.contentProfile;
          $scope.profileGraph = ContentProfile.graphProfile();

          $scope.$broadcast('cytoscapeReset');
        });
      }
    };

    $scope.editorOptions = { tree: {mode: "tree", modes:["tree","code","form"]}, text: {mode:"text", modes:["text","code"]}};
    $scope.onEditorLoad = function(instance){
      $scope.editor = instance;
    };

    $scope.save = function() {
      var profile = $scope.editor.get();
      ContentProfile.updateProfile(profile);
      ContentProfile.saveProfile().then(function (response) {
        console.log(response);
        $scope.cplist.push(response.data);
        angular.element('[data-target="#list"]').tab('show');
        return response.statusText;
      });
    };

    $scope.styles = [
      {'selector': 'node',
        'css': {
          'content': 'data(id)',
          'font-size': '14pt',
          'min-zoomed-font-size': '9pt',
          'text-halign': 'center',
          'text-valign': 'center',
          'color': '#222D32',
          'background-color': '#499ef5',
          'width': '120',
          'height': '55',
          'shape': 'roundrectangle'
        }},
      {'selector': 'node[type="tag"]',
        'css': {
          'background-color': '#ff7701',
          'content': 'data(label)',
          'shape': 'ellipse',
          'width': '110',
          'height': '50'
        }},
      {'selector': 'node[type="alias"]',
        'css': {
          'background-color': '#f7bf65',
          'content': 'data(description)',
          'shape': 'ellipse',
          'width': '100',
          'height': '45'
        }},
      {'selector':'edge',
        'css':{
          'content': 'data(relationship)',
          'width': 3,
          'target-arrow-color': '#ccc',
          'target-arrow-shape': 'triangle'
        }},
      {'selector':':selected',
        'css':{
          'background-color': 'f2b871',
          'line-color': 'black',
          'target-arrow-color': 'black',
          'source-arrow-color': 'black',
          'text-outline-width': 2,
          'text-outline-color': '#888',
          // 'text-outline-color': 'black'
        }},
      {'selector':'.mouseover',
        'css':{
          'color':'#398de0'
        }},
      {'selector':'.is-dragover',
        'css':{
          'background-color':'#398de0'
        }}
    ];
    $scope.layouts = [{name: 'circle'},{name: 'cose'},
      {name: 'grid'},{name: 'concentric'},
      {name: 'random'},{name: 'breadthfirst'}];
    $scope.layout = $scope.layouts[0];
    $scope.keys = ['City', 'DeliveryPoint', 'Address', 'Suburb', 'PostCode', 'Country'];

  }]);

fdView.controller('LoadProfileCtrl', ['$scope', '$uibModal', 'QueryService', 'ContentProfile', '$state', '$http', '$timeout', '$compile', 'configuration',
  function ($scope, $uibModal, QueryService, ContentProfile, $state, $http, $timeout, $compile, configuration) {
    $scope.delim=',';
    $scope.hasHeader=true;

    $scope.loadFile = function(fileContent, fileName){
      $scope.fileName = fileName;
      $scope.csvContent = fileContent;
    };

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });

    $scope.fortress = ContentProfile.getFortress();
    $scope.type = ContentProfile.getDocType();

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
      ContentProfile.getProfile($scope.fortress, type)
        .success(function (data) {
          $scope.contentProfile = data;
          $scope.profileGraph = profile2graph();
        })
        .error(function(){
          $scope.noProfile = true;
        });
    };

    var profile2graph = function () {
      return ContentProfile.graphProfile();
    };



    $scope.checkProfile = function() {
      if (!$scope.fortress) {
        $uibModal.open({
          templateUrl: 'error-modal.html',
          size: 'sm',
          controller: function($scope, $uibModalInstance){
            $scope.missing = 'Data Provider';
            $scope.ok = $uibModalInstance.dismiss;
          }
        });
      } else if (!$scope.type) {
        $uibModal.open({
          templateUrl: 'error-modal.html',
          size: 'sm',
          controller: function($scope, $uibModalInstance){
            $scope.missing = 'Data Type';
            $scope.ok = $uibModalInstance.dismiss;
          }
        });
      } /*else if (!$scope.csvContent) {
       $uibModal.open({
       templateUrl: 'error-modal.html',
       size: 'sm',
       controller: function($scope, $uibModalInstance){
       $scope.missing = 'CSV file';
       $scope.ok = $uibModalInstance.dismiss;
       }
       });
       }*/ else {
        if ($scope.csvContent) {
          var csvParser = d3.dsv($scope.delim, 'text/plain');
          csvParser.parse($scope.csvContent, function (data) {
            $scope.data = data;
            $scope.keys = d3.keys(data);
          });
        }
        // option for comma only
        // d3.csv.parse($scope.csvContent, function(data){
        //   $scope.data = data;
        //   $scope.keys = d3.keys(data);
        // });

        $state.go('import.edit', {keys: $scope.keys});
      }
    };

    $scope.reset = function(){
      $state.reload();
    };

  }]);

fdView.controller('EditProfileCtrl', ['$scope', '$uibModal', 'QueryService', 'ContentProfile', '$state', '$http', '$timeout', '$compile', 'configuration', 'keys',
  function ($scope, $uibModal, QueryService, ContentProfile, $state, $http, $timeout, $compile, configuration, keys) {

    $scope.profileGraph = ContentProfile.graphProfile();

    $scope.keys = keys;

    $scope.save = function() {
      ContentProfile.saveProfile().then(function (response) {
        console.log(response);
        return response.statusText;
      });
    };

    $scope.styles = [
      {'selector': 'node',
        'css': {
          'content': 'data(id)',
          'font-size': '15pt',
          'min-zoomed-font-size': '9pt',
          'text-halign': 'center',
          'text-valign': 'center',
          'color': '#222D32',
          'background-color': '#499ef0',
          'width': '100',//'mapData(degree,0,5,20,80)',
          'height': '50',//'mapData(degree,0,5,20,80)',
          'shape': 'roundrectangle'
        }},
      {'selector': 'node[type="tag"]',
        'css': {
          'background-color': '#ff7701',
          'content': 'data(label)',
          'shape': 'ellipse',
          'width': '110',
          'height': '50'
        }},
      {'selector': 'node[type="alias"]',
        'css': {
          'background-color': '#f7bf65',
          'content': 'data(description)',
          'shape': 'ellipse',
          'width': '100',
          'height': '45'
        }},
      {'selector':'edge',
        'css':{
          'content': 'data(relationship)',
          'width': 3,
          'target-arrow-color': '#ccc',
          'target-arrow-shape': 'triangle'
        }},
      {'selector':':selected',
        'css':{
          'background-color': 'f2b871',
          'line-color': 'black',
          'target-arrow-color': 'black',
          'source-arrow-color': 'black',
          'text-outline-width': 2,
          'text-outline-color': '#888',
          // 'text-outline-color': 'black'
        }},
      {'selector':'.mouseover',
        'css':{
          'color':'#499ef0'
        }}
    ];

    // $scope.layout = {name: 'breadthfirst'};
    $scope.layouts = [{name: 'circle'},{name: 'cose'},
      {name: 'grid'},{name: 'concentric'},
      {name: 'random'},{name: 'breadthfirst'}];
    $scope.layout = $scope.layouts[0];

    $scope.editJson = function () {
      $uibModal.open({
        templateUrl: 'modal-json-profile.html',
        size: 'lg',
        resolve: {
          jsonProfile: ContentProfile.getProfile().then(function (profile) {
            return profile;
          })
        },
        controller: ['$scope','$uibModalInstance','jsonProfile', function ($scope, $uibModalInstance, jsonProfile) {
          $scope.contentProfile = jsonProfile;

          $scope.editorOptions = { tree: {mode: "tree", modes:["tree","code","form"]}, text: {mode:"text", modes:["text","code"]}};
          $scope.onEditorLoad = function(instance){
            $scope.editor = instance;
          };

          $scope.cancel = $uibModalInstance.dismiss;
          $scope.update = function () {
            var profile = $scope.editor.get();
            $uibModalInstance.close(profile);
          }
        }]
      }).result.then(function (cp) {
        ContentProfile.updateProfile(cp);
        $scope.profileGraph = ContentProfile.graphProfile();
      });
    };

    $scope.validate = function(){
      // $http.put(configuration.engineUrl() + '/api/v1/fortress/' + someProfile.fortressName+'/'+someProfile.documentType.name).then(function (response) {
      //   console.log(response.data);
      //   return response.data;
      // });
    };



  }]);
