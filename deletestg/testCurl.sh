#!/bin/bash
echo delete.sh called
url=http://store.cdlib.org:33143/item/$1
echo URL=$url
curl -X DELETE $url?t=xml

