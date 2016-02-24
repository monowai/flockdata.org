'use strict';


fdView.factory('QueryService', ['$http', 'configuration', function ($http, configuration) {
    return {
      general: function (queryName) {
        return $http.get(configuration.engineUrl() + '/v1/' + queryName + '/').then(function (response) {
            return response.data;
          }
        );
      },
      query: function (queryName, params) {
        return $http.post(configuration.engineUrl() + '/v1/query/' + queryName + '/', params).then(function (response) {
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
        var promise = $http.post(configuration.engineUrl() + '/v1/query/matrix/', dataParam).then(function (response) {
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
        return $http.post(configuration.engineUrl() + '/v1/query/tagcloud/', tagCloudParams).then(function (response) {
          return response.data;
        });
      }


    };
  }]
);
