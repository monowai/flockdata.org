#!/bin/bash

## First, ensure we have a registered user against which we can upload data
## the user's company in this example is Monowai. In a multi tenanted mode data is partitioned by company
curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8080/api/v1/profiles/ -d '{"name":"mike", "companyName":"Monowai","password":"whocares"}'

## Create a fortress against which we can start to load data. Fortress is owned by the Company
## Here, law-suits is the name of our fortress. Fortress is only required if you are going to load track records
# curl -u mike:123 -H "Content-Type:application/json" -X POST http://localhost:8080/api/v1/fortress/ -d '{"name": "law-suits","searchEnabled": true}'

curl -u mike:123 -H "Content-Type:application/json" -X PUT http://localhost:8080/api/v1/tag/ --data-binary @./suits.json
