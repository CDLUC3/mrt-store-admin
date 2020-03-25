#!/bin/bash
#echo delete.sh called
url="http://inv01-aws.cdlib.org:36121/mrtinv/object/$1?missing=yes&t=xml"
echo deleteInv: curl -X DELETE \'$url\'
curl -s -X DELETE $url

