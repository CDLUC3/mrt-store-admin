#!/bin/bash
url=$1
echo $url
echo "***$url" >> outfile.txt
curl -X GET $url | fgrep 'exc:status>REQUESTED_ITEM_NOT_FOUND'
echo $?
