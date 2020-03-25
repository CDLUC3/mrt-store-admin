#!/bin/bash
#echo manStore.sh called
url="http://uc3-mrtinv-stg.cdlib.org:36121/mrtinv/manurl/$1?t=xml"
echo manStore: curl -X GET \'$url\'
curl -s -X GET $url

