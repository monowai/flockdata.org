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
  var lastMatrixQuery={},
      lastMatrixResult={};
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
      matrixSearch: function (fortresses, searchText, resultSize, documents, sumByCount, concepts, fromRlxs, toRlxs, minCount, reciprocals, byKey) {
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
          reciprocalExcluded: reciprocals,
          byKey: byKey
        };
        if(dataParam === lastMatrixQuery) return lastMatrixResult;
        else lastMatrixQuery = dataParam;
        console.log(dataParam);
        var promise = $http.post(configuration.engineUrl() + '/api/v1/query/matrix/', dataParam).then(function (response) {
          angular.copy(response.data, lastMatrixResult);
          if(byKey===false) {
            return response.data.edges;
          } else return response.data;
        });
        return promise;
      },
      lastMatrix: function () {
        return lastMatrixResult;
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
)
.factory('ContentProfile', ['$http', '$q', 'configuration',
  function ($http, $q, configuration) {
    var cp = {};
    var cpGraph = {};
    var cpFortress, cpType;
    return {
      getFortress: function () {
        if (cpFortress) { return cpFortress; }
      },
      getDocType: function () {
        if (cpType) { return cpType; }
      },
      getProfile: function (fortress, type) {
        if ((fortress===cpFortress && type===cpType && cp!=={}) || (!fortress && cp.length>0)) {
          var deferred = $q.defer();
          deferred.resolve(cp);
          return deferred.promise;
        } else {
          angular.copy(fortress, cpFortress);
          angular.copy(type, cpType);
          this.cpFortress = fortress;
          this.cpType = type;
          return $http.get(configuration.engineUrl() + '/api/v1/content/' + fortress + '/' + type)
            .success(function (data) {
              console.log(data);
              angular.copy(data, cp);
            });
        }
      },
      graphProfile: function () {
        if (cpGraph.length>0) {
          return cpGraph;
        }
        else {
          var graph = {nodes: [], edges: []};

          var createEntity = function (name, data) {
            var entity = new Object({id: name, name: name, type: 'entity'});
            _.extend(entity, data);
            return entity;
          };
          var isTag = function (o) {
            return o.tag === true || o.tagOrEntity === 'tag';
          };
          var addProp = function (obj, property) {
            return _.extend(obj, property);
          };
          var createTag = function (key, data) {
            var tag = new Object({id: key, name: key, type: 'tag'});
            _.extend(tag, data);
            return tag;
          };
          var connect = function (source, target, rel) {
            return {source: source, target: target, relationship: rel};
          };

          var hasTargets = function (obj) {
            return !!obj.targets && obj.targets.length > 0;
          };

          var hasEntityLinks = function (obj) {
            return !!obj.entityLinks && obj.entityLinks.length > 0;
          };

          var hasAliases = function (obj) {
            return !!obj.aliases && obj.aliases.length > 0;
          };
          var createTargets = function (tag, id) {
            _.each(tag.targets, function (target) {
              var t = createTag(target.code, {label: target.label});
              graph.nodes.push({data: t});
              var src, tgt;
              if (target.reverse) {
                src = t.id;
                tgt = id || tag.code;
              } else {
                src = id || tag.code;
                tgt = t.id;
              }
              graph.edges.push({data: connect(src, tgt, target.relationship)});
              if (hasTargets(target)) createTargets(target);
            })
          };

          var root = {};

          if (!isTag(cp)) {
            root = createEntity(cp.documentName || cp.documentType.name);
            graph.nodes.push({data: root});
          } else {
            root = createTag(cp.documentName);
            graph.nodes.push({data: root});
          }
          _.each(cp.content, function (obj, key) {
            if (isTag(obj)) {
              var tag = createTag(key, {label: (obj.label || key)});
              graph.nodes.push({data: tag});
              graph.edges.push({data: connect(root.id, tag.id, obj.relationship)});
              if (hasTargets(obj)) {
                createTargets(obj, tag.id);
              }
              if (hasAliases(obj)) {
                _.each(obj.aliases, function (alias) {
                  var a = {id: alias.code, code: alias.code, description: alias.description, type: 'alias'};
                  graph.nodes.push({data: a});
                  graph.edges.push({data: connect(tag.id, a.id)});
                })
              }
            }
            if (hasEntityLinks(obj)) {
              _.each(obj.entityLinks, function (entity) {
                var e = createEntity(entity.documentName);
                graph.nodes.push({data: e});
                graph.edges.push({data: connect(root.id, e.id, entity.relationshipName)});
              })
            }
          });
          angular.copy(graph, cpGraph);
          return graph;
        }
      },
      updateProfile: function (profile) {
        angular.copy(profile, cp);
        this.graphProfile();
      },
      saveProfile: function () {
       return $http.post(configuration.engineUrl() + '/api/v1/content/' + this.cpFortress+'/'+this.cpType, cp);
      }
  };
}]);

fdView.factory('cyGraph', ['$q', function($q){
  var cy;
  var cyGraph = function(graph) {
    var deferred = $q.defer();

    $(function(){
      cy = cytoscape({
        container: document.getElementById('cy'),

        style: cytoscape.stylesheet()
          .selector('node')
          .css({
            'content': 'data(name)',
            'font-size': '15pt',
            'min-zoomed-font-size': '9pt',
            'text-halign': 'center',
            'text-valign': 'center',
            'color': 'white',
            'text-outline-width': 2,
            'text-outline-color': '#888',
            'width': 20,//'mapData(degree,0,5,20,80)',
            'height': 20//'mapData(degree,0,5,20,80)'
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
          name: 'cose'
        },

        elements: graph,

        ready: function(){
          deferred.resolve( this );
        }
      });
    });

    return deferred.promise;
  };

  cyGraph.listeners = {};

  function fire(e, args) {
    var listeners = cyGraph.listeners[e];

    for (var i = 0; listeners && i < listeners.length; i++) {
      var fn = listeners[i];

      fn.apply(fn, args);
    }
  }

  function listen(e, fn) {
    var listeners = cyGraph.listeners[e] = cyGraph.listeners[e] || [];

    listeners.push(fn);
  }
  return cyGraph;
}]);
