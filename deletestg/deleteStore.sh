#!/bin/bash
#echo deleteStore.sh called
url=$1
echo deleteStore: curl -s -X DELETE \'$url\'
curl -s -L -X DELETE $url

