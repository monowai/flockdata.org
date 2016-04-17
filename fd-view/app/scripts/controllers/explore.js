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

fdView.controller('ExploreController', ['$scope', '$http', 'QueryService', '$window', '$controller', 'configuration',
  function ($scope, $http, QueryService, $window, $controller, configuration) {
    $scope.weight = 40;
    var graph={};
    $scope.slider ={
      value: $scope.weight,
      options: {
        floor: 5,
        ceil: 100,
        hideLimitLabels: true
      }
    };
    $scope.graph = {};
    $http.get('NetworkGraph.json').success(function(graph){
      // netGraph(graph, $scope.weight);
      // var elems ={nodes:[],edges:[]};
      for (var i = 0; i < graph.edges.length; i++) {
        $scope.graph['e'+i] = {
          data: {
            id: i,
            source: graph.edges[i].source,
            target: graph.edges[i].target,
          },
          group: "edges"
        };

      };
      for (var i = 0; i < graph.nodes.length; i++) {
        $scope.graph['n'+i] = {
          data: {
            id: graph.nodes[i].key,
            name: graph.nodes[i].value,
            weight: 30
          },
          group: "nodes"
        };
      };
    });
    $scope.layout = {name:'cose'};
    $scope.styles =[
      {
        selector: 'node',
        style: {
          'content': 'data(name)',
          'font-size': '15pt',
          'min-zoomed-font-size': '9pt',
          'text-halign': 'center',
          'text-valign': 'center',
          'color': 'white',
          'width': 'data(weight)',
          'height': 'data(weight)',
          'text-outline-width': 2,
          'text-outline-color': '#888'
        }
      },
      {
        selector: 'edge',
        style: {
          'width': 3,
          'target-arrow-color': '#ccc',
          'target-arrow-shape': 'triangle'
        }
      },
      {
        selector: ':selected',
        style: {
          'background-color': 'black',
          'line-color': 'black',
          'target-arrow-color': 'black',
          'source-arrow-color': 'black',
          'text-outline-color': 'black'
        }
      }
    ];
  }]);
