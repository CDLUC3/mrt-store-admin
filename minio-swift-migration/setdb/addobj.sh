base='http://uc3-mrtreplic1-prd.cdlib.org:38001/mrtreplic'
encark="curl -s -X POST $base/$1?t=xml"
echo ${encark}