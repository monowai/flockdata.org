'use strict';

fdView.factory('ProfileService', ['$http', 'configuration',
    function ($http, configuration) {
      return {
        getMyProfile: function () {
          return $http.get(configuration.engineUrl() + '/v1/profiles/me').then(function (response) {
              return response.data;
            }
          );
        }
      };
    }
  ]
);

fdView.factory('EntityService', ['$http', 'configuration',
    function ($http, configuration) {

      return {
        search: function (searchText, company, fortress, typesToBeSend) {
          var dataParam = {searchText: searchText, company: company, fortress: fortress, types: typesToBeSend};
          return $http.post(configuration.engineUrl() + '/v1/query/', dataParam).then(function (response) {
            return response.data.results;
          });
        },
        getLogsForEntity: function (metaKey) {
          var url = configuration.engineUrl() + '/v1/entity/' + metaKey + '/summary';
          return $http.get(url).then(function (response) {
            return response.data;
          });
        },
        getJsonContentForLog: function (metaKey, logId) {
          var url = configuration.engineUrl() + '/v1/entity/' + metaKey + '/log/' + logId + '/data';
          return $http.get(url).then(function (response) {
//                  This endpoint only ever returns JSON type data
            return response.data;
          });
        },
        getJsonAttachmentForLog: function (metaKey, logId) {
          var url = configuration.engineUrl() + '/v1/entity/' + metaKey + '/log/' + logId + '/attachment';
          return $http.get(url).then(function (response) {
//                  Content for this EP is variable - PDF, XLS, PPT etc. Can be found from the Log
            return response.data;
          });
        },
        getTagsForEntity: function (metaKey) {
          var url = configuration.engineUrl() + '/v1/entity/' + metaKey + '/tags';
          return $http.get(url).then(function (response) {
            return response.data;
          });
        },
        getEntityPK: function (metaKey) {
          var url = configuration.engineUrl() + '/v1/entity/' + metaKey;
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
      this.create = function (userId, userName, userEmail, status, company, userRoles, apiKey) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.status = status;
        this.company = company;
        this.userRoles = userRoles;
        this.apiKey = apiKey;
      };
      this.invalidate = function () {
        this.userId = null;
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
      return $resource(configuration.engineUrl() + '/v1/account', {}, {});
    }
  ]
);

fdView.factory('AuthenticationSharedService', ['$rootScope', '$http', 'authService', 'Account', 'Session', 'configuration',
    function ($rootScope, $http, authService, Account, Session, configuration) {
      return {
        login: function (username, password) {
          var data = {username: username, password: password};
          var url = configuration.engineUrl() + '/v1/login';
          $http.post(url, data).success(function (data, status, headers, config) {
            authService.loginConfirmed(data);
            Session.create(data.userId, data.userName, data.userEmail, data.status, data.company, data.userRoles, data.apiKey);
            $rootScope.account = Session;
            authService.loginConfirmed(data);
          }).error(function (data, status, headers, config) {
              $rootScope.authenticationError = true;
              Session.invalidate();
            }
          );
        },
        valid: function (authorizedRoles) {
          if (!Session.login) {
            Account.get(function (data) {
                Session.create(data.userId, data.userName, data.userEmail, data.status, data.company, data.userRoles, data.apiKey);
                $rootScope.account = Session;

                if (!$rootScope.isAuthorized(authorizedRoles)) {
                  event.preventDefault();
                  // user is not allowed
                  $rootScope.$broadcast('event:auth-notAuthorized');
                }

                $rootScope.authenticated = true;
              }
            );
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
        logout: function () {
          $rootScope.authenticationError = false;
          $rootScope.authenticated = false;
          $rootScope.account = null;

          $http.get(configuration.engineUrl() + '/v1/logout');
          Session.invalidate();
          authService.loginCancelled();
        }
      };
    }
  ]
);


fdView.factory('interceptorNgProgress', function ($injector) {
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
  }
);
