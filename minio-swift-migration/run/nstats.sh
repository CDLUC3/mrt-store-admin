function list() {
pval=$1
from=2
to=$(date -d "$D" '+%-d')
for ((i=$from; i<=$to; i++))
do
   im=`expr $i - 1`
   sfrom=`printf "%02d" $im`	
   sto=`printf "%02d" $i`	
  ../report/stat.sh $sfrom $sto | fgrep "$pval" 
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
