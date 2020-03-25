#!/bin/bash
#echo deleteLocal.sh called
url="http://uc3-mrtinv-stg.cdlib.org:36121/mrtinv/primary/$1?t=xml"
echo deleteLocal: curl -X DELETE \'$url\'
curl -s -X DELETE $url

