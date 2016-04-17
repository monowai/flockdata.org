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


fdView.factory('QueryService', ['$http', 'configuration', function ($http, configuration) {
    return {
      general: function (queryName) {
        return $http.get(configuration.engineUrl() + '/api/v1/' + queryName + '/').then(function (response) {
            return response.data;
          }
        );
      },
      query: function (queryName, params) {
        return $http.post(configuration.engineUrl() + '/api/v1/query/' + queryName + '/', params).then(function (response) {
            return response.data;
          }
        );
      },
      matrixSearch: function (fortresses, searchText, resultSize, documents, sumByCount, concepts, fromRlxs, toRlxs, minCount, reciprocals) {
        var dataParam = {
          documents: documents,
          sampleSize: resultSize,
          fortresses: fortresses,
          sumByCol: !sumByCount,
          queryString: searchText,
          concepts: concepts,
          fromRlxs: fromRlxs,
          toRlxs: toRlxs,
          minCount: minCount,
          reciprocalExcluded: reciprocals
        };
        console.log(dataParam);
        var promise = $http.post(configuration.engineUrl() + '/api/v1/query/matrix/', dataParam).then(function (response) {
          console.log(response.data);
          return response.data.edges;
        });
        return promise;
      },
      tagCloud: function (searchText, documents, fortress, tags, relationships) {
        var tagCloudParams = {
          searchText: searchText,
          types: documents,
          fortress: fortress[0],
          tags: tags,
          relationships: relationships
        };
        return $http.post(configuration.engineUrl() + '/api/v1/query/tagcloud/', tagCloudParams).then(function (response) {
          return response.data;
        });
      }


    };
  }]
);

fdView.factory('netGraph', ['$q', function($q){
  var cy;
  var netGraph = function(graph) {
    var deferred = $q.defer();
    var elems = {nodes:[],edges:[]};
    for (var i = 0; i < graph.edges.length; i++) {
      elems.edges.push({
        data: {
          id: i,
          source: graph.edges[i].source,
          target: graph.edges[i].target,
          // count: graph.edges[i].count
        }
      });
    }
    for (var i = 0; i < graph.nodes.length; i++) {
      elems.nodes.push({
        data: {
          id: graph.nodes[i].key,
          name: graph.nodes[i].value
        }
      });
    }

    $(function(){
      cy = cytoscape({
        container: document.getElementById('fd-cy'),

        style: cytoscape.stylesheet()
          .selector('node')
          .css({
            'content': 'data(name)',
            'font-size': '12pt',
            'min-zoomed-font-size': '9pt',
            'text-halign': 'center',
            'text-valign': 'center',
            'color': 'white',
            'width': 'mapData(degree,0,5,20,80)',
            'height': 'mapData(degree,0,5,20,80)'
          })
          .selector('edge')
          .css({
            'width': 3,
            'target-arrow-color': '#ccc',
            'target-arrow-shape': 'triangle'
          })
          .selector(':selected')
          .css({
            'background-color': 'black',
            'line-color': 'black',
            'target-arrow-color': 'black',
            'source-arrow-color': 'black',
            'text-outline-color': 'black'
          }),
        layout: {
          name: 'cose',
          padding: 10
        },

        elements: elems,
        ready: function(){
          deferred.resolve( this );
        }
      });
    });
    return deferred.promise;
  };

  netGraph.listeners = {};

  function fire(e, args) {
    var listeners = netGraph.listeners[e];

    for (var i = 0; listeners && i < listeners.length; i++) {
      var fn = listeners[i];

      fn.apply(fn, args);
    }
  }

  function listen(e, fn) {
    var listeners = netGraph.listeners[e] = netGraph.listeners[e] || [];

    listeners.push(fn);
  }
  return netGraph;
}]);
