function list() {
pval=$1
to=$(date -d "$D" '+%d')
for i in $(seq -f "%02g" 2 $to)
do
  ../report/stat.sh 01 $i | fgrep "$pval" 
done
echo '------------------'
}
echo '===|$toDatetime|$diffTotobjall|$objleft|$diffTotobjmigr|$diffTotbyteall|$bytesleft|$diffTotbytemigr'
echo '+++|$adjobjdays|$adjbytesdays|$noaddbytesdays|$bytespersec|$bytesleft|$testday'
echo '&&&|$toDatetime|$toPerobj|$toPerbyte|$adjobjdays|$adjbytesdays|$bytespersec'
echo '@@@||$objleft|$bytesleft|$adjobjdays|$adjbytesdays|$bytespersec'
echo '-----------------------------------------------------------'
list '==='
list '+++'
list '&&&'
list '@@@'
