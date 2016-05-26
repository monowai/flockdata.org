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

fdView.controller('ExploreCtrl', ['$scope', '$http', 'QueryService', '$compile', '$controller', 'configuration',
  function ($scope, $http, QueryService, $compile, $controller, configuration) {
    $scope.matrix = QueryService.lastMatrix();
    if(_.isEmpty($scope.matrix)) {
      angular.element('[data-target="#search"]').tab('show');
      $scope.graphData = [];
    } else $scope.graphData=$scope.matrix;

    $scope.layouts = [{name: 'cose'},
      {name: 'grid'},{name: 'concentric'},
      {name: 'circle'},{name: 'random'},{name: 'breadthfirst'}];
    $scope.layout = $scope.layouts[0];
    $scope.minCount = 1;
    $scope.resultSize = 1000;
    $scope.sharedRlxChecked = true;
    $scope.reciprocalExcludedChecked = true;
    $scope.sumByCountChecked = true;
    if (configuration.devMode()) {
      $scope.devMode = 'true';
    } else {
      delete $scope.devMode;
    }

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });

    $scope.selectFortress = function () {
      QueryService.query('documents', $scope.fortress).then(function (data) {
        $scope.documents = data;
      });
      $scope.concepts = [];
      $scope.fromRlxs = [];
      $scope.toRlxs = [];
    };
    $scope.selectDocument = function () {
      QueryService.query('concepts', $scope.document).then(function (data) {
        var conceptMap = _.flatten(_.pluck(data, 'concepts'));
        $scope.concepts = _.uniq(conceptMap, function (c) {
          return c.name;
        });
      });
      $scope.fromRlxs = [];
      $scope.toRlxs = [];
    };

    $scope.selectAllFromRlx = function () {
      var filtered = filter($scope.fromRlxs);

      angular.forEach(filtered, function (item) {
        item.selected = true;
      });
    };

    $scope.selectConcept = function () {
      QueryService.query('relationships', $scope.document).then(function (data) {
        var conceptMap = _.filter(_.flatten(_.pluck(data, 'concepts')), function (c) {
          return _.contains($scope.concept, c.name);
        });
        var rlxMap = _.flatten(_.pluck(conceptMap, 'relationships'));
        var rlx = _.uniq(rlxMap, function (c) {
          return c.name;
        });
        $scope.fromRlxs = rlx;
        $scope.toRlxs = rlx;

      });
    };

    $scope.styles = [
      {'selector': 'node',
      'css': {
        'content': 'data(name)',
        'font-size': '15pt',
        'min-zoomed-font-size': '9pt',
        'text-halign': 'center',
        'text-valign': 'center',
        'color': 'white',
        'text-outline-width': 2,
        'text-outline-color': '#888',
        'width': '40',//'mapData(degree,0,5,20,80)',
        'height': '40',//'mapData(degree,0,5,20,80)',
        // 'shape': 'roundrectangle'
      }},
    {'selector':'edge',
      'css':{
        'width': 3,
        'target-arrow-color': '#ccc',
        'target-arrow-shape': 'triangle'
      }},
    {'selector':':selected',
      'css':{
        'background-color': 'black',
        'line-color': 'black',
        'target-arrow-color': 'black',
        'source-arrow-color': 'black',
        'text-outline-color': 'black'
      }},
    {'selector':'.mouseover',
      'css':{
        'color':'#499ef0'
      }}
    ];

    $scope.search = function () {
      angular.element('[data-target="#view"]').tab('show');
      if ($scope.sharedRlxChecked) {
        $scope.toRlx = $scope.fromRlx;
      }
      $scope.msg = '';

      QueryService.matrixSearch($scope.fortress,
        $scope.searchText,
        $scope.resultSize,
        $scope.document,
        $scope.sumByCountChecked,
        $scope.concept,
        $scope.fromRlx,
        $scope.toRlx,
        $scope.minCount,
        $scope.reciprocalExcludedChecked,
        true).then(function (data) {
          if (!data || data.length === 0) {
            $scope.msg = 'No Results.';
            return data;
          } else {
            $scope.msg = null;
          }

          $scope.graphData = data;
          // cyGraph($scope.graphData);
        });

    };
  }]);
