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

angular.module('ab.graph.matrix.directives', [])
  .directive('chordDiagram', function ($timeout) {
    function drawChords(ele, matrix, mmap, scope) {
      var el = ele[0];
      var w = 1024, h = 640, r1 = h / 2, r0 = r1 - 100;
      var graphID = ele.parent()[0].id;
      var fill = d3.scale.ordinal()
        .domain(d3.range(6))
        .range(['#788b92', '#A2AC72', '#cab8a1', '#89b4fd', '#9eac74', '#f6f0ba']);

      var chord = d3.layout.chord()
        .padding(.03)
        .sortSubgroups(d3.descending)
        .sortChords(d3.ascending);

      var arc = d3.svg.arc()
        .innerRadius(r0)
        .outerRadius(r0 + 20);

      d3.select('#' + graphID + ' svg').remove();

      var svgP = d3.select(el).append('svg:svg');
//            .attr('width', w)
//            .attr('height', h);
      var svg = svgP.append('svg:g')
        .attr('id', 'circle');
//            .attr('transform', 'translate(' + w / 2 + ',' + h / 2 + ')');

      var circle = svg.append('circle');
//                .attr('r', r0 + 20);

      var rdr = chordRdr(matrix, mmap);
      chord.matrix(matrix);

      var g = svg.selectAll('g.group')
        .data(chord.groups())
        .enter().append('svg:g')
        .attr('class', 'group')
        .on('mouseover', mouseover)
        .on('mouseout', function (d) {
          d3.select('#tooltip').style('visibility', 'hidden');
        });

      var arcs = g.append('svg:path')
        .style('stroke', 'black')
        .style('fill', function (d) {
          return fill(d.index);
        });
//            .attr('d', arc);

      var txtG;
      txtG = g.append('svg:text')
        .each(function (d) {
          d.angle = (d.startAngle + d.endAngle) / 2;
        })
        .attr('dy', '.35em')
        .style('font-family', 'helvetica, arial, sans-serif')
        .style('font-size', '9px')
        .attr('text-anchor', function (d) {
          return d.angle > Math.PI ? 'end' : null;
        })
        .attr('transform', function (d) {
          return 'rotate(' + (d.angle * 180 / Math.PI - 90) + ')' +
            'translate(' + (r0 + 26) + ')' +
            (d.angle > Math.PI ? 'rotate(180)' : '');
        })
        .text(function (d) {
          // Trying to get the labels to look a little better when they are really looooonng.
          var maxLength = 20;
          if (rdr(d).gname.length > maxLength)
            return '...' + rdr(d).gname.substring(rdr(d).gname.length - maxLength);
          else
            return rdr(d).gname;
        });

      var chordPaths = svg.selectAll('path.chord')
        .data(chord.chords())
        .enter().append('svg:path')
        .attr('class', 'chord')
        .style('stroke', function (d) {
          return d3.rgb(fill(d.target.index)).darker();
        })
        .style('fill', function (d) {
          return fill(d.target.index);
        })
//                .attr('d', d3.svg.chord().radius(r0))
        .on('mouseover', function (d) {
          d3.select('#tooltip')
            .style('visibility', 'visible')
            .html(chordTip(rdr(d)))
            .style('top', function () {
              return (d3.event.pageY - 100) + 'px';
            })
            .style('left', function () {
              return '100px';
            });
        })
        .on('mouseout', function (d) {
          d3.select('#tooltip').style('visibility', 'hidden');
        });

      function chordTip(d) {
        var p = d3.format('.2%'), q = d3.format(',.3r');
        return p(d.stotal === 0 ? 0 : d.svalue / d.stotal) + ' of ' +
          d.sname + '<br>-to-<br>' + p(d.ttotal === 0 ? 0 : d.tvalue / d.ttotal) + ' of ' + d.tname;
        //+ ' <br> values - ' +  q(d.stotal) + ' / ' + q(d.tvalue);

      }

      function groupTip(d) {
        var p = d3.format('.1%'), q = d3.format(',.3r');
        return d.gname + ' <br>' + p(d.gvalue / d.mtotal) + ': ' + q(d.gvalue) +
          '  of ' + q(d.mtotal);
      }

      function mouseover(d, i) {
        d3.select('#tooltip')
          .style('visibility', 'visible')
          .html(groupTip(rdr(d)))
          .style('top', function () {
            return (d3.event.pageY - 80) + 'px';
          })
          .style('left', function () {
            return '130px';
          });

        chordPaths.classed('fade', function (p) {
          return p.source.index !== i &&
            p.target.index !== i;
        });
      }


      scope.$watch(function () {
        return el.clientWidth * el.clientHeight;
      }, function () {
        w = el.clientWidth;
        h = el.clientWidth * 2 / 3;
        r1 = h / 2;
        r0 = r1 - 100;
        resize();
      });
      function resize() {
        if (r0 < 0) {
          return;
        }
        svgP.attr({width: w, height: h});
        svg.attr('transform', 'translate(' + w / 2 + ',' + h / 2 + ')');
        circle.attr('r', r0 + 20);
        arc.innerRadius(r0).outerRadius(r0 + 20);
        arcs.attr('d', arc);
        txtG.attr('transform', function (d) {
          return 'rotate(' + (d.angle * 180 / Math.PI - 90) + ')' +
            'translate(' + (r0 + 26) + ')' +
            (d.angle > Math.PI ? 'rotate(180)' : '');
        });
        chordPaths.attr('d', d3.svg.chord().radius(r0));

      }
    }

    return {
      restrict: 'EA',
      scope: {
        data: '='
      },
      link: function (scope, ele, attr) {
        scope.$watch('data', function (data) {
          if (!data || data.matrix.length === 0) {
            return
          }
          ;
          drawChords(ele, data.matrix, data.mmap, scope);
        });
      }
    }
  }).directive('cooccurrenceDiagram', function () {
    /**
     * The graph is drawn by row.
     * @param  {[type]} matrix [matrix is a 2 dimension arrays. The 1st dimension is rows and 2nd one is each row.]
     * @param  {[type]} nodes  [the names of each cell]
     * @return {[type]}        [void]
     */
    function drawCooccurrence(ele, matrix, fromNodes, toNodes, fromOrders, toOrders) {
      var el = ele[0];

      var margin = {top: 60, right: 0, bottom: 0, left: 60}, width = 580, height = 580;

      var x = d3.scale.ordinal().rangeBands([0, width]),
        y = d3.scale.ordinal().rangeBands([height, 0]),
        z = d3.scale.linear().domain([0, 4]).clamp(true),
        c = d3.scale.category10().domain(d3.range(10));
      var graphID = ele.parent()[0].id;

      d3.select('#' + graphID + ' svg').remove();

      var svg = d3.select(el).append('svg')
        .attr('width', width + margin.left + margin.right)
        .attr('height', height + margin.top + margin.bottom)
        .style('float', 'left')
        .append('g')
        .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

      // The default sort order.
      y.domain(fromOrders);//DONE: Get axis labels
      x.domain(toOrders);//DONE: Get axis labels

      var rect = svg.append('rect')
        .attr('class', 'background')
        .attr('width', width)
        .attr('height', height);

      var row = svg.selectAll('.row')
        .data(matrix)
        .enter().append('g')
        .attr('class', 'row')
        .attr('transform', function (d, i) {
          return 'translate(0,' + y(i) + ')';
        })
        .each(row);


      row.append('line')
        .attr('x2', width);

      row.append('text')
        .attr('x', -6)
        .attr('y', x.rangeBand() / 2)
        .attr('dy', '.32em')
        .attr('text-anchor', 'end')
        .text(function (d, i) {
          return fromNodes[i].name;//DONE: get node name which is axis' name
        });

      var column = svg.selectAll('.column')
        .data(toNodes)
        .enter().append('g')
        .attr('class', 'column')
        .attr('transform', function (d, i) {
          return 'translate(' + x(i) + ')rotate(-90)';
        });

      column.append('line')
        .attr('x1', -width);

      column.append('text')
        .attr('x', 6)
        .attr('y', x.rangeBand() / 2)
        .attr('dy', '.32em')
        .attr('text-anchor', 'start')
        .text(function (d, i) {
          return toNodes[i].name;
        });

      function row(row) {
        var cell = d3.select(this).selectAll('.cell')
          .data(row.filter(function (d) {
            return d.z;
          }))
          .enter().append('rect')
          .attr('class', 'cell')
          .attr('x', function (d) {
            return x(d.x);
          })
          .attr('width', x.rangeBand())
          .attr('height', y.rangeBand())
          .style('fill-opacity', function (d) {
            return z(d.z);
          })
          .style('fill', function (d) {
            return c(d.z);//nodes[d.x].group == nodes[d.y].group ? c(nodes[d.x].group) : null;
          })
          .on('mouseover', mouseover)
          .on('mouseout', mouseout);
      }

      function mouseover(p) {
        d3.selectAll('.row text').classed('active', function (d, i) {
          return i === p.y;
        });
        d3.selectAll('.column text').classed('active', function (d, i) {
          return d.index === p.x;
        });
      }

      function mouseout() {
        d3.selectAll('text').classed('active', false);
      }

      // d3.select('#order').on('change', function() {
      //  clearTimeout(timeout);
      //  order(this.value);
      // });


      // var timeout = setTimeout(function() {
      //  order('group');
      //  d3.select('#order').property('selectedIndex', 2).node().focus();
      // }, 5000);

//        scope.$watch(function () {
//            return el.clientWidth * el.clientHeight;
//        }, function () {
//            w = el.clientWidth;
//            h = el.clientWidth * 2/3;
//            r1 = h / 2, r0 = r1 - 100
//            resize();
//        })
//        function resize() {
//            svg.attr({width: w+ margin.left + margin.right, height: h + margin.top + margin.bottom});
//            rect.attr({width:w, height:h});
//            circle.attr('r', r0 + 20);
//            arc.innerRadius(r0).outerRadius(r0 + 20);
//            arcs.attr('d', arc);
//            txtG.attr('transform', function(d) {
//                return 'rotate(' + (d.angle * 180 / Math.PI - 90) + ')'
//                    + 'translate(' + (r0 + 26) + ')'
//                    + (d.angle > Math.PI ? 'rotate(180)' : '');
//            })
//            chordPaths.attr('d', d3.svg.chord().radius(r0))
//
//        }

      function order(orderedNodes) {

        x.domain(orderedNodes);

        var t = svg.transition().duration(2500);

        t.selectAll('.row')
          .delay(function (d, i) {
            return x(i) * 4;
          })
          .attr('transform', function (d, i) {
            return 'translate(0,' + x(i) + ')';
          })
          .selectAll('.cell')
          .delay(function (d) {
            return x(d.x) * 4;
          })
          .attr('x', function (d) {
            return x(d.x);
          });

        t.selectAll('.column')
          .delay(function (d, i) {
            return x(i) * 4;
          })
          .attr('transform', function (d, i) {
            return 'translate(' + x(i) + ')rotate(-90)';
          });
      }

      return {orderFn: order};
    }

    return {
      restrict: 'EA',
      scope: {
        data: '=',
        orderedNodes: '='
      },
      link: function (scope, ele, attr) {
        var painter;
        scope.$watch('data', function (data) {
          if (!data || data.matrix.length === 0) {
            return;
          }
          painter = drawCooccurrence(ele, data.matrix, data.fromNodes, data.toNodes, data.fromOrders, data.toOrders); //DONE: What shall we do
        });
        scope.$watch('orderedNodes', function (orderedNodes) {
          if (!orderedNodes || !painter) {
            return;
          }
          painter.orderFn(orderedNodes);
        });
      }
    };
  }).directive('bipartiteDiagram', function ($q, $timeout) {
    function reset(graphID) {
      d3.select('#' + graphID).remove();
    }

    function drawBipartite(ele, data) {
      var width = 1400, height = d3.max([data[0].dataLength * 10 + 40, 600]), margin = {b: 0, t: 40, l: 170, r: 50};
      var el = ele[0];
      var graphID = data[0].id + 'svg';
      reset(graphID);
      var svg = d3.select(el).append('svg')
        .attr('id', graphID)
        .attr('width', width).attr('height', (height + margin.b + margin.t))
        .append('g').attr('transform', 'translate(' + margin.l + ',' + margin.t + ')');
      /* var bpData = [
       {data:bP.partData(mappedData,2), id:'Relationships', header:['From','To', 'Relationships']}
       ];  */
      return $timeout(function () {
          bP.draw(data, svg);
        }, 500, false
      );
    }

    return {
      restrict: 'EA',
      scope: {
        data: '='
      },
      link: function (scope, ele, attr) {
        scope.$watch('data', function (data) {
          if (!data || data[0].dataLength === 0) {
            return
          }
          ;
          var defer;
          if (data[0].dataLength > 200) {
            warnMsg($timeout, data[0].id + 'svg', defer, data[0].dataLength);
          }
          drawBipartite(ele, data); //DONE: What shall we do
        });

      }
    };
  });

var warnMsg = function (timeout, graphID, defer, dataLength) {
  var timeoutMs = 5000;

  return timeout(function () {
    if (!confirm('Data set is taking a while to process. There are a total of ' + dataLength + ' results. ' +
        'Would you like to continue?')) {
      // ToDo - report to the user on the D3 chart that it is 'incomplete'
//            if(d3.select('#' + graphID).length>0 )
//                d3.select('#' + graphID).remove();
      if (defer) {
        timeout.cancel(defer);
      }
      timeout.cancel(warnMsg);
    } else {
      timeout.cancel(warnMsg);
      //warnMsg(timeout,graphID,defer,dataLength);
    }
  }, timeoutMs);
};
