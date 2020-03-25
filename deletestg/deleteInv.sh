#!/bin/bash
#echo delete.sh called
url="http://uc3-mrtinv-stg.cdlib.org:36121/mrtinv/object/$1?missing=yes&t=xml"
echo deleteInv: curl -X DELETE \'$url\'
curl -s -X DELETE $url

