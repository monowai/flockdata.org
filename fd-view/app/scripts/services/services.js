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

fdView.factory('EntityService', ['$http', 'configuration',
    function ($http, configuration) {

      return {
        search: function (searchText, company, fortress, typesToBeSend) {
          var dataParam = {searchText: searchText, company: company, fortress: fortress, types: typesToBeSend};
          return $http.post(configuration.engineUrl() + '/api/v1/query/', dataParam).then(function (response) {
            return response.data.results;
          });
        },
        getLogsForEntity: function (entityKey) {
          var url = configuration.engineUrl() + '/api/v1/entity/' + entityKey + '/summary';
          return $http.get(url).then(function (response) {
            return response.data;
          });
        },
        getJsonContentForLog: function (entityKey, logId) {
          var url = configuration.engineUrl() + '/api/v1/entity/' + entityKey + '/log/' + logId + '/data';
          return $http.get(url).then(function (response) {
//                  This endpoint only ever returns JSON type data
            return response.data;
          });
        },
        getJsonAttachmentForLog: function (entityKey, logId) {
          var url = configuration.engineUrl() + '/api/v1/entity/' + entityKey + '/log/' + logId + '/attachment';
          return $http.get(url).then(function (response) {
//                  Content for this EP is variable - PDF, XLS, PPT etc. Can be found from the Log
            return response.data;
          });
        },
        getTagsForEntity: function (entityKey) {
          var url = configuration.engineUrl() + '/api/v1/entity/' + entityKey + '/tags';
          return $http.get(url).then(function (response) {
            return response.data;
          });
        },
        getEntityPK: function (entityKey) {
          var url = configuration.engineUrl() + '/api/v1/entity/' + entityKey;
          return $http.get(url).then(function (response) {
            return response.data.id;
          });
        }
      };
    }
  ]
);

fdView.factory('DataSharingService', function () {
    return {searchTerm: ''};
  }
);


fdView.factory('Session', [
    function () {
      this.create = function (data) {
        this.login = data.login;
        this.userName = data.name;
        this.userEmail = data.userEmail;
        this.status = data.status;
        this.company = data.companyName;
        this.userRoles = data.userRoles;
        this.apiKey = data.apiKey;
      };
      this.invalidate = function () {
        this.login = null;
        this.userName = null;
        this.userEmail = null;
        this.status = null;
        this.company = null;
        this.userRoles = null;
        this.apiKey = null;
      };
      return this;
    }
  ]
);

fdView.factory('Account', ['$resource', 'configuration',
    function ($resource, configuration) {
      return $resource(configuration.engineUrl() + '/api/account', {}, {});
    }
  ]
);

fdView.factory('AuthenticationSharedService', ['$rootScope', '$state', '$http', 'authService', 'Account', 'Session', 'configuration',
    function ($rootScope, $state, $http, authService, Account, Session, configuration) {
      return {
        login: function (username, password) {
          var data = {username: username, password: password};
          var url = configuration.engineUrl() + '/api/login';
          return $http.post(url, data).success(function (data, status, headers, config) {
            Session.create(data);
            $rootScope.account = Session;
            authService.loginConfirmed(data);
            return data;
          }).error(function (data, status, headers, config) {
              $rootScope.authenticationError = true;
              Session.invalidate();
            }
          );
        },
        valid: function (authorizedRoles) {
          if (!Session.login) {
            Account.get(function (data) {
              Session.create(data);
              $rootScope.account = Session;
              $rootScope.authenticated = true;
            });
          }
          if (!$rootScope.isAuthorized(authorizedRoles)) {
            // user is not allowed
            $rootScope.$broadcast('event:auth-notAuthorized');
          }
          $rootScope.authenticated = !!Session.login;
        },
        isAuthorized: function (authorizedRoles) {
          if (!angular.isArray(authorizedRoles)) {
            if (authorizedRoles === '*') {
              return true;
            }
            authorizedRoles = [authorizedRoles];
          }

          var isAuthorized = false;
          angular.forEach(authorizedRoles, function (authorizedRole) {
            var authorized = (!!Session.login &&
            Session.userRoles.indexOf(authorizedRole) !== -1);

            if (authorized || authorizedRole === '*') {
              isAuthorized = true;
            }
          });

          return isAuthorized;
        },
        getMyProfile: function () {
          return $http.get(configuration.engineUrl() + '/api/account').then(function (response) {
            return response.data;
          });
        },
        logout: function () {
          $rootScope.authenticationError = false;
          $rootScope.authenticated = false;
          $rootScope.account = null;

          $http.get(configuration.engineUrl() + '/logout');
          Session.invalidate();
          authService.loginCancelled();
        }
      };
    }
  ]
);


fdView.factory('interceptorNgProgress', ['$injector' ,function ($injector) {
    var completedProgress, getNgProgress, ngProgress, working;
    ngProgress = null;
    working = false;

    getNgProgress = function () {
      ngProgress = ngProgress || $injector.get('ngProgress');
      ngProgress.color('red');
      return ngProgress;
    };

    completedProgress = function () {
      var ngProgress;
      if (working) {
        ngProgress = getNgProgress();
        ngProgress.complete();
        return working = false;
      }
    };

    return {
      request: function (request) {
        var ngProgress;
        ngProgress = getNgProgress();
        if (request.url.indexOf('.html') > 0) {
          return request;
        }
        if (!working) {
          ngProgress.reset();
          ngProgress.start();
          working = true;
        }
        return request;
      },
      requestError: function (request) {
        completedProgress();
        return request;
      },
      response: function (response) {
        completedProgress();
        return response;
      },
      responseError: function (response) {
        completedProgress();
        return response;
      }
    };
  }]
);
