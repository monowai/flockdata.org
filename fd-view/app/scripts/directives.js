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

angular.module('fdView.directives', [])
  .directive('ngFlockheader', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/partials/header.html'
    };
  })
  .directive('ngFlockleftbar', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/partials/leftbar.html',
      scope: {
        active: '='
      }
    };
  }).directive('ngAbTable', function () {
    return {
      restrict: 'E, A, C',
      link: function (scope, element, attrs, controller) {
        // If Actions Defined
        if (scope.actions) {
          scope.options.aoColumns.push(scope.actions);
        }
        var dataTable = element.dataTable(scope.options);

        scope.$watch('options.aaData', handleModelUpdates, true);

        function handleModelUpdates(newData) {
          var data = newData || null;
          if (data) {
            dataTable.fnClearTable();
            dataTable.fnAddData(data);
          }
        }
      },
      scope: {
        options: '=',
        actions: '='
      }
    };
  })
  .directive('ngJsonabview', function () {
    return {
      restrict: 'A',
      link: function (scope, element, attrs) {
        scope.$watch(attrs.abData, function (newValue) {
          var container = document.getElementById(attrs.id);
          $('#' + attrs.id).empty();
          var options = {
            mode: 'view'
          };
          var editor = new JSONEditor(container, options);
          var data = scope.$eval(attrs.abData);

          editor.set(data);
        });

//                var container = document.getElementById(attrs.id);
//                var options = {
//                    mode: 'view'
//                };
//                var editor = new JSONEditor(container,options);
//                var data = scope.$eval(attrs.abData);
//
//                editor.set(data);
      }
    };
  })
  .directive('ngJsondiff', function () {
    return {
      restrict: 'A',
      link: function (scope, element, attrs) {
        var left = scope.$eval(attrs.abLeft);
        var right = scope.$eval(attrs.abRight);

        var delta = jsondiffpatch.create({
          objectHash: function (obj) {
            return obj._id || obj.id || obj.name || JSON.stringify(obj);
          }
        }).diff(left, right);

        jsondiffpatch.formatters.html.hideUnchanged();
        // beautiful html diff
        document.getElementById('visual').innerHTML = jsondiffpatch.formatters.html.format(delta, left);
        // self-explained json
        document.getElementById('annotated').innerHTML = jsondiffpatch.formatters.annotated.format(delta, left);
      }
    }
  })
  .directive('ngJsondiff2', function () {
    return {
      restrict: 'A',
      link: function (scope, element, attrs) {
        //scope.$watchCollection(['attr.abLeft','attrs.abRight'], function (newValue) {
        scope.$watch(attrs.abLeft, function (newValue) {
          var left = scope.$eval(attrs.abLeft);
          var right = scope.$eval(attrs.abRight);

          var delta = jsondiffpatch.create({
            objectHash: function (obj) {
              return obj._id || obj.id || obj.name || JSON.stringify(obj);
            }
          }).diff(left, right);

          jsondiffpatch.formatters.html.hideUnchanged();
          // beautiful html diff
          document.getElementById('visual').innerHTML = jsondiffpatch.formatters.html.format(delta, left);
        });
      }
    }
  })
  .directive('ngFlockdeltapopup', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/partials/deltaPopup.html'
    }
  })
  .directive('ngFlocksinglelogpopup', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/partials/singleLogPopup.html'
    }
  });

// Directives
