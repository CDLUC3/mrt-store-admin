#!/bin/bash
#echo delete.sh called
url="http://uc3-mrtreplic1-prd.cdlib.org:38001/mrtreplic/deletesecondary/$1?t=xml"
echo deleteInv: curl -X DELETE \'$url\'
curl -s -X DELETE $url
