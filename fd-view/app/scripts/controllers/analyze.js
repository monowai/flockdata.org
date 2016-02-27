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

fdView.controller('AnalyseController', ['$scope', 'QueryService', '$window', '$controller', 'configuration',
  function ($scope, QueryService, $window, $controller, configuration) {
    $scope.minCount = 1;
    $scope.resultSize = 1000;
    $scope.sharedRlxChecked = true;
    $scope.reciprocalExcludedChecked = false;
    $scope.sumByCountChecked = true;
    $scope.chartType = 'Chord';
    if (configuration.devMode()) {
      $scope.devMode = 'true';
    } else {
      delete $scope.devMode;
    }

    QueryService.general('fortress').then(function (data) {
      $scope.fortresses = data;
    });
    var addOptions = function () {
      if ($scope.chartType === 'Chord' || $scope.chartType === 'TagCloud') {
        $scope.sharedRlxChecked = true;
        $scope.reciprocalExcludedChecked = false;
      } else if ($scope.chartType === 'Matrix' || $scope.chartType === 'BiPartite') {
        $scope.sharedRlxChecked = false;
        $scope.reciprocalExcludedChecked = true;
      }
    };
    addOptions();

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
    $scope.graphData = [];

    $scope.search = function () {
      if ($scope.chartType === 'TagCloud') {
        QueryService.tagCloud($scope.searchText, $scope.document, $scope.fortress, $scope.concept, $scope.fromRlx).then(function (data) {
          var terms = [];
          for (var key in data.terms) {
            var item = {};
            item.occur = data.terms[key];
            item.term = key;
            item.size = data.terms[key].value;
            terms.push(item);
          }

          d3.layout.cloud().size([600, 600])
            .words(terms.map(function (d) {
              return {text: d.term, size: d.occur};
            }))
            .padding(2)
            .rotate(function () {
              //return (~~(Math.random() * 6) - 3) * 30;
              return 0;
            })
            .font('Impact')
            .fontSize(function (d) {
              return d.size;
            })
            .on('end', draw)
            .start();

        });
      }
      else {

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
          $scope.reciprocalExcludedChecked).then(function (data) {
            if (!data || data.length === 0) {
              $scope.msg = 'No Results.';
              return data;
            } else {
              $scope.msg = null;
            }
            $scope.graphData = data;
            $scope.cdData = null;
            $scope.coData = null;
            $scope.bpData = null;
            $scope.coMgr = $controller(constructCooccurrenceData, {
              $scope: {},
              getData: function () {
                return $scope.graphData;
              }
            });
            $scope.switchChart();
          }
        );
      }
    };

    function draw(words) {
      jQuery('#tagCloudPrinted').empty();
      var fill = d3.scale.category20();
      d3.select('#tagCloudPrinted').append('svg')
        .attr('width', 900)
        .attr('height', 600)
        .append('g')
        .attr('transform', 'translate(450,300)')
        .selectAll('text')
        .data(words)
        .enter().append('text')
        .style('font-size', function (d) {
          return d.size + 'px';
        })
        .style('font-family', 'Impact')
        .style('fill', function (d, i) {
          return fill(i);
        })
        .attr('text-anchor', 'middle')
        .attr('transform', function (d) {
          return 'translate(' + [d.x, d.y] + ')rotate(' + d.rotate + ')';
        })
        .text(function (d) {
          return d.text;
        });
    }

    $scope.switchChart = function () {
      if ($scope.chartType === 'Chord' && !$scope.cdData) {
        $scope.cdData = constructChordData($scope.graphData);
      } else if ($scope.chartType === 'Matrix' && !$scope.coData && $scope.coMgr) {
        $scope.order = 'name';
        $scope.coData = $scope.coMgr.getConstructedData();
      } else if ($scope.chartType === 'BiPartite' && !$scope.bpData) {
        $scope.bpData = constructBiPartiteData($scope.graphData);
      }

      if ($scope.chartType === 'TagCloud') {
        //$scope.cdData = constructChordData($scope.graphData);
      }
      addOptions();
    };
    $scope.orderMatrix = function () {
      if ($scope.order === 'count') {
        $scope.orderedNodes = $scope.coData.ordersFn.count;
      } else {
        $scope.orderedNodes = $scope.coData.ordersFn.name;
      }
    };
    angular.element($window).on('resize', function () {
      $scope.$apply()
    })

  }
]);

function constructChordData(data) {
  if (!data) {
    return;
  }
  var mpr = chordMpr(data);

  mpr.addValuesToMap('source')
    .addValuesToMap('target')
    .setFilter(function (row, a, b) {
      return (row.source === a.name && row.target === b.name);
    }).setAccessor(function (recs, a, b) {
      if (!recs[0]) {
        return 0;
      }
      return +recs[0].count;
    }
  );
  return {matrix: mpr.getMatrix(), mmap: mpr.getMap()};
}

function constructTagCloudData(data) {

}

function constructCooccurrenceData($scope, getData) {
  var data = getData();
  this.getConstructedData = function () {
    var matrix = [],
      nodes = _.union(_.pluck(data, 'source'), _.pluck(data, 'target')),
      fromNodes = _.unique(_.pluck(data, 'source')), toNodes = _.unique(_.pluck(data, 'target'));

    if (!nodes) {
      return null;
    }
    var m = fromNodes.length;
    var n = toNodes.length;
    var clonedFromNodes = angular.copy(fromNodes);
    var clonedToNodes = angular.copy(toNodes);
    // Compute index per node.
    fromNodes.forEach(function (name, i, self) {
      var node = {};
      node.index = i;
      node.count = 0;
      node.name = name;
      self[i] = node;
      matrix[i] = d3.range(n).map(function (j) {
        return {x: j, y: i, z: 0};
      });
    });
    toNodes.forEach(function (name, i, self) {
      var node = {};
      node.index = i;
      node.count = 0;
      node.name = name;
      self[i] = node;

    });

    // Convert links to matrix; count character occurrences.
    data.forEach(function (link) {
      var i = clonedFromNodes.indexOf(link.source);
      var j = clonedToNodes.indexOf(link.target);
      matrix[i][j].z += link.count;
      fromNodes[i].count += link.count;
      toNodes[j].count += link.count;
    });
    // Pre-compute the orders.
    var orders = {
      fromName: d3.range(m).sort(function (a, b) {
        return d3.descending(fromNodes[a].name, fromNodes[b].name);
      }),
      toName: d3.range(n).sort(function (a, b) {
        return d3.ascending(toNodes[a].name, toNodes[b].name);
      }),
      count: d3.range(n).sort(function (a, b) {
        return nodes[b].count - nodes[a].count;
      })
    };

    return {
      matrix: matrix,
      fromNodes: fromNodes,
      toNodes: toNodes,
      fromOrders: orders.fromName,
      toOrders: orders.toName,
      ordersFn: orders
    };
  };
  return this;

}
function split(a, n) {
  var len = a.length, out = [], i = 0;
  while (i < len) {
    var size = Math.ceil((len - i) / n--);
    out.push(a.slice(i, i + size));
    i += size;
  }
  return out;
}
function constructBiPartiteData(data) {
  var chartData = [];

  var mappedData = _.map(data, function (v, k) {
    return _.values(v);
  });

  var bpData = [
    {
      data: bP.partData(mappedData, 2),
      dataLength: data.length,
      id: 'Relationship',
      header: ['Source', 'Target', 'Relationship']
    }
  ];
  chartData.push(bpData);

  return chartData;
}

